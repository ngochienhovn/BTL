package com.ltnc.auction.server.dao;

import com.ltnc.auction.server.db.DBConnection;
<<<<<<< HEAD
import com.ltnc.auction.server.model.Admin;
import com.ltnc.auction.server.model.Bidder;
import com.ltnc.auction.server.model.Seller;
=======
>>>>>>> a4a9980ce3593461ea601ec5d280f231fec24645
import com.ltnc.auction.server.model.User;
import com.ltnc.auction.server.model.UserRole;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
<<<<<<< HEAD
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    public User findByEmail(String email) {
        String sql = "SELECT id, full_name, username, password_hash, role FROM users WHERE username = ? AND is_deleted = FALSE";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding user by email", e);
        }
        return null;
    }

    public User findById(Long id) {
        String sql = "SELECT id, full_name, username, password_hash, role FROM users WHERE id = ? AND is_deleted = FALSE";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding user by id", e);
        }
        return null;
=======
import java.util.Optional;

public class UserDAO {

    public Optional<User> findByEmail(String email) {
        String sql = "SELECT id, full_name, username, password_hash, role FROM users WHERE username = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapUser(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Cannot find user by email", e);
        }
>>>>>>> a4a9980ce3593461ea601ec5d280f231fec24645
    }

    public Long insert(User user) {
        String sql = "INSERT INTO users (username, full_name, password_hash, role) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
<<<<<<< HEAD
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getEmail());
            ps.setString(2, user.getFullName());
            ps.setString(3, user.getPasswordHash());
            ps.setString(4, user.getRole().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
=======
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, user.getEmail());
            stmt.setString(2, user.getFullName());
            stmt.setString(3, user.getPasswordHash());
            stmt.setString(4, user.getRole().name());
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
>>>>>>> a4a9980ce3593461ea601ec5d280f231fec24645
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
<<<<<<< HEAD
        } catch (SQLException e) {
            throw new RuntimeException("Error inserting user", e);
        }
        return null;
    }

    public boolean update(User user) {
        String sql = "UPDATE users SET full_name = ?, password_hash = ?, role = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getRole().name());
            ps.setLong(4, user.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error updating user", e);
        }
    }

    public boolean delete(Long id) {
        String sql = "UPDATE users SET is_deleted = TRUE WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting user", e);
        }
    }

    public List<User> findAll() {
        String sql = "SELECT id, full_name, username, password_hash, role FROM users WHERE is_deleted = FALSE ORDER BY id";
        List<User> users = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                users.add(mapUser(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding all users", e);
        }
        return users;
    }

    private User mapUser(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        String fullName = rs.getString("full_name");
        String email = rs.getString("username");
        String passwordHash = rs.getString("password_hash");
        String roleStr = rs.getString("role");
        UserRole role = UserRole.valueOf(roleStr);

        User user = switch (role) {
            case SELLER -> new Seller(fullName, email, passwordHash);
            case ADMIN -> new Admin(fullName, email, passwordHash);
            default -> new Bidder(fullName, email, passwordHash);
        };
        user.setId(id);
=======
            return null;
        } catch (SQLException e) {
            throw new RuntimeException("Cannot insert user", e);
        }
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setFullName(rs.getString("full_name"));
        user.setEmail(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setRole(UserRole.valueOf(rs.getString("role")));
>>>>>>> a4a9980ce3593461ea601ec5d280f231fec24645
        return user;
    }
}
