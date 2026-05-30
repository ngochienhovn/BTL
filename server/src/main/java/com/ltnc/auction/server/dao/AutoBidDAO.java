package com.ltnc.auction.server.dao;

import com.ltnc.auction.server.db.DBConnection;
import com.ltnc.auction.server.model.AutoBidConfig;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class AutoBidDAO {

    public void upsert(AutoBidConfig config) {
        // H2 supports MERGE INTO for upsert
        String sql = "MERGE INTO auto_bid_configs (auction_id, bidder_id, bidder_email, max_bid, increment_amount, registered_at) " +
                     "KEY(auction_id, bidder_id) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, config.getAuctionId());
            ps.setLong(2, config.getBidderId());
            ps.setString(3, config.getBidderEmail());
            ps.setBigDecimal(4, config.getMaxBid());
            ps.setBigDecimal(5, config.getIncrement());
            ps.setTimestamp(6, config.getRegisteredAt() != null
                    ? Timestamp.valueOf(config.getRegisteredAt())
                    : Timestamp.valueOf(java.time.LocalDateTime.now()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error upserting auto bid config", e);
        }
    }

    public List<AutoBidConfig> findByAuction(Long auctionId) {
        String sql = "SELECT * FROM auto_bid_configs WHERE auction_id = ?";
        List<AutoBidConfig> configs = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AutoBidConfig config = new AutoBidConfig(
                            rs.getLong("auction_id"),
                            rs.getLong("bidder_id"),
                            rs.getString("bidder_email"),
                            rs.getBigDecimal("max_bid"),
                            rs.getBigDecimal("increment_amount")
                    );
                    config.setId(rs.getLong("id"));
                    Timestamp ts = rs.getTimestamp("registered_at");
                    if (ts != null) config.setRegisteredAt(ts.toLocalDateTime());
                    configs.add(config);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding auto bid configs by auction", e);
        }
        return configs;
    }

    public void deleteByAuction(Long auctionId) {
        String sql = "DELETE FROM auto_bid_configs WHERE auction_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, auctionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting auto bid configs", e);
        }
    }
}
