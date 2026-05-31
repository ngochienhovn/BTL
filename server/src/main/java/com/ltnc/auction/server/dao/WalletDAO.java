package com.ltnc.auction.server.dao;

import com.ltnc.auction.server.db.DBConnection;
import com.ltnc.auction.server.model.Wallet;
<<<<<<< HEAD
=======

>>>>>>> a4a9980ce3593461ea601ec5d280f231fec24645
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class WalletDAO {

<<<<<<< HEAD
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
=======
    public Wallet findOrCreateByUserId(Long userId) 
    {
        String selectSql = "SELECT * FROM wallets WHERE user_id = ?";
        String insertSql = "INSERT INTO wallets (user_id, balance, reserved, updated_at) VALUES (?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection()) {

            // Tìm ví theo user_id
            try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                ps.setLong(1, userId);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return mapWallet(rs);
                    }
                }
            }

            // Nếu chưa có ví thì tạo ví mới
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                ps.setLong(1, userId);
                ps.setBigDecimal(2, BigDecimal.ZERO);
                ps.setBigDecimal(3, BigDecimal.ZERO);
                ps.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
                ps.executeUpdate();
            }

            // Trả về ví mới tạo
            Wallet wallet = new Wallet();
            wallet.setUserId(userId);
            wallet.setBalance(BigDecimal.ZERO);
            wallet.setReserved(BigDecimal.ZERO);
            wallet.setUpdatedAt(java.time.LocalDateTime.now());

            return wallet;

        } catch (SQLException e) {
            throw new RuntimeException("Error finding or creating wallet", e);
        }
    }

    public boolean updateBalances(Long userId, BigDecimal balance, BigDecimal reserved) 
    {
        String sql = "UPDATE wallets SET balance = ?, reserved = ?, updated_at = ? WHERE user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) 
        {

            ps.setBigDecimal(1, balance);
            ps.setBigDecimal(2, reserved);
            ps.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
            ps.setLong(4, userId);

            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;

>>>>>>> a4a9980ce3593461ea601ec5d280f231fec24645
        } catch (SQLException e) {
            throw new RuntimeException("Error updating wallet balances", e);
        }
    }

<<<<<<< HEAD
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
=======
    private Wallet mapWallet(ResultSet rs) throws SQLException 
    {
        Wallet wallet = new Wallet();

        wallet.setUserId(rs.getLong("user_id"));
        wallet.setBalance(rs.getBigDecimal("balance"));
        wallet.setReserved(rs.getBigDecimal("reserved"));

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            wallet.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        return wallet;
    }
}
>>>>>>> a4a9980ce3593461ea601ec5d280f231fec24645
