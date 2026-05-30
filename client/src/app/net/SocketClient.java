package app.net;

import com.ltnc.auction.shared.protocol.ClientToServerMessage;
import com.ltnc.auction.shared.protocol.ServerToClientMessage;
import com.ltnc.auction.shared.protocol.MessageType;


import app.config.ClientConfig;
import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;

/**
 * ============================================================
 *  TẦNG NETWORK (Client) – KẾT NỐI VỚI SERVER QUA TCP SOCKET
 *  Phụ trách: Thành viên Network (TV3)
 * ============================================================
 *
 *  SocketClient là "cầu nối" giữa Client và Server:
 *  - Kết nối TCP tới server (mặc định {@code localhost:9090}; ghi đè bằng biến môi trường, xem {@link app.config.ClientConfig}).
 *  - Gửi request dạng JSON (một dòng) và nhận response.
 *  - Phân biệt hai loại message từ server:
 *    + Response thông thường (AUCTION_LIST, BID_RESULT, ...) → đưa vào responseQueue.
 *    + Broadcast realtime (AUCTION_UPDATE) → forward tới các listener đã đăng ký.
 *
 *  Pattern: Singleton – toàn app chỉ có một kết nối TCP duy nhất.
 *
 *  Sơ đồ luồng message:
 *
 *    [Service layer]
 *         │ sendAndReceive(request)
 *         ▼
 *    [SocketClient]──── gửi JSON ────► [Server]
 *         │                                │
 *         │        ◄─── nhận JSON ─────────┤
 *         │                                │
 *    [Reader Thread] ──┬─ AUCTION_UPDATE → broadcastListeners → UI cập nhật
 *                      └─ Response khác  → responseQueue → sendAndReceive() trả về
 */
public class SocketClient {
    private static final SocketClient INSTANCE = new SocketClient();
    private static final Logger LOG = Logger.getLogger(SocketClient.class.getName());

    private Socket socket;
    private PrintWriter out;     // ghi JSON ra socket (gửi lên server)
    private BufferedReader in;   // đọc JSON từ socket (nhận từ server)
    private volatile boolean connected = false;
    private volatile Runnable onReconnected;

    /**
     * Hàng đợi chứa response thông thường.
     * sendAndReceive() gửi request rồi poll hàng đợi này để chờ response.
     * Dùng BlockingQueue để thread gửi có thể chờ thread đọc mà không busy-wait.
     */
    private final BlockingQueue<ServerToClientMessage> responseQueue = new LinkedBlockingQueue<>();

    /**
     * Danh sách listener nhận broadcast AUCTION_UPDATE.
     * Dùng CopyOnWriteArrayList để an toàn khi duyệt từ Reader Thread
     * trong khi thread khác có thể add/remove listener.
     */
    private final List<Consumer<ServerToClientMessage>> broadcastListeners = new CopyOnWriteArrayList<>();

    private final Gson gson = new Gson();

    private SocketClient() {}

    public static SocketClient getInstance() { return INSTANCE; }

    /**
     * Kết nối TCP tới server. Nếu thành công → khởi động Reader Thread.
     * @return true nếu kết nối thành công, false nếu server chưa chạy.
     */
    public boolean connect() {
        String host = ClientConfig.getServerHost();
        int port = ClientConfig.getServerPort();
        try {
            socket = new Socket(host, port);
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            connected = true;
            startReaderThread(); // bắt đầu lắng nghe message từ server
            startPingThread();   // tự động gửi PING để giữ kết nối
            LOG.info(() -> "Socket connected to " + host + ":" + port);
            return true;
        } catch (Exception e) {
            connected = false;
            LOG.log(Level.WARNING, "Failed to connect to " + host + ":" + port, e);
            return false; // server chưa khởi động → app chạy ở MOCK mode
        }
    }

    public boolean isConnected() { return connected; }

    public void setOnReconnected(Runnable onReconnected) {
        this.onReconnected = onReconnected;
    }

    /** Đăng ký nhận broadcast AUCTION_UPDATE (dùng bởi NetworkAuctionService). */
    public void addBroadcastListener(Consumer<ServerToClientMessage> listener) {
        broadcastListeners.add(listener);
    }

    public void removeBroadcastListener(Consumer<ServerToClientMessage> listener) {
        broadcastListeners.remove(listener);
    }

