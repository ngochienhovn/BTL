package com.ltnc.auction.server.network;

<<<<<<< HEAD
import com.ltnc.auction.server.service.AuctionService;
import com.ltnc.auction.server.service.AuthService;
import com.ltnc.auction.server.service.BroadcastManager;
import com.ltnc.auction.server.service.ItemService;
=======
import com.ltnc.auction.server.services.AuthService;
import com.ltnc.auction.server.services.ItemService;
import com.ltnc.auction.server.services.AuctionService;
import com.ltnc.auction.server.services.WalletService;
import com.ltnc.auction.server.network.AuctionBroadcaster;

>>>>>>> a4a9980ce3593461ea601ec5d280f231fec24645
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
<<<<<<< HEAD
import java.util.concurrent.TimeUnit;
import com.ltnc.auction.server.db.DBConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ============================================================
 *  TẦNG NETWORK – MỞ CỔNG VÀ QUẢN LÝ KẾT NỐI
 *  Phụ trách: Thành viên Network (TV3)
 * ============================================================
 *
 *  SocketServer là "cửa ngõ" của server:
 *  - Mở một cổng TCP (mặc định 9090) và lắng nghe kết nối từ client.
 *  - Khi có client kết nối → tạo một {@link ClientHandler} riêng và
 *    giao cho Thread Pool xử lý song song.
 *
 *  Thread Pool (16 luồng) cho phép server phục vụ tối đa 16 client cùng lúc
 *  mà không bị nghẽn. Mỗi ClientHandler chạy độc lập trên một luồng.
 *
 *  Sơ đồ luồng khi có kết nối mới:
 *
 *    Client kết nối TCP
 *        │
 *        ▼
 *    serverSocket.accept()  ← SocketServer đang chờ ở đây
 *        │
 *        ▼
 *    new ClientHandler(socket, ...)
 *        │
 *        ▼
 *    executor.submit(handler)  ← giao cho thread pool xử lý async
 *        │
 *        ▼
 *    ClientHandler.run()  ← xử lý toàn bộ vòng đời của client đó
 */

public class SocketServer {
    private static final Logger LOG = LoggerFactory.getLogger(SocketServer.class);

    private final int port;

    /**
     * Thread pool cố định 16 luồng.
     * Lý do dùng fixedThreadPool thay vì cachedThreadPool:
     * giới hạn tối đa số client đồng thời, tránh quá tải server.
     */
    private final ExecutorService executor = Executors.newFixedThreadPool(16);

    public SocketServer(int port) {
        this.port = port;
    }

    /**
     * Bắt đầu lắng nghe kết nối. Phương thức này BLOCKING (không bao giờ return
     * trừ khi xảy ra lỗi), nên phải gọi sau khi mọi khởi tạo đã xong.
     */
    public void start() {
        // Lấy các Singleton service – chỉ có một instance duy nhất trong JVM.
        AuthService authService = AuthService.getInstance();
        AuctionService auctionService = AuctionService.getInstance();
        ItemService itemService = ItemService.getInstance();
        BroadcastManager broadcastManager = BroadcastManager.getInstance();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOG.info("Received shutdown signal. Commencing graceful shutdown...");
            try {
                broadcastManager.shutdown();
                
                LOG.info("Shutting down SocketServer thread pool...");
                executor.shutdown();
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
                
                LOG.info("Closing database connection pool...");
                DBConnection.closePool();
                LOG.info("Graceful shutdown completed. Goodbye!");
            } catch (Exception e) {
                LOG.error("Error during shutdown", e);
            }
        }));

        LOG.info("Socket accept loop started on port {}", port);
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            // Vòng lặp vô tận: chờ client → tạo handler → tiếp tục chờ.
            while (true) {
                Socket socket = serverSocket.accept(); // blocking – chờ client kết nối
                ClientHandler handler = new ClientHandler(socket, authService, auctionService,
                        itemService, broadcastManager);
                executor.submit(handler); // chạy handler trên thread riêng
            }
        } catch (IOException e) {
            LOG.error("Server failed on port {}", port, e);
=======

public class SocketServer {
    private final int port;
    private final AuthService authService;
    private final ItemService itemService;
    private final AuctionService auctionService;
    private final WalletService walletService;
    private final AuctionBroadcaster broadcasterService;
    private final ExecutorService executor = Executors.newFixedThreadPool(16);

    public SocketServer(
            int port,
            AuthService authService,
            ItemService itemService,
            AuctionService auctionService,
            WalletService walletService,
            AuctionBroadcaster broadcasterService
    ) {
        this.port = port;
        this.authService = authService;
        this.itemService = itemService;
        this.auctionService = auctionService;
        this.walletService = walletService;
        this.broadcasterService = broadcasterService;
    }

    public void start() {
        System.out.println("[server] Starting on port " + port);
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket socket = serverSocket.accept();
                executor.submit(new ClientHandler(socket, authService, itemService, auctionService, walletService, broadcasterService));
            }
        } catch (IOException e) {
>>>>>>> a4a9980ce3593461ea601ec5d280f231fec24645
            throw new RuntimeException("Server failed to start", e);
        }
    }
}
