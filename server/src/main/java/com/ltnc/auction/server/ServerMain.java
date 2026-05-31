package com.ltnc.auction.server;

<<<<<<< HEAD
import com.ltnc.auction.server.config.ServerConfig;
import com.ltnc.auction.server.db.DataSeeder;
import com.ltnc.auction.server.db.SchemaInitializer;
import com.ltnc.auction.server.network.SocketServer;
import com.ltnc.auction.server.service.AuctionStateManager;
import com.ltnc.auction.server.service.AuctionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ============================================================
 *  ĐIỂM KHỞI ĐỘNG CỦA SERVER (Entry Point)
 * ============================================================
 *
 *  Luồng khởi động theo thứ tự:
 *
 *  [ServerMain]
 *      │
 *      ├─ Bước 1: SchemaInitializer  → Tạo bảng trong DB (nếu chưa có)
 *      │          DataSeeder          → Thêm dữ liệu mẫu để test
 *      │
 *      ├─ Bước 2: AuctionService.initialize() → Nạp các phiên đang OPEN/RUNNING
 *      │          vào bộ nhớ (ConcurrentHashMap) để xử lý nhanh
 *      │
 *      ├─ Bước 3: AuctionStateManager.start() → Chạy "đồng hồ" mỗi 1 giây
 *      │          để tự động chuyển trạng thái phiên đấu giá
 *      │          (OPEN → RUNNING → FINISHED)
 *      │
 *      └─ Bước 4: SocketServer.start() → Mở cổng TCP, chờ client kết nối
 *                 (vòng lặp vô tận, blocking)
 *
 *  Sau khi khởi động xong, mọi tương tác với client đều qua SocketServer.
 */
public class ServerMain {
    private static final Logger LOG = LoggerFactory.getLogger(ServerMain.class);

    public static void main(String[] args) {
        SchemaInitializer.initialize();
        DataSeeder.seed();

        AuctionService.getInstance().initialize();
        AuctionStateManager.getInstance().start();

        int port = ServerConfig.resolveListenPort(args);
        LOG.info("BidMaster server listening on TCP port {} (override with arg[0] or AUCTION_SERVER_PORT)", port);
        new SocketServer(port).start();
    }
}
=======
import com.ltnc.auction.server.dao.AuctionDAO;
import com.ltnc.auction.server.dao.BidDAO;
import com.ltnc.auction.server.dao.ItemDAO;
import com.ltnc.auction.server.dao.UserDAO;
import com.ltnc.auction.server.dao.WalletDAO;
import com.ltnc.auction.server.dao.WalletTransactionDAO;
import com.ltnc.auction.server.db.SchemaInitializer;
import com.ltnc.auction.server.network.SocketServer;
import com.ltnc.auction.server.services.AuctionService;
import com.ltnc.auction.server.services.AuthService;
import com.ltnc.auction.server.services.ItemService;
import com.ltnc.auction.server.services.WalletService;
import com.ltnc.auction.server.services.AuctionStateManager;
import com.ltnc.auction.server.network.AuctionBroadcaster;

public class ServerMain 
{
    public static void main(String[] args) 
    {
        int port = 5555;

        if (args.length >= 1) 
        {
            port = Integer.parseInt(args[0]);
        }

        System.out.println("Starting server on port " + port);
        SchemaInitializer.initialize();

        UserDAO userDAO = new UserDAO();
        ItemDAO itemDAO = new ItemDAO();
        AuctionDAO auctionDAO = new AuctionDAO();
        BidDAO bidDAO = new BidDAO();
        WalletDAO walletDAO = new WalletDAO();
        WalletTransactionDAO walletTransactionDAO = new WalletTransactionDAO();
        AuctionBroadcaster broadcaster = new AuctionBroadcaster();

        AuthService authService = new AuthService(userDAO);
        ItemService itemService = new ItemService(itemDAO, authService);
        AuctionService auctionService = new AuctionService(auctionDAO, bidDAO, userDAO, walletDAO, walletTransactionDAO, broadcaster);
        WalletService walletService = new WalletService(walletDAO, walletTransactionDAO);
        AuctionStateManager auctionStateManager = new AuctionStateManager(auctionDAO, broadcaster);

        SocketServer server = new SocketServer(port, authService, itemService, auctionService, walletService, broadcaster);
        server.start();
    }
}
>>>>>>> a4a9980ce3593461ea601ec5d280f231fec24645
