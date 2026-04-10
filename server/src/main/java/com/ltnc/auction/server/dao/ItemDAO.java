package com.ltnc.auction.server.dao;

import com.ltnc.auction.server.db.DBConnection;
import com.ltnc.auction.server.model.Item;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ItemDAO {

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
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
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
        }
    }

    private Item mapItem(ResultSet rs) throws SQLException {
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
        return item;
    }
}
