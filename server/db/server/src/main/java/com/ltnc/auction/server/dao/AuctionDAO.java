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

public class AuctionDao {

    public List<Auction> findAll() {
        List<Auction> auctions = new ArrayList<>();
        String sql = "SELECT * FROM auctions ORDER BY id";

        try (
                Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()
        ) {
            while (rs.next()) {
                auctions.add(mapAuction(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding all auctions", e);
        }

        return auctions;
    }

    public Auction findById(Long id) {
        String sql = "SELECT * FROM auctions WHERE id = ?";

        try (
                Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapAuction(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding auction by id", e);
        }

        return null;
    }

    public boolean updateCurrentBid(Long auctionId, BigDecimal newBid, Long highestBidderId) {
        String sql = "UPDATE auctions SET current_bid = ?, highest_bidder_id = ? WHERE id = ?";

        try (
                Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setBigDecimal(1, newBid);

            if (highestBidderId != null) {
                ps.setLong(2, highestBidderId);
            } else {
                ps.setNull(2, Types.BIGINT);
            }

            ps.setLong(3, auctionId);

            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Error updating current bid", e);
        }
    }

    private Auction mapAuction(ResultSet rs) throws SQLException {
        Auction auction = new Auction();

        auction.setId(rs.getLong("id"));

        long itemId = rs.getLong("item_id");
        if (!rs.wasNull()) {
            auction.setItemId(itemId);
        }

        auction.setTitle(rs.getString("title"));
        auction.setDescription(rs.getString("description"));
        auction.setCategory(rs.getString("category"));
        auction.setStartingBid(rs.getBigDecimal("starting_bid"));
        auction.setCurrentBid(rs.getBigDecimal("current_bid"));

        Timestamp startTime = rs.getTimestamp("start_time");
        if (startTime != null) {
            auction.setStartTime(startTime.toLocalDateTime());
        }

        Timestamp endTime = rs.getTimestamp("end_time");
        if (endTime != null) {
            auction.setEndTime(endTime.toLocalDateTime());
        }

        auction.setStatus(rs.getString("status"));

        long highestBidderId = rs.getLong("highest_bidder_id");
        if (!rs.wasNull()) {
            auction.setHighestBidderId(highestBidderId);
        }

        auction.setWinnerEmail(rs.getString("winner_email"));
        auction.setSellerEmail(rs.getString("seller_email"));
        auction.setImageUrl(rs.getString("image_url"));

        return auction;
    }
}