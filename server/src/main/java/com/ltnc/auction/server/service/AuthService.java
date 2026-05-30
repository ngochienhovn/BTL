package com.ltnc.auction.server.service;

import com.ltnc.auction.server.dao.UserDAO;
import com.ltnc.auction.server.model.Admin;
import com.ltnc.auction.server.model.Bidder;
import com.ltnc.auction.server.model.Seller;
import com.ltnc.auction.server.model.User;
import com.ltnc.auction.server.model.UserRole;
import com.ltnc.auction.shared.protocol.MessageType;
import com.ltnc.auction.shared.protocol.ServerToClientMessage;
import java.util.List;

public class AuthService {
    private static volatile AuthService instance;
    private final UserDAO userDAO = new UserDAO();

    private AuthService() {}

    public static AuthService getInstance() {
        if (instance == null) {
            synchronized (AuthService.class) {
                if (instance == null) {
                    instance = new AuthService();
                }
            }
        }
        return instance;
    }

    public record LoginResult(boolean success, String code, User user) {}
    public record RegisterResult(boolean success, String code, User user) {}

    public LoginResult login(String email, String password) {
        if (email == null || password == null) {
            return new LoginResult(false, "MISSING_FIELDS", null);
        }
        User user = userDAO.findByEmail(email.trim().toLowerCase());
        if (user == null) {
            return new LoginResult(false, "INVALID_CREDENTIALS", null);
        }
        if (!PasswordUtil.verify(password, user.getPasswordHash())) {
            return new LoginResult(false, "INVALID_CREDENTIALS", null);
        }
        return new LoginResult(true, "OK", user);
    }

    public RegisterResult register(String fullName, String email, String password, String role) {
        if (fullName == null || email == null || password == null || role == null) {
            return new RegisterResult(false, "MISSING_FIELDS", null);
        }
        String normalizedEmail = email.trim().toLowerCase();
        User existing = userDAO.findByEmail(normalizedEmail);
        if (existing != null) {
            return new RegisterResult(false, "EMAIL_EXISTS", null);
        }

        UserRole userRole;
        try {
            userRole = UserRole.valueOf(role.toUpperCase());
            // Security: Public registration cannot create ADMINs
            if (userRole == UserRole.ADMIN) {
                return new RegisterResult(false, "FORBIDDEN_ROLE", null);
            }
        } catch (IllegalArgumentException e) {
            userRole = UserRole.BIDDER;
        }

        String passwordHash = PasswordUtil.hash(password);
        User user = switch (userRole) {
            case SELLER -> new Seller(fullName, normalizedEmail, passwordHash);
            case ADMIN -> new Admin(fullName, normalizedEmail, passwordHash);
            default -> new Bidder(fullName, normalizedEmail, passwordHash);
        };

        Long id = userDAO.insert(user);
        user.setId(id);
        
        broadcastUserCreated(user);
        return new RegisterResult(true, "OK", user);
    }

    private void broadcastUserCreated(User user) {
        ServerToClientMessage msg = new ServerToClientMessage();
        msg.type = MessageType.USER_CREATED;
        msg.user = AuctionMapper.toUserDto(user);
        msg.success = true;
        BroadcastManager.getInstance().broadcastAll(msg);
    }

    public User findByEmail(String email) {
        if (email == null) return null;
        return userDAO.findByEmail(email.trim().toLowerCase());
    }

    public List<User> getAllUsers() {
        return userDAO.findAll();
    }

    public boolean updateUser(Long userId, String fullName, String newPassword, String newRole) {
        User user = userDAO.findById(userId);
        if (user == null) return false;

        if (fullName != null && !fullName.isBlank()) {
            user.setFullName(fullName.trim());
        }
        if (newPassword != null && !newPassword.isBlank()) {
            user.setPasswordHash(PasswordUtil.hash(newPassword));
        }
        if (newRole != null && !newRole.isBlank()) {
            try {
                user.setRole(UserRole.valueOf(newRole.toUpperCase()));
            } catch (IllegalArgumentException ignored) {}
        }
        return userDAO.update(user);
    }

    public String deleteUser(Long userId) {
        if (userId == null) return "INVALID_USER_ID";
        User user = userDAO.findById(userId);
        if (user == null) return "USER_NOT_FOUND";

        // Check if has active auctions as seller
        boolean hasSellerAuctions = AuctionService.getInstance().hasActiveAuctionsAsSeller(user.getEmail());
        if (hasSellerAuctions) {
            return "ACTIVE_SELLER_AUCTIONS";
        }

        // Check if is highest bidder on active auctions
        List<com.ltnc.auction.server.model.Auction> leadAuctions = AuctionService.getInstance().getAllActiveAuctionsForUser(userId);
        if (!leadAuctions.isEmpty()) {
            return "ACTIVE_BIDDER_ENGAGEMENTS";
        }

        // Financial Safety: Release all reserves for this user (fallback / safety)
        WalletService ws = WalletService.getInstance();
        com.ltnc.auction.server.model.Wallet w = ws.getWallet(userId);
        if (w != null && w.getReserved().compareTo(java.math.BigDecimal.ZERO) > 0) {
            for (com.ltnc.auction.server.model.Auction a : leadAuctions) {
                ws.releaseReserve(userId, a.getId(), a.getCurrentBid());
            }
        }

        boolean ok = userDAO.delete(userId);
        return ok ? "OK" : "DELETE_FAILED";
    }
}
