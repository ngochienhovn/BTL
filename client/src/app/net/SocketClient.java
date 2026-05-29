package app.net;

import com.google.gson.Gson;
import com.ltnc.auction.shared.protocol.ClientToServerMessage;
import com.ltnc.auction.shared.protocol.MessageType;
import com.ltnc.auction.shared.protocol.ServerToClientMessage;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.function.Consumer;

public class SocketClient {
    private static final SocketClient INSTANCE = new SocketClient();

    private static final String HOST = "localhost";
    private static final int PORT = 5555;

    private final Gson gson = new Gson();

    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;

    private Consumer<ServerToClientMessage> messageListener;

    private SocketClient() {}

    public static SocketClient getInstance() {
        return INSTANCE;
    }

    public void connect() {
        try {
            if (socket != null && socket.isConnected() && !socket.isClosed()) {
                return;
            }

            socket = new Socket(HOST, PORT);
            writer = new PrintWriter(socket.getOutputStream(), true);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            ServerMessageListener listener = new ServerMessageListener(reader, msg -> {
                if (messageListener != null) {
                    messageListener.accept(msg);
                }
            });

            Thread listenerThread = new Thread(listener);
            listenerThread.setDaemon(true);
            listenerThread.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setMessageListener(Consumer<ServerToClientMessage> messageListener) {
        this.messageListener = messageListener;
        connect();
    }

    public void sendBid(Long auctionId, Double amount) {
        ClientToServerMessage request = new ClientToServerMessage();
        request.type = MessageType.PLACE_BID;
        request.auctionId = auctionId;
        request.bidAmount = amount;

        // Tạm thời để demo.
        // Sau này khi login thật thì thay bằng user đang đăng nhập.
        request.userId = 1L;
        request.email = "bidder1@gmail.com";
        request.role = "BIDDER";

        send(request);
    }

    public void send(ClientToServerMessage request) {
        try {
            connect();

            if (writer != null) {
                writer.println(gson.toJson(request));
                writer.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}