    /**
     * Gửi request và chờ response (synchronous, tối đa 5 giây).
     *
     * Tại sao synchronized? → đảm bảo chỉ một request được gửi tại một thời điểm,
     * tránh response của request A bị nhầm sang request B.
     */
    public synchronized ServerToClientMessage sendAndReceive(ClientToServerMessage req) {
        if (!connected) return null;
        try {
            responseQueue.clear(); // xóa response cũ (nếu có)
            out.println(gson.toJson(req)); // serialize object → JSON string, gửi một dòng
            return responseQueue.poll(ClientConfig.getSocketTimeoutMs(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /**
     * Thread riêng chạy ngầm, liên tục đọc message từ server.
     *
     * Tại sao cần thread riêng? → JavaFX UI Thread không được block.
     * Nếu đọc socket trên UI Thread → giao diện bị đơ trong khi chờ server.
     *
     * Phân loại message nhận được:
     *  - AUCTION_UPDATE (broadcast) → gọi tất cả broadcastListeners ngay lập tức.
     *  - Mọi loại khác (response) → đẩy vào responseQueue để sendAndReceive() nhận.
     */
    private void startReaderThread() {
        Thread t = new Thread(() -> {
            while (connected) {
                try {
                    String line = in.readLine(); // blocking – chờ server gửi dữ liệu
                    if (line == null) {          // null → server đóng kết nối
                        connected = false;
                        break;
                    }
                    ServerToClientMessage resp = gson.fromJson(line, ServerToClientMessage.class);
                    if (resp == null) continue;

                    if (resp.type != null && isBroadcast(resp.type)) {
                        for (Consumer<ServerToClientMessage> listener : broadcastListeners) {
                            listener.accept(resp);
                        }
                    } else {
                        responseQueue.offer(resp);
                    }
                } catch (IOException e) {
                    connected = false;
                    break;
                }
            }
            scheduleReconnect();
        });
        t.setDaemon(true);      // daemon thread – tự tắt khi app đóng
        t.setName("socket-reader");
        t.start();
    }

    private void startPingThread() {
        Thread t = new Thread(() -> {
            while (connected) {
                try {
                    Thread.sleep(15000); // Gửi PING mỗi 15 giây
                    if (connected && out != null) {
                        ClientToServerMessage pingReq = new ClientToServerMessage();
                        pingReq.type = MessageType.PING;
                        sendMessage(pingReq);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        t.setDaemon(true);
        t.setName("socket-ping");
        t.start();
    }

    public void sendMessage(ClientToServerMessage req) {
        if (connected && out != null) {
            out.println(gson.toJson(req));
        }
    }

    private static boolean isBroadcast(MessageType type) {
        return type == MessageType.AUCTION_UPDATE
                || type == MessageType.AUCTION_CREATED
                || type == MessageType.ITEM_UPDATE
                || type == MessageType.ITEM_DELETED
                || type == MessageType.USER_CREATED
                || type == MessageType.WALLET_UPDATE
                || type == MessageType.AUCTION_STATE_CHANGE
                || type == MessageType.AUCTION_DELETED
                || type == MessageType.FORCE_LOGOUT
                || type == MessageType.NOTIFICATIONS_RESULT
                || type == MessageType.NEW_NOTIFICATION;
    }

    private void scheduleReconnect() {
        Thread recon = new Thread(() -> {
            while (!connected) {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                if (tryReconnect()) {
                    Runnable cb = onReconnected;
                    if (cb != null) {
                        Platform.runLater(cb);
                    }
                    return;
                }
            }
        }, "socket-reconnect");
        recon.setDaemon(true);
        recon.start();
    }

    /** Kết nối lại không khởi tạo socket mới từ {@link #connect()} — dùng nội bộ reconnect. */
    private synchronized boolean tryReconnect() {
        String host = ClientConfig.getServerHost();
        int port = ClientConfig.getServerPort();
        try {
            socket = new Socket(host, port);
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            connected = true;
            startReaderThread();
            startPingThread();
            LOG.info(() -> "Socket reconnected to " + host + ":" + port);
            return true;
        } catch (Exception e) {
            connected = false;
            LOG.log(Level.FINE, "Reconnect failed to " + host + ":" + port, e);
            return false;
        }
    }
}
