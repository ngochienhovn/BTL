package com.ltnc.auction.server.db;

import com.ltnc.auction.server.service.PasswordUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataSeeder {

    private static final Logger LOG = LoggerFactory.getLogger(DataSeeder.class);

    private DataSeeder() {}


    public static void seed() {
        try (Connection conn = DBConnection.getConnection()) {

            clearOldData(conn);

            LocalDateTime now = LocalDateTime.now();

            // =======================================================
            // DEMO DATABASE - 7 AUCTION USE CASES
            // =======================================================
            // 1. RUNNING  - Có nhiều bid
            // 2. RUNNING  - Không có bid
            // 3. OPEN     - Sắp mở, chưa có bid
            // 4. FINISHED - Có winner, chưa thanh toán
            // 5. PAID     - Đã thanh toán xong
            // 6. FINISHED - Không có bid
            // 7. CANCELED - Phiên bị hủy
            // =======================================================

            // =======================================================
            // USERS
            // =======================================================

            long admin1 = getUserId(conn, "admin@gmail.com");

            long seller1 = getUserId(conn, "seller@gmail.com");
            long seller2 = seedUser(conn, "Nguyen Thi Lan", "lan@gmail.com", "Lan@123", "SELLER");
            long seller3 = seedUser(conn, "Pham Quoc Bao", "bao.seller@gmail.com", "Bao@123", "SELLER");

            long bidder1 = getUserId(conn, "demo@gmail.com");
            long bidder2 = seedUser(conn, "Tran Van Binh", "binh@gmail.com", "Binh@123", "BIDDER");
            long bidder3 = seedUser(conn, "Le Thi Hoa", "hoa@gmail.com", "Hoa@123", "BIDDER");
            long bidder4 = seedUser(conn, "Mai Minh Anh", "anh@gmail.com", "Anh@123", "BIDDER");
            long bidder5 = seedUser(conn, "Vu Gia Khanh", "khanh@gmail.com", "Khanh@123", "BIDDER");

            // =======================================================
            // WALLETS
            // =======================================================
            // balance  = tổng tiền user có
            // reserved = tiền đang bị giữ do user đang thắng phiên chưa thanh toán

            upsertWallet(conn, admin1, 1_000_000_000, 0, now);

            upsertWallet(conn, seller1, 120_000_000, 0, now);
            upsertWallet(conn, seller2, 92_500_000, 0, now);   // 80M + 12.5M nhận từ phiên PAID Canon
            upsertWallet(conn, seller3, 45_000_000, 0, now);   // Sony FINISHED nhưng chưa thanh toán nên seller chưa nhận tiền

            upsertWallet(conn, bidder1, 167_500_000, 0, now);  // đã thanh toán 12.5M phiên Canon
            upsertWallet(conn, bidder2, 260_000_000, 29_500_000, now); // đang giữ tiền phiên Laptop
            upsertWallet(conn, bidder3, 140_000_000, 6_800_000, now);  // thắng Sony nhưng chưa thanh toán
            upsertWallet(conn, bidder4, 85_000_000, 0, now);
            upsertWallet(conn, bidder5, 42_500_000, 0, now);

            // =======================================================
            // WALLET TRANSACTIONS - NẠP TIỀN BAN ĐẦU
            // =======================================================

            insertWalletTx(conn, bidder1, "DEPOSIT", 180_000_000, null, now.minusDays(7));
            insertWalletTx(conn, bidder2, "DEPOSIT", 260_000_000, null, now.minusDays(6));
            insertWalletTx(conn, bidder3, "DEPOSIT", 140_000_000, null, now.minusDays(5));
            insertWalletTx(conn, bidder4, "DEPOSIT", 85_000_000, null, now.minusDays(4));
            insertWalletTx(conn, bidder5, "DEPOSIT", 42_500_000, null, now.minusDays(3));

            // =======================================================
            // ITEMS - 7 SẢN PHẨM
            // =======================================================

            long item1 = insertItem(conn, seller1, "seller@gmail.com", "ELECTRONICS",
                    "Laptop Dell XPS 15 OLED 2024",
                    "Dell XPS 15 OLED 2024, Core i7, RAM 32GB, SSD 1TB, màn OLED 3.5K. Máy còn bảo hành, ngoại hình 98%, phù hợp lập trình viên, designer và editor.",
                    24_000_000,
                    "https://images.unsplash.com/photo-1593642632823-8f785ba67e45?auto=format&fit=crop&w=1200&q=80");

            long item2 = insertItem(conn, seller1, "seller@gmail.com", "ELECTRONICS",
                    "iPhone 15 Pro Max 256GB",
                    "iPhone 15 Pro Max 256GB màu Titan Tự Nhiên, fullbox, pin tốt, ngoại hình đẹp. ",
                    22_000_000,
                    "https://tse4.mm.bing.net/th/id/OIP.HdFzMBPOKKKvmqjw28b1NAHaE7?pid=Api&P=0&h=180");

            long item3 = insertItem(conn, seller2, "lan@gmail.com", "ART",
                    "Tranh sơn dầu Hoàng Hôn Hạ Long",
                    "Tranh sơn dầu trên canvas kích thước 80x120cm, chủ đề hoàng hôn Vịnh Hạ Long.",
                    8_000_000,
                    "https://images.unsplash.com/photo-1579783902614-a3fb3927b6a5?auto=format&fit=crop&w=1200&q=80");

            long item4 = insertItem(conn, seller3, "bao.seller@gmail.com", "ELECTRONICS",
                    "Sony WH-1000XM5 Headphones",
                    "Tai nghe Sony WH-1000XM5 chống ồn chủ động, màu đen, fullbox, dùng 6 tháng. ",
                    5_500_000,
                    "https://images.unsplash.com/photo-1618366712010-f4ae9c647dcb?auto=format&fit=crop&w=1200&q=80");

            long item5 = insertItem(conn, seller2, "lan@gmail.com", "ELECTRONICS",
                    "Máy ảnh Canon EOS M50 Mark II",
                    "Máy ảnh Canon EOS M50 Mark II kèm lens kit, ngoại hình đẹp. ",
                    10_000_000,
                    "https://images.unsplash.com/photo-1516035069371-29a1b244cc32?auto=format&fit=crop&w=1200&q=80");

            long item6 = insertItem(conn, seller2, "lan@gmail.com", "ART",
                    "Bình gốm thủ công Bát Tràng",
                    "Bình gốm Bát Tràng thủ công, men hỏa biến, cao 45cm.",
                    3_200_000,
                    "https://images.unsplash.com/photo-1610701596007-11502861dcfa?auto=format&fit=crop&w=1200&q=80");

            long item7 = insertItem(conn, seller3, "bao.seller@gmail.com", "WATCHES",
                    "Đồng hồ Seiko 5 Automatic",
                    "Đồng hồ Seiko 5 Automatic, máy cơ, dây thép. ",
                    2_500_000,
                    "https://images.unsplash.com/photo-1524592094714-0f0654e20314?auto=format&fit=crop&w=1200&q=80");

            // =======================================================
            // =======================================================
            // AUCTIONS - 7 PHIÊN ĐẤU GIÁ
            // =======================================================

            // 1. Laptop - RUNNING - Có nhiều bid
            long auc1 = insertAuction(conn, item1,
                    "Laptop Dell XPS 15 OLED 2024",
                    "Laptop cao cấp cho designer/dev, màn OLED, RAM 32GB, SSD 1TB.",
                    "ELECTRONICS",
                    24_000_000,
                    29_500_000,
                    now.minusHours(2),
                    now.plusHours(6),
                    "RUNNING",
                    bidder2,
                    "binh@gmail.com",
                    "seller@gmail.com",
                    "https://images.unsplash.com/photo-1593642632823-8f785ba67e45?auto=format&fit=crop&w=1200&q=80");

            // 2. iPhone - RUNNING - Không có bid
            long auc2 = insertAuction(conn, item2,
                    "iPhone 15 Pro Max 256GB",
                    "iPhone 15 Pro Max 256GB, fullbox, màu Titan Tự Nhiên. Phiên này đang chạy nhưng chưa có người bid.",
                    "ELECTRONICS",
                    22_000_000,
                    22_000_000,
                    now.minusMinutes(30),
                    now.plusHours(10),
                    "RUNNING",
                    null,
                    null,
                    "seller@gmail.com",
                    "https://tse4.mm.bing.net/th/id/OIP.HdFzMBPOKKKvmqjw28b1NAHaE7?pid=Api&P=0&h=180");

            // 3. Tranh - OPEN - Sắp mở, chưa có bid
            long auc3 = insertAuction(conn, item3,
                    "Tranh sơn dầu Hoàng Hôn Hạ Long",
                    "Tác phẩm nghệ thuật độc bản, có chứng nhận tác giả.",
                    "ART",
                    8_000_000,
                    8_000_000,
                    now.plusHours(4),
                    now.plusDays(2),
                    "OPEN",
                    null,
                    null,
                    "lan@gmail.com",
                    "https://images.unsplash.com/photo-1579783902614-a3fb3927b6a5?auto=format&fit=crop&w=1200&q=80");

            // 4. Sony - FINISHED - Có winner nhưng chưa thanh toán
            long auc4 = insertAuction(conn, item4,
                    "Sony WH-1000XM5 Headphones",
                    "Tai nghe chống ồn Sony WH-1000XM5, fullbox. Phiên này đã kết thúc, có người thắng nhưng chưa thanh toán.",
                    "ELECTRONICS",
                    5_500_000,
                    6_800_000,
                    now.minusDays(2),
                    now.minusHours(1),
                    "FINISHED",
                    bidder3,
                    "hoa@gmail.com",
                    "bao.seller@gmail.com",
                    "https://images.unsplash.com/photo-1618366712010-f4ae9c647dcb?auto=format&fit=crop&w=1200&q=80");

            // 5. Canon - PAID - Đã thanh toán xong
            long auc5 = insertAuction(conn, item5,
                    "Máy ảnh Canon EOS M50 Mark II",
                    "Máy ảnh Canon EOS M50 Mark II. Phiên này đã thanh toán xong.",
                    "ELECTRONICS",
                    10_000_000,
                    12_500_000,
                    now.minusDays(4),
                    now.minusDays(3),
                    "PAID",
                    bidder1,
                    "demo@gmail.com",
                    "lan@gmail.com",
                    "https://images.unsplash.com/photo-1516035069371-29a1b244cc32?auto=format&fit=crop&w=1200&q=80");

            // 6. Bình gốm - FINISHED - Không có bid
            long auc6 = insertAuction(conn, item6,
                    "Bình gốm thủ công Bát Tràng",
                    "Bình gốm thủ công Bát Tràng, men hỏa biến. Phiên này đã kết thúc nhưng không có người đặt giá.",
                    "ART",
                    3_200_000,
                    3_200_000,
                    now.minusDays(3),
                    now.minusDays(1),
                    "FINISHED",
                    null,
                    null,
                    "lan@gmail.com",
                    "https://images.unsplash.com/photo-1610701596007-11502861dcfa?auto=format&fit=crop&w=1200&q=80");

            // 7. Seiko - CANCELED - Phiên bị hủy
            long auc7 = insertAuction(conn, item7,
                    "Đồng hồ Seiko 5 Automatic",
                    "Đồng hồ Seiko 5 Automatic. Phiên này đã bị hủy.",
                    "WATCHES",
                    2_500_000,
                    2_800_000,
                    now.minusDays(1),
                    now.plusHours(3),
                    "CANCELED",
                    null,
                    null,
                    "bao.seller@gmail.com",
                    "https://images.unsplash.com/photo-1524592094714-0f0654e20314?auto=format&fit=crop&w=1200&q=80");
            // =======================================================
            // BID HISTORY
            // =======================================================

            // UC1: Laptop - RUNNING - Có nhiều lượt bid
            insertBid(conn, auc1, bidder5, "khanh@gmail.com", "Vu Gia Khanh", 24_500_000, now.minusMinutes(118));
            insertBid(conn, auc1, bidder1, "demo@gmail.com", "Demo Bidder", 25_000_000, now.minusMinutes(111));
            insertBid(conn, auc1, bidder4, "anh@gmail.com", "Mai Minh Anh", 25_500_000, now.minusMinutes(103));
            insertBid(conn, auc1, bidder3, "hoa@gmail.com", "Le Thi Hoa", 26_000_000, now.minusMinutes(96));
            insertBid(conn, auc1, bidder2, "binh@gmail.com", "Tran Van Binh", 26_500_000, now.minusMinutes(88));
            insertBid(conn, auc1, bidder1, "demo@gmail.com", "Demo Bidder", 27_000_000, now.minusMinutes(81));
            insertBid(conn, auc1, bidder5, "khanh@gmail.com", "Vu Gia Khanh", 27_500_000, now.minusMinutes(73));
            insertBid(conn, auc1, bidder4, "anh@gmail.com", "Mai Minh Anh", 28_000_000, now.minusMinutes(66));
            insertBid(conn, auc1, bidder3, "hoa@gmail.com", "Le Thi Hoa", 28_500_000, now.minusMinutes(54));
            insertBid(conn, auc1, bidder1, "demo@gmail.com", "Demo Bidder", 29_000_000, now.minusMinutes(41));
            insertBid(conn, auc1, bidder4, "anh@gmail.com", "Mai Minh Anh", 29_200_000, now.minusMinutes(25));
            insertBid(conn, auc1, bidder2, "binh@gmail.com", "Tran Van Binh", 29_500_000, now.minusMinutes(4));

            // UC2: iPhone - RUNNING - Không có bid
            // Không insertBid cho auc2

            // UC3: Tranh - OPEN/upcoming - Chưa có bid
            // Không insertBid cho auc3

            // UC4: Sony - FINISHED - Có winner nhưng chưa thanh toán
            insertBid(conn, auc4, bidder4, "anh@gmail.com", "Mai Minh Anh", 5_600_000, now.minusDays(1).minusHours(23));
            insertBid(conn, auc4, bidder5, "khanh@gmail.com", "Vu Gia Khanh", 5_800_000, now.minusDays(1).minusHours(22));
            insertBid(conn, auc4, bidder1, "demo@gmail.com", "Demo Bidder", 6_000_000, now.minusDays(1).minusHours(21));
            insertBid(conn, auc4, bidder2, "binh@gmail.com", "Tran Van Binh", 6_200_000, now.minusDays(1).minusHours(20));
            insertBid(conn, auc4, bidder5, "khanh@gmail.com", "Vu Gia Khanh", 6_400_000, now.minusDays(1).minusHours(19));
            insertBid(conn, auc4, bidder1, "demo@gmail.com", "Demo Bidder", 6_600_000, now.minusDays(1).minusHours(18));
            insertBid(conn, auc4, bidder3, "hoa@gmail.com", "Le Thi Hoa", 6_800_000, now.minusDays(1).minusHours(17));

            // UC5: Canon - PAID - Đã thanh toán xong
            insertBid(conn, auc5, bidder4, "anh@gmail.com", "Mai Minh Anh", 10_500_000, now.minusDays(3).minusHours(23));
            insertBid(conn, auc5, bidder2, "binh@gmail.com", "Tran Van Binh", 11_000_000, now.minusDays(3).minusHours(22));
            insertBid(conn, auc5, bidder1, "demo@gmail.com", "Demo Bidder", 12_500_000, now.minusDays(3).minusHours(21));

            // UC6: Bình gốm - FINISHED - Không có bid
            // Không insertBid cho auc6

            // UC7: Seiko - CANCELED - Có bid trước khi bị hủy
            insertBid(conn, auc7, bidder5, "khanh@gmail.com", "Vu Gia Khanh", 2_800_000, now.minusHours(20));

            // =======================================================
            // WALLET TRANSACTIONS LIÊN QUAN ĐẤU GIÁ
            // =======================================================

            // UC1 - Laptop đang chạy
            insertWalletTx(conn, bidder1, "RESERVE", 29_000_000, auc1, now.minusMinutes(41));
            insertWalletTx(conn, bidder1, "RELEASE", 29_000_000, auc1, now.minusMinutes(25));

            insertWalletTx(conn, bidder4, "RESERVE", 29_200_000, auc1, now.minusMinutes(25));
            insertWalletTx(conn, bidder4, "RELEASE", 29_200_000, auc1, now.minusMinutes(4));

            insertWalletTx(conn, bidder2, "RESERVE", 29_500_000, auc1, now.minusMinutes(4));

            // UC4 - Sony FINISHED có winner nhưng chưa thanh toán
            insertWalletTx(conn, bidder3, "RESERVE", 6_800_000, auc4, now.minusDays(1).minusHours(17));

            // UC5 - Canon PAID đã thanh toán xong
            insertWalletTx(conn, bidder1, "RESERVE", 12_500_000, auc5, now.minusDays(3).minusHours(21));
            insertWalletTx(conn, bidder1, "PAYMENT", 12_500_000, auc5, now.minusDays(3).minusHours(20));
            insertWalletTx(conn, seller2, "RECEIVE", 12_500_000, auc5, now.minusDays(3).minusHours(20));

            // UC7 - Seiko CANCELED nên hoàn tiền giữ chỗ
            insertWalletTx(conn, bidder5, "RESERVE", 2_800_000, auc7, now.minusHours(20));
            insertWalletTx(conn, bidder5, "RELEASE", 2_800_000, auc7, now.minusHours(19));

            // =======================================================
            // NOTIFICATIONS
            // =======================================================
            insertNotification(conn, admin1, "System Alert", "System is running smoothly. 35 bulk auctions generated.", "INFO", false, now.minusHours(1));
            insertNotification(conn, seller1, "Sản phẩm được quan tâm", "Laptop Dell XPS của bạn đang nhận được nhiều lượt trả giá!", "SUCCESS", false, now.minusMinutes(30));
            insertNotification(conn, bidder1, "Chào mừng", "Chào mừng bạn đến với hệ thống đấu giá BidMaster!", "INFO", false, now.minusDays(1));
            insertNotification(conn, bidder1, "Sắp kết thúc", "Phiên đấu giá Laptop Dell XPS 15 sắp kết thúc. Hãy nhanh tay trả giá!", "WARNING", false, now.minusMinutes(10));

            // =======================================================
            // AUTO-BID CHO DEMO
            // =======================================================
            // Cài đặt cho binh@gmail.com tự động trả giá đè ở UC1 (Laptop)
            insertAutoBid(conn, auc1, bidder2, "binh@gmail.com", 35_000_000, 500_000);
            
            // Cài đặt cho khanh@gmail.com tự động trả giá đè ở UC2 (iPhone)
            insertAutoBid(conn, auc2, bidder5, "khanh@gmail.com", 25_000_000, 200_000);

            LOG.info("Fake seed data inserted successfully (7 auction use cases)");
            LOG.info("Demo users: demo@gmail.com / seller@gmail.com / admin@gmail.com");
            LOG.info("Extra users: binh@gmail.com, hoa@gmail.com, anh@gmail.com, khanh@gmail.com");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to seed fake data", e);
        }
    }

private static void clearOldData(Connection conn) throws SQLException {
    try (Statement st = conn.createStatement()) {
        st.execute("SET REFERENTIAL_INTEGRITY FALSE");

        st.executeUpdate("DELETE FROM notifications");
        st.executeUpdate("DELETE FROM auto_bid_configs");
        st.executeUpdate("DELETE FROM bid_transactions");
        st.executeUpdate("DELETE FROM wallet_transactions");
        st.executeUpdate("DELETE FROM auctions");
        st.executeUpdate("DELETE FROM items");
        st.executeUpdate("DELETE FROM wallets");

        st.executeUpdate("""
            DELETE FROM users
            WHERE username NOT IN ('admin@gmail.com', 'seller@gmail.com', 'demo@gmail.com')
        """);

        st.execute("SET REFERENTIAL_INTEGRITY TRUE");
    }
}
    private static boolean isEmpty(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM items")) {
            return rs.next() && rs.getLong(1) == 0;
        }
    }

    private static long getUserId(Connection conn, String email) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id FROM users WHERE username = ?")) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        throw new SQLException("User not found: " + email);
    }

    private static long seedUser(Connection conn, String fullName, String email,
                                  String password, String role) throws SQLException {
        try (PreparedStatement check = conn.prepareStatement(
                "SELECT id FROM users WHERE username = ?")) {
            check.setString(1, email);
            try (ResultSet rs = check.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO users (username, full_name, password_hash, role) VALUES (?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, email);
            ps.setString(2, fullName);
            ps.setString(3, PasswordUtil.hash(password));
            ps.setString(4, role);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    private static void upsertWallet(Connection conn, long userId, double balance, double reserved,
            LocalDateTime updatedAt) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "MERGE INTO wallets (user_id, balance, reserved, updated_at) KEY(user_id) VALUES (?,?,?,?)")) {
            ps.setLong(1, userId);
            ps.setDouble(2, balance);
            ps.setDouble(3, reserved);
            ps.setTimestamp(4, Timestamp.valueOf(updatedAt));
            ps.executeUpdate();
        }
    }

    private static void insertWalletTx(Connection conn, long userId, String type, double amount,
            Long refAuctionId, LocalDateTime createdAt) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO wallet_transactions (user_id, type, amount, ref_auction_id, created_at) VALUES (?,?,?,?,?)")) {
            ps.setLong(1, userId);
            ps.setString(2, type);
            ps.setDouble(3, amount);
            if (refAuctionId == null) {
                ps.setNull(4, Types.BIGINT);
            } else {
                ps.setLong(4, refAuctionId);
            }
            ps.setTimestamp(5, Timestamp.valueOf(createdAt));
            ps.executeUpdate();
        }
    }

    private static void seedBulkDemoData(
            Connection conn,
            LocalDateTime now,
            long[] sellerIds,
            String[] sellerEmails,
            long[] bidderIds,
            String[] bidderEmails) throws SQLException {
        String[] categories = {"ELECTRONICS", "ART", "VEHICLE", "COLLECTIBLES", "WATCHES"};
        String[] statuses = {"RUNNING", "OPEN", "FINISHED", "CANCELED", "PAID"};

        // 20 user profiles giả lập bổ sung: mỗi user có wallet + lịch sử nạp/rút.
        for (int i = 0; i < 20; i++) {
            String email = "bulk.bidder" + (i + 1) + "@gmail.com";
            long uid = seedUser(conn, "Bulk Bidder " + (i + 1), email, "Bulk@123", "BIDDER");
            double balance = 12_000_000 + (i * 5_350_000);
            double reserved = (i % 3 == 0) ? Math.min(3_900_000 + (i * 355_000), balance * 0.45) : 0;
            upsertWallet(conn, uid, balance, reserved, now.minusMinutes(i * 7L));
            insertWalletTx(conn, uid, "DEPOSIT", balance + 4_000_000, null, now.minusDays(2).minusMinutes(i));
            if (i % 4 == 0) {
                insertWalletTx(conn, uid, "WITHDRAW", 1_400_000 + (i * 120_000), null, now.minusHours(20).minusMinutes(i));
            }
        }

        // 35 item + auction với trạng thái xen kẽ, kèm bid history/auto-bid/wallet tx.
        for (int i = 0; i < 35; i++) {
            int sellerIdx = i % sellerIds.length;
            int bidderLeadIdx = i % bidderIds.length;
            int bidderAltIdx = (i + 2) % bidderIds.length;

            String category = categories[i % categories.length];
            String status = statuses[i % statuses.length];
            double startingBid = 5_000_000 + (i * 1_350_000);
            double currentBid = startingBid + ((i % 6) * 850_000);

            long itemId = insertItem(
                    conn,
                    sellerIds[sellerIdx],
                    sellerEmails[sellerIdx],
                    category,
                    "[Bulk] " + category + " Item " + (i + 1),
                    "Bulk generated dataset item #" + (i + 1) + " for stress testing feed/filter/detail views.",
                    startingBid,
                    "https://picsum.photos/seed/auction-" + (100 + i) + "/1200/800.jpg");

            LocalDateTime start;
            LocalDateTime end;
            Long highestBidderId;
            String winnerEmail;
            if ("OPEN".equals(status)) {
                start = now.plusMinutes(20L + i);
                end = start.plusHours(4);
                currentBid = startingBid;
                highestBidderId = null;
                winnerEmail = null;
            } else if ("RUNNING".equals(status)) {
                start = now.minusHours(2).minusMinutes(i * 2L);
                end = now.plusMinutes(30L + i);
                highestBidderId = bidderIds[bidderLeadIdx];
                winnerEmail = bidderEmails[bidderLeadIdx];
            } else {
                start = now.minusDays(1).minusHours(i % 8);
                end = now.minusHours(1).minusMinutes(i * 3L);
                highestBidderId = ("CANCELED".equals(status)) ? null : bidderIds[bidderLeadIdx];
                winnerEmail = ("CANCELED".equals(status)) ? null : bidderEmails[bidderLeadIdx];
            }

            long auctionId = insertAuction(
                    conn,
                    itemId,
                    "[Bulk] Auction " + (i + 1),
                    "Bulk generated auction #" + (i + 1) + " to cover many UI and business cases.",
                    category,
                    startingBid,
                    currentBid,
                    start,
                    end,
                    status,
                    highestBidderId,
                    winnerEmail,
                    sellerEmails[sellerIdx],
                    "https://picsum.photos/seed/auction-cover-" + (100 + i) + "/1200/800.jpg");

            if (!"OPEN".equals(status) && !"CANCELED".equals(status)) {
                insertBid(conn, auctionId, bidderIds[bidderAltIdx], bidderEmails[bidderAltIdx],
                        "Bulk Bidder Alt", startingBid + 500_000, end.minusHours(2));
                insertBid(conn, auctionId, bidderIds[bidderLeadIdx], bidderEmails[bidderLeadIdx],
                        "Bulk Bidder Lead", currentBid, end.minusMinutes(12));
                insertBidSeries(conn, auctionId, bidderIds[bidderLeadIdx], bidderEmails[bidderLeadIdx],
                        "Bulk Bidder Lead", currentBid + 250_000, 6 + (i % 5), 125_000, end.minusMinutes(10));
            }

            if ("RUNNING".equals(status) && (i % 2 == 0)) {
                insertAutoBid(conn, auctionId, bidderIds[bidderAltIdx], bidderEmails[bidderAltIdx],
                        currentBid + 3_000_000, 400_000 + ((i % 3) * 250_000));
                insertWalletTx(conn, bidderIds[bidderLeadIdx], "RESERVE", Math.max(500_000, currentBid * 0.2),
                        auctionId, now.minusMinutes(5 + i));
            }
        }
    }

    private static long insertItem(Connection conn, long sellerId, String sellerEmail,
                                    String type, String name, String description,
                                    double startingBid, String imageUrl) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO items (seller_id, seller_email, type, name, description, starting_bid, image_url) " +
                "VALUES (?,?,?,?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, sellerId);
            ps.setString(2, sellerEmail);
            ps.setString(3, type);
            ps.setString(4, name);
            ps.setString(5, description);
            ps.setDouble(6, startingBid);
            ps.setString(7, imageUrl);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    private static long insertAuction(Connection conn, long itemId, String title,
                                       String description, String category,
                                       double startingBid, double currentBid,
                                       LocalDateTime startTime, LocalDateTime endTime,
                                       String status, Long highestBidderId,
                                       String winnerEmail, String sellerEmail,
                                       String imageUrl) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO auctions (item_id, title, description, category, starting_bid, current_bid, " +
                "start_time, end_time, status, highest_bidder_id, winner_email, seller_email, image_url) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, itemId);
            ps.setString(2, title);
            ps.setString(3, description);
            ps.setString(4, category);
            ps.setDouble(5, startingBid);
            ps.setDouble(6, currentBid);
            ps.setTimestamp(7, Timestamp.valueOf(startTime));
            ps.setTimestamp(8, Timestamp.valueOf(endTime));
            ps.setString(9, status);
            if (highestBidderId != null) ps.setLong(10, highestBidderId);
            else ps.setNull(10, Types.BIGINT);
            ps.setString(11, winnerEmail);
            ps.setString(12, sellerEmail);
            ps.setString(13, imageUrl);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    private static void insertBid(Connection conn, long auctionId, long bidderId,
                                   String bidderEmail, String bidderName,
                                   double amount, LocalDateTime createdAt) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO bid_transactions (auction_id, bidder_id, bidder_email, bidder_name, amount, created_at) " +
                "VALUES (?,?,?,?,?,?)")) {
            ps.setLong(1, auctionId);
            ps.setLong(2, bidderId);
            ps.setString(3, bidderEmail);
            ps.setString(4, bidderName);
            ps.setDouble(5, amount);
            ps.setTimestamp(6, Timestamp.valueOf(createdAt));
            ps.executeUpdate();
        }
    }

    private static void insertBidSeries(
            Connection conn,
            long auctionId,
            long bidderId,
            String bidderEmail,
            String bidderName,
            double startAmount,
            int count,
            double step,
            LocalDateTime startTime) throws SQLException {
        for (int i = 0; i < count; i++) {
            insertBid(
                    conn,
                    auctionId,
                    bidderId,
                    bidderEmail,
                    bidderName,
                    startAmount + (i * step),
                    startTime.plusSeconds(i * 15L));
        }
    }

    private static void insertAutoBid(Connection conn, long auctionId, long bidderId,
                                       String bidderEmail, double maxBid,
                                       double incrementAmount) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO auto_bid_configs (auction_id, bidder_id, bidder_email, max_bid, increment_amount, registered_at) " +
                "VALUES (?,?,?,?,?,?)")) {
            ps.setLong(1, auctionId);
            ps.setLong(2, bidderId);
            ps.setString(3, bidderEmail);
            ps.setDouble(4, maxBid);
            ps.setDouble(5, incrementAmount);
            ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        }
    }
    private static void insertNotification(Connection conn, long userId, String title,
                                           String message, String type, boolean isRead,
                                           LocalDateTime createdAt) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO notifications (user_id, title, message, type, is_read, created_at) " +
                "VALUES (?,?,?,?,?,?)")) {
            ps.setLong(1, userId);
            ps.setString(2, title);
            ps.setString(3, message);
            ps.setString(4, type);
            ps.setBoolean(5, isRead);
            ps.setTimestamp(6, Timestamp.valueOf(createdAt));
            ps.executeUpdate();
        }
    }
}
