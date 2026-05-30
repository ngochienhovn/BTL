package com.ltnc.auction.server.dao;

import com.ltnc.auction.server.db.DBConnection;
import com.ltnc.auction.server.model.Notification;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class NotificationDAO {

    public Long insert(Notification n) {
        String sql = "INSERT INTO notifications (user_id, title, message, type, is_read, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, n.getUserId());
            stmt.setString(2, n.getTitle());
            stmt.setString(3, n.getMessage());
            stmt.setString(4, n.getType());
            stmt.setBoolean(5, n.isRead());
            stmt.setTimestamp(6, Timestamp.valueOf(n.getCreatedAt()));

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                return null;
            }
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Notification> findByUserId(Long userId) {
        List<Notification> list = new ArrayList<>();
        String sql = "SELECT * FROM notifications WHERE user_id = ? ORDER BY created_at DESC LIMIT 100";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean markAsRead(Long id, Long userId) {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE id = ? AND user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.setLong(2, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean markAsUnread(Long id, Long userId) {
        String sql = "UPDATE notifications SET is_read = FALSE WHERE id = ? AND user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.setLong(2, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean markAllAsRead(Long userId) {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE user_id = ? AND is_read = FALSE";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private Notification mapRow(ResultSet rs) throws SQLException {
        Notification n = new Notification();
        n.setId(rs.getLong("id"));
        n.setUserId(rs.getLong("user_id"));
        n.setTitle(rs.getString("title"));
        n.setMessage(rs.getString("message"));
        n.setType(rs.getString("type"));
        n.setRead(rs.getBoolean("is_read"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            n.setCreatedAt(ts.toLocalDateTime());
        } else {
            n.setCreatedAt(LocalDateTime.now());
        }
        return n;
    }
}
