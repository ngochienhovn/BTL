package com.ltnc.auction.server.dao;

import com.ltnc.auction.server.db.DBConnection;
import com.ltnc.auction.server.model.Auction;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
<<<<<<< HEAD
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
=======
import java.sql.Timestamp;
import java.sql.Types;
>>>>>>> a4a9980ce3593461ea601ec5d280f231fec24645
import java.util.ArrayList;
import java.util.List;

public class AuctionDAO {

<<<<<<< HEAD
    public Long insert(Auction auction) {
        String sql = "INSERT INTO auctions (item_id, title, description, category, starting_bid, current_bid, " +
                     "start_time, end_time, status, highest_bidder_id, winner_email, seller_email, image_url) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setObject(1, auction.getItemId());
            ps.setString(2, auction.getTitle());
            ps.setString(3, auction.getDescription());
            ps.setString(4, auction.getCategory());
            ps.setBigDecimal(5, auction.getStartingBid());
            ps.setBigDecimal(6, auction.getCurrentBid());
            ps.setTimestamp(7, auction.getStartTime() != null ? Timestamp.valueOf(auction.getStartTime()) : null);
            ps.setTimestamp(8, auction.getEndTime() != null ? Timestamp.valueOf(auction.getEndTime()) : null);
            ps.setString(9, auction.getStatus());
            ps.setObject(10, auction.getHighestBidderId());
            ps.setString(11, auction.getWinnerEmail());
            ps.setString(12, auction.getSellerEmail());
            ps.setString(13, auction.getImageUrl());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        } catch (SQLException e) {
            throw new DaoException("Error inserting auction into database", e);
        }
        return null;
    }

    public Auction findById(Long id) {
        String sql = "SELECT * FROM auctions WHERE id = ? AND is_deleted = FALSE";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapAuction(rs);
                }
            }
        } catch (SQLException e) {
            throw new DaoException("Error finding auction with id: " + id, e);
        }
        return null;
    }

    public List<Auction> findAll() {
        String sql = "SELECT * FROM auctions WHERE is_deleted = FALSE ORDER BY id";
=======
    public List<Auction> findAll() {
        String sql = "SELECT * FROM auctions ORDER BY id DESC";
>>>>>>> a4a9980ce3593461ea601ec5d280f231fec24645
        List<Auction> auctions = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                auctions.add(mapAuction(rs));
            }
<<<<<<< HEAD
        } catch (SQLException e) {
            throw new DaoException("Error fetching all auctions", e);
        }
        return auctions;
    }

    public List<Auction> findAll(int offset, int limit) {
        String sql = "SELECT * FROM auctions WHERE is_deleted = FALSE ORDER BY id LIMIT ? OFFSET ?";
        List<Auction> auctions = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ps.setInt(2, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    auctions.add(mapAuction(rs));
                }
            }
        } catch (SQLException e) {
            throw new DaoException("Error fetching paginated auctions", e);
        }
        return auctions;
    }


    public List<Auction> findByStatus(String status) {
        String sql = "SELECT * FROM auctions WHERE status = ? AND is_deleted = FALSE ORDER BY id";
        List<Auction> auctions = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    auctions.add(mapAuction(rs));
                }
            }
        } catch (SQLException e) {
            throw new DaoException("Error finding auctions with status: " + status, e);
        }
        return auctions;
