package com.ltnc.auction.server.dao;

import com.ltnc.auction.server.db.DBConnection;
import com.ltnc.auction.server.model.Item;
import com.ltnc.auction.server.model.ItemFactory;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ItemDAO {

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
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
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
        }
    }

    private Item mapItem(ResultSet rs) throws SQLException {
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
        return item;
    }
}
