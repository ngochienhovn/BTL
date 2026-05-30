package com.ltnc.auction.server;

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
