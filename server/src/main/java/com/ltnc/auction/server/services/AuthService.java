package com.ltnc.auction.server.services;

import com.ltnc.auction.server.dao.UserDAO;
import com.ltnc.auction.server.model.User;
import com.ltnc.auction.server.model.UserRole;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;

public class AuthService {
    private final UserDAO userDAO;

    public AuthService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public record LoginResult(boolean success, String code, User user) {
    }

    public record RegisterResult(boolean success, String code, User user) {
    }

    public LoginResult login(String email, String rawPassword) {
        Optional<User> userOpt = userDAO.findByEmail(email);
        if (userOpt.isEmpty()) {
            return new LoginResult(false, "USER_NOT_FOUND", null);
        }
        User user = userOpt.get();
        if (!hash(rawPassword).equals(user.getPasswordHash())) {
            return new LoginResult(false, "INVALID_PASSWORD", null);
        }
        return new LoginResult(true, "LOGIN_SUCCESS", user);
    }

    public RegisterResult register(String fullName, String email, String rawPassword, UserRole role) {
        if (userDAO.findByEmail(email).isPresent()) {
            return new RegisterResult(false, "EMAIL_EXISTS", null);
        }
        User user = new User(
                null,
                fullName,
                email,
                hash(rawPassword),
                role == null ? UserRole.BIDDER : role
        );
        Long id = userDAO.insert(user);
        user.setId(id);
        return new RegisterResult(true, "REGISTER_SUCCESS", user);
    }

    public boolean canCreateItem(User user) {
        return user != null && user.getRole() == UserRole.SELLER;
    }

    public boolean canViewAdminDashboard(User user) {
        return user != null && user.getRole() == UserRole.ADMIN;
    }

    private String hash(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (Exception e) {
            throw new RuntimeException("Cannot hash password", e);
        }
    }
}
