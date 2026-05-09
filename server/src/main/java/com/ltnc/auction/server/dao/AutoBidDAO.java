package com.ltnc.auction.server.dao;

import com.ltnc.auction.server.db.DBConnection;
import com.ltnc.auction.server.model.AutoBidConfig;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO xử lý dữ liệu bảng auto_bid_configs.
 *
 * DAO = Data Access Object.
 * Nhiệm vụ của class này là làm việc với database:
 * - Lưu cấu hình Auto-Bid
 * - Lấy danh sách Auto-Bid theo auction
 * - Tìm Auto-Bid của một user
 * - Xóa Auto-Bid
 */
public class AutoBidDAO {

    /**
     * Lưu cấu hình Auto-Bid.
     *
     * Nếu user chưa có Auto-Bid trong auction này:
     *      INSERT dòng mới.
     *
     * Nếu user đã có Auto-Bid trong auction này:
     *      UPDATE max_bid và increment.
     */
    public void save(AutoBidConfig config) {
        validateConfig(config);

        String sql =
                "INSERT INTO auto_bid_configs (auction_id, user_id, max_bid, increment) " +
                "VALUES (?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE max_bid = ?, increment = ?";

        try (
                Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setLong(1, config.getAuctionId());
            statement.setLong(2, config.getUserId());
            statement.setBigDecimal(3, config.getMaxBid());
            statement.setBigDecimal(4, config.getIncrement());

            // Hai tham số này dùng cho phần UPDATE sau ON DUPLICATE KEY
            statement.setBigDecimal(5, config.getMaxBid());
            statement.setBigDecimal(6, config.getIncrement());

            statement.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lưu cấu hình Auto-Bid", e);
        }
    }

    /**
     * Lấy toàn bộ cấu hình Auto-Bid của một phiên đấu giá.
     *
     * Hàm này sau này sẽ được Bot Auto-Bid dùng.
     * Ví dụ:
     * Khi auction 1 có giá mới, server gọi findByAuction(1L)
     * để xem những user nào đang bật auto-bid.
     */
    public List<AutoBidConfig> findByAuction(Long auctionId) {
        if (auctionId == null) {
            throw new IllegalArgumentException("auctionId không được null");
        }

        String sql =
                "SELECT id, auction_id, user_id, max_bid, increment " +
                "FROM auto_bid_configs " +
                "WHERE auction_id = ? " +
                "ORDER BY max_bid DESC, id ASC";

        List<AutoBidConfig> configs = new ArrayList<>();

        try (
                Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setLong(1, auctionId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    configs.add(mapRowToAutoBidConfig(resultSet));
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lấy danh sách Auto-Bid theo auctionId", e);
        }

        return configs;
    }

    /**
     * Tìm cấu hình Auto-Bid của một user trong một auction.
     *
     * Nếu không tìm thấy thì trả về null.
     */
    public AutoBidConfig findByAuctionAndUser(Long auctionId, Long userId) {
        if (auctionId == null) {
            throw new IllegalArgumentException("auctionId không được null");
        }

        if (userId == null) {
            throw new IllegalArgumentException("userId không được null");
        }

        String sql =
                "SELECT id, auction_id, user_id, max_bid, increment " +
                "FROM auto_bid_configs " +
                "WHERE auction_id = ? AND user_id = ?";

        try (
                Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setLong(1, auctionId);
            statement.setLong(2, userId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapRowToAutoBidConfig(resultSet);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tìm Auto-Bid theo auctionId và userId", e);
        }

        return null;
    }

    /**
     * Xóa cấu hình Auto-Bid của một user trong một auction.
     *
     * Trả về true nếu xóa thành công.
     * Trả về false nếu không có dòng nào để xóa.
     */
    public boolean delete(Long auctionId, Long userId) {
        if (auctionId == null) {
            throw new IllegalArgumentException("auctionId không được null");
        }

        if (userId == null) {
            throw new IllegalArgumentException("userId không được null");
        }

        String sql =
                "DELETE FROM auto_bid_configs " +
                "WHERE auction_id = ? AND user_id = ?";

        try (
                Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setLong(1, auctionId);
            statement.setLong(2, userId);

            int affectedRows = statement.executeUpdate();

            return affectedRows > 0;

        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi xóa Auto-Bid", e);
        }
    }

    /**
     * Chuyển một dòng dữ liệu trong ResultSet thành object AutoBidConfig.
     */
    private AutoBidConfig mapRowToAutoBidConfig(ResultSet resultSet) throws SQLException {
        Long id = resultSet.getLong("id");
        Long auctionId = resultSet.getLong("auction_id");
        Long userId = resultSet.getLong("user_id");
        BigDecimal maxBid = resultSet.getBigDecimal("max_bid");
        BigDecimal increment = resultSet.getBigDecimal("increment");

        return new AutoBidConfig(id, auctionId, userId, maxBid, increment);
    }

    /**
     * Kiểm tra dữ liệu trước khi lưu vào database.
     */
    private void validateConfig(AutoBidConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("AutoBidConfig không được null");
        }

        if (config.getAuctionId() == null) {
            throw new IllegalArgumentException("auctionId không được null");
        }

        if (config.getUserId() == null) {
            throw new IllegalArgumentException("userId không được null");
        }

        requirePositive(config.getMaxBid(), "maxBid");
        requirePositive(config.getIncrement(), "increment");
    }

    private void requirePositive(BigDecimal value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " không được null");
        }

        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(fieldName + " phải lớn hơn 0");
        }
    }

    /**
     * Tách riêng hàm lấy connection để nếu DBConnection của nhóm bạn
     * đặt tên hàm khác thì chỉ cần sửa ở đây.
     */
    private Connection getConnection() throws Exception {
        return DBConnection.getConnection();
    }
}