=======
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
>>>>>>> a4a9980ce3593461ea601ec5d280f231fec24645
    }

    public boolean updateCurrentBid(Long auctionId, BigDecimal newBid, Long highestBidderId) {
        String sql = "UPDATE auctions SET current_bid = ?, highest_bidder_id = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, newBid);
<<<<<<< HEAD
            ps.setObject(2, highestBidderId);
            ps.setLong(3, auctionId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DaoException("Error updating bid for auction: " + auctionId, e);
        }
    }

    public boolean updateStatus(Long auctionId, String status, String winnerEmail) {
        String sql = "UPDATE auctions SET status = ?, winner_email = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, winnerEmail);
            ps.setLong(3, auctionId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DaoException("Error updating status for auction: " + auctionId, e);
        }
    }

    public boolean update(Auction auction) {
        String sql = "UPDATE auctions SET title = ?, description = ?, starting_bid = ?, current_bid = ?, end_time = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, auction.getTitle());
            ps.setString(2, auction.getDescription());
            ps.setBigDecimal(3, auction.getStartingBid());
            ps.setBigDecimal(4, auction.getCurrentBid());
            ps.setTimestamp(5, auction.getEndTime() != null ? Timestamp.valueOf(auction.getEndTime()) : null);
            ps.setLong(6, auction.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DaoException("Error updating auction in database", e);
        }
    }

    public boolean updateEndTime(Long auctionId, LocalDateTime newEndTime) {
        String sql = "UPDATE auctions SET end_time = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(newEndTime));
            ps.setLong(2, auctionId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DaoException("Error updating end time for auction: " + auctionId, e);
        }
    }

    public void updateByItemId(Long itemId, String title, String description, String category, BigDecimal startingBid, String imageUrl) {
        String sqlBasic = "UPDATE auctions SET title = ?, description = ?, category = ?, image_url = ? WHERE item_id = ? AND status IN ('OPEN', 'RUNNING')";
        String sqlPrice = "UPDATE auctions SET starting_bid = ?, current_bid = ? WHERE item_id = ? AND status IN ('OPEN', 'RUNNING') AND highest_bidder_id IS NULL";
        try (Connection conn = DBConnection.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(sqlBasic)) {
                ps.setString(1, title);
                ps.setString(2, description);
                ps.setString(3, category);
                ps.setString(4, imageUrl);
                ps.setLong(5, itemId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(sqlPrice)) {
                ps.setBigDecimal(1, startingBid);
                ps.setBigDecimal(2, startingBid);
                ps.setLong(3, itemId);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new DaoException("Error updating auctions by item_id: " + itemId, e);
        }
    }

    public boolean delete(Long id) {
        String sql = "UPDATE auctions SET is_deleted = TRUE WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DaoException("Error deleting auction with id: " + id, e);
=======
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

    public boolean updateStatus(Long auctionId, String status)
    {
        String sql = "UPDATE auctions SET status = ? WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) 
        {
            ps.setString(1, status);
            ps.setLong(2, auctionId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Cannot update status", e);
>>>>>>> a4a9980ce3593461ea601ec5d280f231fec24645
        }
    }

    private Auction mapAuction(ResultSet rs) throws SQLException {
        Auction auction = new Auction();
        auction.setId(rs.getLong("id"));
        auction.setItemId(rs.getLong("item_id"));
        auction.setTitle(rs.getString("title"));
        auction.setDescription(rs.getString("description"));
<<<<<<< HEAD
        auction.setCategory(rs.getString("category"));
        auction.setStartingBid(rs.getBigDecimal("starting_bid"));
        auction.setCurrentBid(rs.getBigDecimal("current_bid"));
        Timestamp startTs = rs.getTimestamp("start_time");
        if (startTs != null) auction.setStartTime(startTs.toLocalDateTime());
        Timestamp endTs = rs.getTimestamp("end_time");
        if (endTs != null) auction.setEndTime(endTs.toLocalDateTime());
        auction.setStatus(rs.getString("status"));
        long highestBidderId = rs.getLong("highest_bidder_id");
        if (!rs.wasNull()) auction.setHighestBidderId(highestBidderId);
        auction.setWinnerEmail(rs.getString("winner_email"));
        auction.setSellerEmail(rs.getString("seller_email"));
        auction.setImageUrl(rs.getString("image_url"));
=======
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
>>>>>>> a4a9980ce3593461ea601ec5d280f231fec24645
        return auction;
    }
}