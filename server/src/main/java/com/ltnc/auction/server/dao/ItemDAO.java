package com.ltnc.auction.server.dao;

import com.ltnc.auction.server.db.DBConnection;
import com.ltnc.auction.server.model.Item;
<<<<<<< HEAD
import com.ltnc.auction.server.model.ItemFactory;
=======
>>>>>>> a4a9980ce3593461ea601ec5d280f231fec24645
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ItemDAO {

<<<<<<< HEAD
    public Long insert(Item item, Long sellerId) {
        String sql = "INSERT INTO items (seller_id, seller_email, type, name, description, starting_bid, image_url) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, sellerId != null ? sellerId : 0);
            ps.setString(2, item.getSellerEmail());
            ps.setString(3, item.getType());
            ps.setString(4, item.getName());
            ps.setString(5, item.getDescription());
            ps.setBigDecimal(6, item.getStartingBid());
            ps.setString(7, item.getImageUrl());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
=======
    public Long insert(Item item) {
        String sql = """
                INSERT INTO items (seller_id, seller_email, type, name, description, starting_bid, image_url)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, item.getSellerId());
            stmt.setString(2, item.getSellerEmail());
            stmt.setString(3, item.getType());
            stmt.setString(4, item.getName());
            stmt.setString(5, item.getDescription());
            stmt.setBigDecimal(6, item.getStartingBid());
            stmt.setString(7, item.getImageUrl());
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
>>>>>>> a4a9980ce3593461ea601ec5d280f231fec24645
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
<<<<<<< HEAD
        } catch (SQLException e) {
            throw new RuntimeException("Error inserting item", e);
        }
        return null;
    }

    public Item findById(Long id) {
        String sql = "SELECT id, seller_id, seller_email, type, name, description, starting_bid, image_url FROM items WHERE id = ? AND is_deleted = FALSE";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapItem(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding item by id", e);
        }
        return null;
    }

    public List<Item> findBySeller(String sellerEmail) {
        String sql = "SELECT id, seller_id, seller_email, type, name, description, starting_bid, image_url FROM items WHERE seller_email = ? AND is_deleted = FALSE";
        List<Item> items = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) 
            {
            ps.setString(1, sellerEmail);
            try (ResultSet rs = ps.executeQuery())
            {
                while (rs.next()) 
                 {
                    items.add(mapItem(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding items by seller", e);
        }
        return items;
    }

    public List<Item> findAll() {
        String sql = "SELECT id, seller_id, seller_email, type, name, description, starting_bid, image_url FROM items WHERE is_deleted = FALSE ORDER BY id";
        List<Item> items = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                items.add(mapItem(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding all items", e);
        }
        return items;
    }

    public boolean update(Item item) {
        String sql = "UPDATE items SET name = ?, description = ?, starting_bid = ?, image_url = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, item.getName());
            ps.setString(2, item.getDescription());
            ps.setBigDecimal(3, item.getStartingBid());
            ps.setString(4, item.getImageUrl());
            ps.setLong(5, item.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error updating item", e);
        }
    }

    public boolean delete(Long id) {
        String sql = "UPDATE items SET is_deleted = TRUE WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting item", e);
=======
            return null;
        } catch (SQLException e) {
            throw new RuntimeException("Cannot insert item", e);
        }
    }

    public boolean update(Item item) {
        String sql = """
                UPDATE items
                SET type = ?, name = ?, description = ?, starting_bid = ?, image_url = ?
                WHERE id = ? AND seller_id = ?
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, item.getType());
            stmt.setString(2, item.getName());
            stmt.setString(3, item.getDescription());
            stmt.setBigDecimal(4, item.getStartingBid());
            stmt.setString(5, item.getImageUrl());
            stmt.setLong(6, item.getId());
            stmt.setLong(7, item.getSellerId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Cannot update item", e);
        }
    }

    public boolean delete(long itemId, long sellerId) {
        String sql = "DELETE FROM items WHERE id = ? AND seller_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, itemId);
            stmt.setLong(2, sellerId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Cannot delete item", e);
        }
    }

    public List<Item> findBySeller(String sellerEmail) {
        String sql = """
                SELECT id, seller_id, seller_email, type, name, description, starting_bid, image_url
                FROM items
                WHERE seller_email = ?
                ORDER BY id DESC
                """;
        List<Item> items = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, sellerEmail);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    items.add(mapItem(rs));
                }
            }
            return items;
        } catch (SQLException e) {
            throw new RuntimeException("Cannot find items by seller", e);
>>>>>>> a4a9980ce3593461ea601ec5d280f231fec24645
        }
    }

    private Item mapItem(ResultSet rs) throws SQLException {
<<<<<<< HEAD
        long id = rs.getLong("id");
        long sellerId = rs.getLong("seller_id");
        String sellerEmail = rs.getString("seller_email");
        String type = rs.getString("type");
        String name = rs.getString("name");
        String description = rs.getString("description");
        BigDecimal startingBid = rs.getBigDecimal("starting_bid");
        String imageUrl = rs.getString("image_url");

        Item item = ItemFactory.create(type, name, description, startingBid, imageUrl, sellerEmail);
        item.setId(id);
        item.setSellerId(sellerId);
=======
        Item item = new Item();
        item.setId(rs.getLong("id"));
        item.setSellerId(rs.getLong("seller_id"));
        item.setSellerEmail(rs.getString("seller_email"));
        item.setType(rs.getString("type"));
        item.setName(rs.getString("name"));
        item.setDescription(rs.getString("description"));
        BigDecimal bid = rs.getBigDecimal("starting_bid");
        item.setStartingBid(bid == null ? BigDecimal.ZERO : bid);
        item.setImageUrl(rs.getString("image_url"));
>>>>>>> a4a9980ce3593461ea601ec5d280f231fec24645
        return item;
    }
}
