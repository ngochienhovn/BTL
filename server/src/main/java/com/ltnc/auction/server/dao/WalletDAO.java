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

        } catch (SQLException e) {
            throw new RuntimeException("Error updating wallet balances", e);
        }
    }

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