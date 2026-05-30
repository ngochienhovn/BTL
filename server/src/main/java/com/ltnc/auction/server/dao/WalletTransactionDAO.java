package com.ltnc.auction.server.dao;

import com.ltnc.auction.server.db.DBConnection;
import com.ltnc.auction.server.model.WalletTransaction;
import com.ltnc.auction.server.model.WalletTxType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class WalletTransactionDAO {

    public Long insert(WalletTransaction tx) {
        String sql = "INSERT INTO wallet_transactions (user_id, type, amount, ref_auction_id, created_at) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, tx.getUserId());
            ps.setString(2, tx.getType().name());
            ps.setBigDecimal(3, tx.getAmount());
            ps.setObject(4, tx.getRefAuctionId());
            ps.setTimestamp(5, Timestamp.valueOf(tx.getCreatedAt() != null ? tx.getCreatedAt() : java.time.LocalDateTime.now()));
            ps.executeUpdate();
            try (var keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error inserting wallet transaction", e);
        }
        return null;
    }

    public static WalletTxType parseType(String s) {
        if (s == null) {
            return null;
        }
        return WalletTxType.valueOf(s);
    }

    public List<WalletTransaction> findByUserId(Long userId) {
        List<WalletTransaction> list = new ArrayList<>();
        String sql = "SELECT id, user_id, type, amount, ref_auction_id, created_at FROM wallet_transactions WHERE user_id = ? ORDER BY created_at DESC";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Long refAuctionId = rs.getObject("ref_auction_id") != null ? rs.getLong("ref_auction_id") : null;
                    WalletTransaction tx = new WalletTransaction(
                            rs.getLong("user_id"),
                            WalletTxType.valueOf(rs.getString("type")),
                            rs.getBigDecimal("amount"),
                            refAuctionId);
                    tx.setId(rs.getLong("id"));
                    tx.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    list.add(tx);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching wallet transactions", e);
        }
        return list;
    }
}
