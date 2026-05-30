package com.ltnc.auction.server.dao;

import com.ltnc.auction.server.db.DBConnection;
import com.ltnc.auction.server.model.Wallet;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class WalletDAO {

    public Wallet findByUserId(Long userId) 
    {
        String sql = "SELECT * FROM wallets WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding wallet", e);
        }
        return null;
    }

    public Wallet findOrCreateByUserId(Long userId) {
        synchronized (String.valueOf(userId).intern()) {
            Wallet existing = findByUserId(userId);
            if (existing != null) {
                return existing;
            }
            String sql = "INSERT INTO wallets (user_id, balance, reserved, updated_at) VALUES (?, 0, 0, CURRENT_TIMESTAMP)";
            try (Connection conn = DBConnection.getConnection();
                    PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, userId);
                ps.executeUpdate();
            } catch (SQLException e) {
                // Ignore unique constraint violation if another thread inserted it
                if (!e.getMessage().contains("Unique index or primary key violation")) {
                    throw new RuntimeException("Error creating wallet", e);
                }
            }
            return findByUserId(userId);
        }
    }

    public void updateBalances(Long userId, BigDecimal balance, BigDecimal reserved) 
    {
        String sql = "UPDATE wallets SET balance = ?, reserved = ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) 
                {
            ps.setBigDecimal(1, balance);
            ps.setBigDecimal(2, reserved);
            ps.setLong(3, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating wallet balances", e);
        }
    }

    private Wallet map(ResultSet rs) throws SQLException {
        Wallet w = new Wallet();
        w.setUserId(rs.getLong("user_id"));
        w.setBalance(rs.getBigDecimal("balance"));
        w.setReserved(rs.getBigDecimal("reserved"));
        Timestamp ts = rs.getTimestamp("updated_at");
        if (ts != null) {
            w.setUpdatedAt(ts.toLocalDateTime());
        }
        return w;
    }
    
}
