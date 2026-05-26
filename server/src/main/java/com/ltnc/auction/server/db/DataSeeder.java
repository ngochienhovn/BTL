package com.ltnc.auction.server.db;

import com.ltnc.auction.server.services.PasswordUtil;
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
            if (hasAuctionData(conn)) {
                LOG.info("Seed data already present, skipping");
                return;
            }

            LocalDateTime now = LocalDateTime.now();

            // ── Users: đa role, đa wallet profile ───────────────────────────────
            long admin1 = getUserId(conn, "admin@gmail.com");
            long admin2 = seedUser(conn, "Ops Admin", "ops.admin@gmail.com", "Ops@123", "ADMIN");

            long seller1 = getUserId(conn, "seller@gmail.com");
            long seller2 = seedUser(conn, "Nguyen Thi Lan", "lan@gmail.com", "Lan@123", "SELLER");
            long seller3 = seedUser(conn, "Pham Quoc Bao", "bao.seller@gmail.com", "Bao@123", "SELLER");
            long seller4 = seedUser(conn, "Do Minh Chau", "chau.seller@gmail.com", "Chau@123", "SELLER");

            long bidder1 = getUserId(conn, "demo@gmail.com");
            long bidder2 = seedUser(conn, "Tran Van Binh", "binh@gmail.com", "Binh@123", "BIDDER");
            long bidder3 = seedUser(conn, "Le Thi Hoa", "hoa@gmail.com", "Hoa@123", "BIDDER");
            long bidder4 = seedUser(conn, "Mai Minh Anh", "anh@gmail.com", "Anh@123", "BIDDER");
            long bidder5 = seedUser(conn, "Vu Gia Khanh", "khanh@gmail.com", "Khanh@123", "BIDDER");
            long bidder6 = seedUser(conn, "Ngoc Trinh", "trinh@gmail.com", "Trinh@123", "BIDDER");

            // ── Wallets + wallet transactions: nhiều case balance/reserved ─────
            upsertWallet(conn, admin1, 1_000_000_000, 0, now);
            upsertWallet(conn, admin2, 500_000_000, 0, now);
            upsertWallet(conn, seller1, 120_000_000, 0, now);
            upsertWallet(conn, seller2, 80_000_000, 0, now);
            upsertWallet(conn, seller3, 45_000_000, 0, now);
            upsertWallet(conn, seller4, 12_000_000, 0, now);
            upsertWallet(conn, bidder1, 180_000_000, 12_000_000, now);
            upsertWallet(conn, bidder2, 260_000_000, 64_500_000, now);
            upsertWallet(conn, bidder3, 140_000_000, 18_200_000, now);
            upsertWallet(conn, bidder4, 85_000_000, 6_500_000, now);
            upsertWallet(conn, bidder5, 42_500_000, 0, now);
            upsertWallet(conn, bidder6, 15_200_000, 0, now);

            insertWalletTx(conn, bidder1, "DEPOSIT", 210_000_000, null, now.minusDays(6));
            insertWalletTx(conn, bidder1, "WITHDRAW", 30_000_000, null, now.minusDays(5));
            insertWalletTx(conn, bidder1, "RESERVE", 12_000_000, null, now.minusHours(5));

            insertWalletTx(conn, bidder2, "DEPOSIT", 320_000_000, null, now.minusDays(7));
            insertWalletTx(conn, bidder2, "RESERVE", 48_000_000, null, now.minusHours(4));
            insertWalletTx(conn, bidder2, "RESERVE", 16_500_000, null, now.minusMinutes(45));

            insertWalletTx(conn, bidder3, "DEPOSIT", 160_000_000, null, now.minusDays(4));
            insertWalletTx(conn, bidder3, "WITHDRAW", 20_000_000, null, now.minusDays(1));
            insertWalletTx(conn, bidder3, "RESERVE", 18_200_000, null, now.minusHours(1));

            insertWalletTx(conn, bidder6, "DEPOSIT", 15_200_000, null, now.minusHours(2));

            // ── Items ──────────────────────────────────────────────────────────
            long item1 = insertItem(conn, seller1, "seller@gmail.com", "ELECTRONICS",
                    "Laptop Dell XPS 15",
                    "Dell XPS 15 OLED, Core i7-13700H, 32GB RAM, 1TB SSD, RTX 4060",
                    25_000_000, imageUrl("laptop-dell-xps"));
            long item2 = insertItem(conn, seller1, "seller@gmail.com", "ELECTRONICS",
                    "iPhone 15 Pro Max 256GB",
                    "Apple iPhone 15 Pro Max, màu Titan Tự Nhiên, fullbox chưa active",
                    32_000_000, imageUrl("iphone-15-pro-max"));
            long item3 = insertItem(conn, seller2, "lan@gmail.com", "ART",
                    "Tranh sơn dầu 'Hoàng Hôn Hạ Long'",
                    "Tác phẩm sơn dầu trên toan canvas 80x120cm, phong cách hiện thực",
                    5_000_000, imageUrl("oil-painting-halong"));
            long item4 = insertItem(conn, seller2, "lan@gmail.com", "VEHICLE",
                    "Honda Wave Alpha 2022",
                    "Xe máy Honda Wave Alpha 110cc, màu đỏ đen, ODO 8.000km, còn bảo hành",
                    18_000_000, imageUrl("honda-wave-alpha"));
            long item5 = insertItem(conn, seller1, "seller@gmail.com", "ELECTRONICS",
                    "Sony WH-1000XM5 Headphones",
                    "Tai nghe chống ồn Sony WH-1000XM5, màu đen, fullbox",
                    7_500_000, imageUrl("sony-wh-1000xm5"));
            long item6 = insertItem(conn, seller2, "lan@gmail.com", "ART",
                    "Tượng gốm thủ công Bát Tràng",
                    "Bộ 3 tượng gốm men ngọc thủ công làng Bát Tràng, cao 30cm",
                    3_200_000, imageUrl("bat-trang-ceramic"));
            long item7 = insertItem(conn, seller1, "seller@gmail.com", "ELECTRONICS",
                    "Vintage Rolex Submariner 1960s",
                    "Rare vintage Rolex Submariner in excellent condition. Original parts, fully serviced.",
                    8_000_000, imageUrl("vintage-rolex"));
            long item8 = insertItem(conn, seller1, "seller@gmail.com", "VEHICLE",
                    "1967 Ford Mustang Fastback",
                    "Iconic 1967 Mustang Fastback. Complete restoration, numbers matching, V8 engine.",
                    350_000_000, imageUrl("ford-mustang-1967"));
            long item9 = insertItem(conn, seller2, "lan@gmail.com", "ART",
                    "1st Edition Charizard Pokemon Card (PSA 9)",
                    "Extremely rare 1999 Base Set 1st Edition Charizard Holo. Graded PSA 9 Mint.",
                    15_000_000, imageUrl("pokemon-card"));
            long item10 = insertItem(conn, seller3, "bao.seller@gmail.com", "ELECTRONICS",
                    "MacBook Pro M3 16\"",
                    "Máy mới 99%, còn AppleCare, pin 100%, phụ kiện đầy đủ.",
                    46_000_000, imageUrl("macbook-pro-m3"));
            long item11 = insertItem(conn, seller3, "bao.seller@gmail.com", "ART",
                    "Mô hình Gundam PG Unicorn LED",
                    "Bản limited, full phụ kiện, chưa bung seal.",
                    6_800_000, imageUrl("gundam-unicorn"));
            long item12 = insertItem(conn, seller4, "chau.seller@gmail.com", "ART",
                    "Trống đồng mini lưu niệm",
                    "Đồ thủ công mạ đồng, hộp gỗ đi kèm.",
                    1_200_000, imageUrl("bronze-drum"));

            // ── Auctions: đầy đủ OPEN/RUNNING/FINISHED/CANCELED/PAID ─────────
            long auc1 = insertAuction(conn, item1, "Đấu giá Laptop Dell XPS 15",
                    "Laptop gaming/đồ họa cao cấp, xuất xứ USA", "ELECTRONICS",
                    25_000_000, 27_500_000, now.minusHours(1), now.plusHours(2),
                    "RUNNING", bidder2, "binh@gmail.com", "seller@gmail.com", imageUrl("laptop-dell-xps"));
            long auc2 = insertAuction(conn, item2, "Đấu giá iPhone 15 Pro Max",
                    "iPhone chính hãng Apple VN/A, nguyên seal", "ELECTRONICS",
                    32_000_000, 34_000_000, now.minusMinutes(30), now.plusMinutes(45),
                    "RUNNING", bidder1, "demo@gmail.com", "seller@gmail.com", imageUrl("iphone-15-pro-max"));
            long auc3 = insertAuction(conn, item3, "Tranh sơn dầu Hoàng Hôn Hạ Long",
                    "Tác phẩm nghệ thuật độc bản, có chứng nhận tác giả", "ART",
                    5_000_000, 5_000_000, now.plusHours(1), now.plusHours(5),
                    "OPEN", null, null, "lan@gmail.com", imageUrl("oil-painting-halong"));
            long auc4 = insertAuction(conn, item4, "Honda Wave Alpha 2022 ODO thấp",
                    "Xe còn mới, đầy đủ giấy tờ, sang tên ngay", "VEHICLE",
                    18_000_000, 18_000_000, now.plusHours(3), now.plusHours(27),
                    "OPEN", null, null, "lan@gmail.com", imageUrl("honda-wave-alpha"));
            long auc5 = insertAuction(conn, item5, "Sony WH-1000XM5 – Phiên đã kết thúc",
                    "Tai nghe chống ồn cao cấp nhất Sony 2023", "ELECTRONICS",
                    7_500_000, 9_200_000, now.minusHours(5), now.minusHours(1),
                    "FINISHED", bidder3, "hoa@gmail.com", "seller@gmail.com", imageUrl("sony-wh-1000xm5"));
            long auc6 = insertAuction(conn, item6, "Tượng gốm Bát Tràng – Không có người đặt",
                    "Đồ thủ công mỹ nghệ truyền thống Việt Nam", "ART",
                    3_200_000, 3_200_000, now.minusHours(10), now.minusHours(3),
                    "FINISHED", null, null, "lan@gmail.com", imageUrl("bat-trang-ceramic"));
            long auc7 = insertAuction(conn, item7, "Vintage Rolex Submariner 1960s",
                    "Rare vintage Rolex Submariner in excellent condition. Original parts.", "ELECTRONICS",
                    8_000_000, 12_500_000, now.minusHours(4), now.plusDays(2),
                    "RUNNING", bidder1, "demo@gmail.com", "seller@gmail.com",
                    imageUrl("vintage-rolex"));
            long auc8 = insertAuction(conn, item8, "1967 Ford Mustang Fastback",
                    "Iconic 1967 Mustang Fastback. Complete restoration.", "VEHICLE",
                    350_000_000, 450_000_000, now.minusDays(1), now.plusDays(3),
                    "RUNNING", bidder2, "binh@gmail.com", "seller@gmail.com",
                    imageUrl("ford-mustang-1967"));
            long auc9 = insertAuction(conn, item9, "1st Edition Charizard Pokemon Card",
                    "Extremely rare 1999 Base Set.", "ART",
                    15_000_000, 22_000_000, now.minusDays(2), now.plusDays(4),
                    "RUNNING", bidder3, "hoa@gmail.com", "lan@gmail.com",
                    imageUrl("pokemon-card"));
            long auc10 = insertAuction(conn, item10, "MacBook Pro M3 - bản premium",
                    "Case anti-sniping: phiên sắp kết thúc trong 20 giây.", "ELECTRONICS",
                    46_000_000, 49_000_000, now.minusMinutes(20), now.plusSeconds(20),
                    "RUNNING", bidder4, "anh@gmail.com", "bao.seller@gmail.com",
                    imageUrl("macbook-pro-m3"));
            long auc11 = insertAuction(conn, item11, "Gundam PG Unicorn LED",
                    "Dùng để test trạng thái CANCELED.", "ART",
                    6_800_000, 6_800_000, now.minusHours(12), now.minusHours(2),
                    "CANCELED", null, null, "bao.seller@gmail.com",
                    imageUrl("gundam-unicorn"));
            long auc12 = insertAuction(conn, item12, "Trống đồng mini (đã thanh toán)",
                    "Case thanh toán hoàn tất sau phiên thắng.", "ART",
                    1_200_000, 1_950_000, now.minusHours(15), now.minusHours(8),
                    "PAID", bidder5, "khanh@gmail.com", "chau.seller@gmail.com",
                    imageUrl("bronze-drum"));

            // ── Bid history ────────────────────────────────────────────────────
            insertBid(conn, auc1, bidder1, "demo@gmail.com", "Demo Bidder", 25_500_000, now.minusMinutes(55));
            insertBid(conn, auc1, bidder3, "hoa@gmail.com", "Le Thi Hoa", 26_000_000, now.minusMinutes(40));
            insertBid(conn, auc1, bidder2, "binh@gmail.com", "Tran Van Binh", 26_500_000, now.minusMinutes(20));
            insertBid(conn, auc1, bidder1, "demo@gmail.com", "Demo Bidder", 27_000_000, now.minusMinutes(10));
            insertBid(conn, auc1, bidder2, "binh@gmail.com", "Tran Van Binh", 27_500_000, now.minusMinutes(3));

            insertBid(conn, auc2, bidder3, "hoa@gmail.com", "Le Thi Hoa", 32_500_000, now.minusMinutes(25));
            insertBid(conn, auc2, bidder1, "demo@gmail.com", "Demo Bidder", 33_000_000, now.minusMinutes(15));
            insertBid(conn, auc2, bidder3, "hoa@gmail.com", "Le Thi Hoa", 33_500_000, now.minusMinutes(8));
            insertBid(conn, auc2, bidder1, "demo@gmail.com", "Demo Bidder", 34_000_000, now.minusMinutes(2));

            insertBid(conn, auc5, bidder1, "demo@gmail.com", "Demo Bidder", 7_800_000, now.minusHours(4));
            insertBid(conn, auc5, bidder2, "binh@gmail.com", "Tran Van Binh", 8_200_000, now.minusHours(3).minusMinutes(30));
            insertBid(conn, auc5, bidder3, "hoa@gmail.com", "Le Thi Hoa", 8_700_000, now.minusHours(2).minusMinutes(45));
            insertBid(conn, auc5, bidder1, "demo@gmail.com", "Demo Bidder", 9_000_000, now.minusHours(2));
            insertBid(conn, auc5, bidder3, "hoa@gmail.com", "Le Thi Hoa", 9_200_000, now.minusHours(1).minusMinutes(10));

            insertBid(conn, auc7, bidder1, "demo@gmail.com", "Demo Bidder", 12_000_000, now.minusHours(1));
            insertBid(conn, auc7, bidder2, "binh@gmail.com", "Tran Van Binh", 12_500_000, now.minusMinutes(30));

            insertBid(conn, auc8, bidder3, "hoa@gmail.com", "Le Thi Hoa", 420_000_000, now.minusHours(4));
            insertBid(conn, auc8, bidder1, "demo@gmail.com", "Demo Bidder", 450_000_000, now.minusMinutes(15));

            insertBid(conn, auc9, bidder2, "binh@gmail.com", "Tran Van Binh", 22_000_000, now.minusMinutes(10));

            insertBid(conn, auc10, bidder6, "trinh@gmail.com", "Ngoc Trinh", 46_500_000, now.minusMinutes(12));
            insertBid(conn, auc10, bidder4, "anh@gmail.com", "Mai Minh Anh", 47_000_000, now.minusMinutes(8));
            insertBid(conn, auc10, bidder6, "trinh@gmail.com", "Ngoc Trinh", 48_000_000, now.minusMinutes(3));
            insertBid(conn, auc10, bidder4, "anh@gmail.com", "Mai Minh Anh", 49_000_000, now.minusSeconds(15));

            insertBid(conn, auc12, bidder2, "binh@gmail.com", "Tran Van Binh", 1_500_000, now.minusHours(13));
            insertBid(conn, auc12, bidder5, "khanh@gmail.com", "Vu Gia Khanh", 1_950_000, now.minusHours(10));

            insertBidSeries(conn, auc1, bidder1, "demo@gmail.com", "Demo Bidder", 28_600_000, 6, 220_000, now.minusMinutes(2));
            insertBidSeries(conn, auc2, bidder3, "hoa@gmail.com", "Le Thi Hoa", 35_100_000, 5, 180_000, now.minusMinutes(1));
            insertBidSeries(conn, auc7, bidder2, "binh@gmail.com", "Tran Van Binh", 13_700_000, 7, 170_000, now.minusMinutes(20));
            insertBidSeries(conn, auc9, bidder3, "hoa@gmail.com", "Le Thi Hoa", 23_200_000, 6, 200_000, now.minusMinutes(30));
            insertBidSeries(conn, auc10, bidder4, "anh@gmail.com", "Mai Minh Anh", 50_200_000, 10, 150_000, now.minusSeconds(40));

            updateAuctionLeader(conn, auc1, 29_700_000, bidder1, "demo@gmail.com");
            updateAuctionLeader(conn, auc2, 35_820_000, bidder3, "hoa@gmail.com");
            updateAuctionLeader(conn, auc7, 14_720_000, bidder2, "binh@gmail.com");
            updateAuctionLeader(conn, auc9, 24_200_000, bidder3, "hoa@gmail.com");
            updateAuctionLeader(conn, auc10, 51_550_000, bidder4, "anh@gmail.com");

            insertAutoBid(conn, auc1, bidder1, "demo@gmail.com", 30_000_000, 500_000);
            insertAutoBid(conn, auc2, bidder3, "hoa@gmail.com", 36_000_000, 500_000);
            insertAutoBid(conn, auc7, bidder4, "anh@gmail.com", 13_000_000, 200_000);
            insertAutoBid(conn, auc10, bidder6, "trinh@gmail.com", 49_500_000, 250_000);

            insertWalletTx(conn, bidder2, "RESERVE", 27_500_000, auc1, now.minusMinutes(3));
            insertWalletTx(conn, bidder1, "RELEASE", 27_000_000, auc1, now.minusMinutes(3));
            insertWalletTx(conn, bidder4, "RESERVE", 49_000_000, auc10, now.minusSeconds(15));
            insertWalletTx(conn, bidder6, "RELEASE", 48_000_000, auc10, now.minusSeconds(15));

            seedBulkDemoData(
                    conn,
                    now,
                    new long[] {seller1, seller2, seller3, seller4},
                    new String[] {"seller@gmail.com", "lan@gmail.com", "bao.seller@gmail.com", "chau.seller@gmail.com"},
                    new long[] {bidder1, bidder2, bidder3, bidder4, bidder5, bidder6},
                    new String[] {"demo@gmail.com", "binh@gmail.com", "hoa@gmail.com", "anh@gmail.com", "khanh@gmail.com", "trinh@gmail.com"});

            LOG.info("Fake seed data inserted successfully (rich dataset)");
            LOG.info("Demo users: demo@gmail.com / seller@gmail.com / admin@gmail.com");
            LOG.info("Extra users: binh@gmail.com, hoa@gmail.com, anh@gmail.com, khanh@gmail.com, trinh@gmail.com");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to seed fake data", e);
        }
    }

    private static boolean hasAuctionData(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM auctions")) {
            return rs.next() && rs.getLong(1) > 0;
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
        String[] categories = {"ELECTRONICS", "ART", "VEHICLE"};
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
                    imageUrl("auction-item-" + (100 + i)));

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
                    imageUrl("auction-cover-" + (100 + i)));

            if (!"OPEN".equals(status) && !"CANCELED".equals(status)) {
                insertBid(conn, auctionId, bidderIds[bidderAltIdx], bidderEmails[bidderAltIdx],
                        "Bulk Bidder Alt", startingBid + 500_000, end.minusHours(2));
                insertBid(conn, auctionId, bidderIds[bidderLeadIdx], bidderEmails[bidderLeadIdx],
                        "Bulk Bidder Lead", currentBid, end.minusMinutes(12));
                double seriesStart = currentBid + 250_000;
                int seriesCount = 6 + (i % 5);
                insertBidSeries(conn, auctionId, bidderIds[bidderLeadIdx], bidderEmails[bidderLeadIdx],
                        "Bulk Bidder Lead", seriesStart, seriesCount, 125_000, end.minusMinutes(10));
                updateAuctionLeader(conn, auctionId, seriesStart + ((seriesCount - 1) * 125_000),
                        bidderIds[bidderLeadIdx], bidderEmails[bidderLeadIdx]);
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

    private static void updateAuctionLeader(Connection conn, long auctionId, double amount,
            long bidderId, String bidderEmail) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE auctions SET current_bid = ?, highest_bidder_id = ?, winner_email = ? WHERE id = ?")) {
            ps.setDouble(1, amount);
            ps.setLong(2, bidderId);
            ps.setString(3, bidderEmail);
            ps.setLong(4, auctionId);
            ps.executeUpdate();
        }
    }

    private static String imageUrl(String seed) {
        return "https://picsum.photos/seed/" + seed + "/1200/800";
    }
}
