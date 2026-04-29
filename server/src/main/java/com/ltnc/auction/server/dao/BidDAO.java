package com.ltnc.auction.server.dao;

import com.ltnc.auction.server.db.DBConnection;
import com.ltnc.auction.server.model.BidTransaction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class BidDAO {

    public Long insert(BidTransaction bid) {
        String sql = "INSERT INTO bid_transactions (auction_id, bidder_id, bidder_email, bidder_name, amount, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, bid.getAuctionId());
            ps.setLong(2, bid.getBidderId());
            ps.setString(3, bid.getBidderEmail());
            ps.setString(4, bid.getBidderName());
            ps.setBigDecimal(5, bid.getAmount());

            if (bid.getCreatedAt() != null) {
                ps.setTimestamp(6, Timestamp.valueOf(bid.getCreatedAt()));
            } else {
                ps.setTimestamp(6, new Timestamp(System.currentTimeMillis()));
            }

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error inserting bid transaction", e);
        }

        return null;
    }

    public List<BidTransaction> findByAuction(Long auctionId) {
        String sql = "SELECT * FROM bid_transactions WHERE auction_id = ? ORDER BY created_at ASC";
        List<BidTransaction> bids = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, auctionId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    bids.add(mapBid(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error finding bids by auction", e);
        }

        return bids;
    }

    private BidTransaction mapBid(ResultSet rs) throws SQLException {
        BidTransaction bid = new BidTransaction(
                rs.getLong("auction_id"),
                rs.getLong("bidder_id"),
                rs.getString("bidder_email"),
                rs.getString("bidder_name"),
                rs.getBigDecimal("amount")
        );

        bid.setId(rs.getLong("id"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            bid.setCreatedAt(createdAt.toLocalDateTime());
        }

        return bid;
    }
}