package com.ltnc.auction.server.dao;

import com.ltnc.auction.server.db.DBConnection;
import com.ltnc.auction.server.model.Auction;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class AuctionDAO {

    public List<Auction> findAll() {
        String sql = "SELECT * FROM auctions ORDER BY id DESC";
        List<Auction> auctions = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                auctions.add(mapAuction(rs));
            }
            return auctions;
        } catch (SQLException e) {
            throw new RuntimeException("Cannot query auctions", e);
        }
    }

    public Auction findById(Long id) {
        String sql = "SELECT * FROM auctions WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapAuction(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Cannot find auction by id", e);
        }
    }

    public boolean updateCurrentBid(Long auctionId, BigDecimal newBid, Long highestBidderId) {
        String sql = "UPDATE auctions SET current_bid = ?, highest_bidder_id = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, newBid);
            if (highestBidderId == null) {
                ps.setNull(2, Types.BIGINT);
            } else {
                ps.setLong(2, highestBidderId);
            }
            ps.setLong(3, auctionId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Cannot update current bid", e);
        }
    }

    private Auction mapAuction(ResultSet rs) throws SQLException {
        Auction auction = new Auction();
        auction.setId(rs.getLong("id"));
        auction.setItemId(rs.getLong("item_id"));
        auction.setTitle(rs.getString("title"));
        auction.setDescription(rs.getString("description"));
        auction.setStartingBid(rs.getBigDecimal("starting_bid"));
        auction.setCurrentBid(rs.getBigDecimal("current_bid"));
        auction.setStatus(rs.getString("status"));

        long highestBidderId = rs.getLong("highest_bidder_id");
        if (!rs.wasNull()) {
            auction.setHighestBidderId(highestBidderId);
        }

        Timestamp startTime = rs.getTimestamp("start_time");
        if (startTime != null) {
            auction.setStartTime(startTime.toLocalDateTime());
        }
        Timestamp endTime = rs.getTimestamp("end_time");
        if (endTime != null) {
            auction.setEndTime(endTime.toLocalDateTime());
        }
        return auction;
    }
}