package com.ltnc.auction.server.dao;

import com.ltnc.auction.server.db.DBConnection;
import com.ltnc.auction.server.model.WalletTransaction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;

public class WalletTransactionDAO 
{

    public Long insert(WalletTransaction tx) 
    {
        String sql = "INSERT INTO wallet_transactions (user_id, type, amount, ref_auction_id, created_at) " +
                     "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, tx.getUserId());
            ps.setString(2, tx.getType());
            ps.setBigDecimal(3, tx.getAmount());

            if (tx.getRefAuctionId() != null) {
                ps.setLong(4, tx.getRefAuctionId());
            } else {
                ps.setNull(4, Types.BIGINT);
            }

            if (tx.getCreatedAt() != null) {
                ps.setTimestamp(5, Timestamp.valueOf(tx.getCreatedAt()));
            } else {
                ps.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
            }

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }

            return null;

        } catch (SQLException e) {
            throw new RuntimeException("Error inserting wallet transaction", e);
        }
        
    }

    private WalletTransaction mapWalletTransaction(ResultSet rs) throws SQLException 
    {
        WalletTransaction tx = new WalletTransaction();

        tx.setId(rs.getLong("id"));
        tx.setUserId(rs.getLong("user_id"));
        tx.setType(rs.getString("type"));
        tx.setAmount(rs.getBigDecimal("amount"));

        long refAuctionId = rs.getLong("ref_auction_id");
        if (!rs.wasNull()) {
            tx.setRefAuctionId(refAuctionId);
        }

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            tx.setCreatedAt(createdAt.toLocalDateTime());
        }

        return tx;
    }
}