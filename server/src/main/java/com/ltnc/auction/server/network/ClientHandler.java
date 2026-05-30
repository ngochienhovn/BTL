package com.ltnc.auction.server.network;

import com.google.gson.Gson;
import com.ltnc.auction.server.model.User;
import com.ltnc.auction.server.model.Wallet;
import com.ltnc.auction.server.service.AuctionService;
import com.ltnc.auction.server.service.AuctionService.BidResult;
import com.ltnc.auction.server.service.AuctionService.BidResultCode;
import com.ltnc.auction.server.service.AuthService;
import com.ltnc.auction.server.service.AuctionMapper;
import com.ltnc.auction.server.service.BroadcastManager;
import com.ltnc.auction.server.service.ItemService;
import com.ltnc.auction.server.service.WalletService;
import com.ltnc.auction.shared.dto.AuctionDto;
import com.ltnc.auction.shared.dto.ItemDto;
import com.ltnc.auction.shared.dto.WalletTransactionDto;
import com.ltnc.auction.server.model.WalletTransaction;
import com.ltnc.auction.shared.protocol.ClientToServerMessage;
import com.ltnc.auction.shared.protocol.MessageType;
import com.ltnc.auction.shared.protocol.ServerToClientMessage;
import java.math.BigDecimal;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REFACTORED CLIENT HANDLER
 * Acts as a Controller in the Server-side MVC architecture.
 */
public class ClientHandler implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(ClientHandler.class);

    private final Socket socket;
    private final AuthService authService;
    private final AuctionService auctionService;
    private final ItemService itemService;
    private final BroadcastManager broadcastManager;
    private final Gson gson = new Gson();
    private final RateLimiter rateLimiter = new RateLimiter(150); // 150 requests per second

    private String authenticatedEmail = null;

    public ClientHandler(Socket socket, AuthService authService, AuctionService auctionService,
                         ItemService itemService, BroadcastManager broadcastManager) {
        this.socket = socket;
        this.authService = authService;
        this.auctionService = auctionService;
        this.itemService = itemService;
        this.broadcastManager = broadcastManager;
    }

    @Override
    public void run() {
        String remote = socket.getRemoteSocketAddress() != null ? socket.getRemoteSocketAddress().toString() : "unknown";
        try (Socket s = socket;
             BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8), true)) {

            ClientSession session = new ClientSession(s, writer);
            broadcastManager.register(session);
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                session.lastActiveTime = System.currentTimeMillis();
                processLine(line, session);
            }
            broadcastManager.unregister(session);
        } catch (Throwable e) {
            LOG.error("Exception or Error in ClientHandler run for client: {}", remote, e);
        }
    }

    private void processLine(String line, ClientSession session) {
        if (!rateLimiter.tryAcquire()) {
            LOG.warn("Rate limit exceeded for client: {}", socket.getRemoteSocketAddress());
            session.writer.println(gson.toJson(error("TOO_MANY_REQUESTS")));
            return;
        }
        try {
            ClientToServerMessage request = gson.fromJson(line, ClientToServerMessage.class);
            if (request == null || request.type == null) {
                session.writer.println(gson.toJson(error("Missing message type")));
                return;
            }
            ServerToClientMessage response = handleRequest(request, session);
            session.writer.println(gson.toJson(response));
        } catch (Throwable ex) {
            LOG.error("Error processing line", ex);
            session.writer.println(gson.toJson(error("Server error: " + ex.getMessage())));
        }
    }

    private ServerToClientMessage handleRequest(ClientToServerMessage req, ClientSession session) {
        // 1. Actions that DON'T require authentication
        if (req.type == MessageType.PING) {
            ServerToClientMessage pong = new ServerToClientMessage();
            pong.type = MessageType.PONG;
            pong.success = true;
            return pong;
        }
        if (req.type == MessageType.REGISTER) return handleRegister(req);
        if (req.type == MessageType.LOGIN) return handleLogin(req, session);
        if (req.type == MessageType.GET_AUCTIONS) return handleGetAuctions(req);

        // 2. Actions that REQUIRE authentication
        if (authenticatedEmail == null || session.userId == null) {
            return error("UNAUTHORIZED: Please login first");
        }

        User currentUser = authService.findByEmail(authenticatedEmail);
        if (currentUser == null) return error("USER_NOT_FOUND");

        return switch (req.type) {
            case LOGOUT -> handleLogout(session);
            case PLACE_BID -> {
                if (currentUser.getRole() != com.ltnc.auction.server.model.UserRole.BIDDER) {
                    yield error("FORBIDDEN: Only bidders can place bids");
                }
                yield handlePlaceBid(req);
            }
            case REGISTER_AUTO_BID -> handleRegisterAutoBid(req);
            case GET_WALLET -> handleGetWallet(req);
            case GET_WALLET_TRANSACTIONS -> handleGetWalletTransactions(req);
            case DEPOSIT -> handleDeposit(req, session);
            case WITHDRAW -> handleWithdraw(req, session);
            
            // Seller actions
            case CREATE_AUCTION -> {
                if (currentUser.getRole() != com.ltnc.auction.server.model.UserRole.SELLER && 
                    currentUser.getRole() != com.ltnc.auction.server.model.UserRole.ADMIN) {
                    yield error("FORBIDDEN: Only sellers can create auctions");
                }
                yield handleCreateAuction(req);
            }
            case CREATE_ITEM -> {
                if (currentUser.getRole() != com.ltnc.auction.server.model.UserRole.SELLER) {
                    yield error("FORBIDDEN: Only sellers can create items");
                }
                yield handleCreateItem(req);
            }
            case GET_ITEMS_BY_SELLER -> handleGetItemsBySeller(req);
            case UPDATE_ITEM -> {
                com.ltnc.auction.server.model.Item item = itemService.getItemById(req.itemId);
                if (item != null && currentUser.getRole() != com.ltnc.auction.server.model.UserRole.ADMIN && 
                    !authenticatedEmail.equalsIgnoreCase(item.getSellerEmail())) {
                    yield error("FORBIDDEN: You can only update your own items");
                }
                yield handleUpdateItem(req);
            }
            case DELETE_ITEM, DELETE_PRODUCT -> {
                com.ltnc.auction.server.model.Item item = itemService.getItemById(req.itemId);
                if (item != null && currentUser.getRole() != com.ltnc.auction.server.model.UserRole.ADMIN && 
                    !authenticatedEmail.equalsIgnoreCase(item.getSellerEmail())) {
                    yield error("FORBIDDEN: You can only delete your own items");
                }
                yield handleDeleteItem(req);
            }
            
            // Admin actions
            case GET_ALL_USERS -> {
                if (currentUser.getRole() != com.ltnc.auction.server.model.UserRole.ADMIN) {
                    yield error("FORBIDDEN: Admin only");
                }
                yield handleGetAllUsers();
            }
            case UPDATE_USER -> {
                if (currentUser.getRole() != com.ltnc.auction.server.model.UserRole.ADMIN && 
                    !currentUser.getId().equals(req.userId)) {
                    yield error("FORBIDDEN: You can only update your own profile");
                }
                yield handleUpdateUser(req);
            }
            case DELETE_USER -> {
                if (currentUser.getRole() != com.ltnc.auction.server.model.UserRole.ADMIN) {
                    yield error("FORBIDDEN: Admin only");
                }
                yield handleDeleteUser(req);
            }
            case DELETE_AUCTION -> {
                if (currentUser.getRole() != com.ltnc.auction.server.model.UserRole.ADMIN) {
                    yield error("FORBIDDEN: Admin only");
                }
                yield handleDeleteAuction(req);
            }
            case CANCEL_AUCTION -> {
                if (currentUser.getRole() != com.ltnc.auction.server.model.UserRole.ADMIN && 
                    !authenticatedEmail.equalsIgnoreCase(req.sellerEmail)) {
                    yield error("FORBIDDEN");
                }
                yield handleCancelAuction(req);
            }
            case UPDATE_AUCTION -> {
                com.ltnc.auction.server.model.Auction auction = auctionService.findCachedAuction(req.auctionId);
                if (auction != null && currentUser.getRole() != com.ltnc.auction.server.model.UserRole.ADMIN && 
                    !authenticatedEmail.equalsIgnoreCase(auction.getSellerEmail())) {
                    yield error("FORBIDDEN: You can only update your own auctions");
                }
                yield handleUpdateAuction(req);
            }
            case CONFIRM_PAYMENT -> {
                if (currentUser.getRole() != com.ltnc.auction.server.model.UserRole.ADMIN) {
                    yield error("FORBIDDEN: Admin only");
                }
                yield handleConfirmPayment(req);
            }
            case GET_BID_LOGS -> handleGetBidLogs();
            case GET_NOTIFICATIONS -> handleGetNotifications(session);
            case MARK_NOTIFICATION_READ -> handleMarkNotificationRead(req, session);
            case MARK_ALL_NOTIFICATIONS_READ -> handleMarkAllNotificationsRead(session);
            case MARK_NOTIFICATION_UNREAD -> handleMarkNotificationUnread(req, session);
            default -> error("Unknown message type: " + req.type);
        };
    }

    private ServerToClientMessage handleUpdateAuction(ClientToServerMessage req) {
        LocalDateTime end = null;
        if (req.endTime != null && !req.endTime.isBlank()) {
            try {
                end = LocalDateTime.parse(req.endTime, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (Exception ignored) {}
        }
        boolean ok = auctionService.updateAuction(req.auctionId, req.title, req.description, req.startingBid, end, req.status);
        ServerToClientMessage resp = new ServerToClientMessage();
        resp.type = MessageType.UPDATE_AUCTION_RESULT;
        resp.success = ok;
        return resp;
    }

    private ServerToClientMessage handleConfirmPayment(ClientToServerMessage req) {
        boolean ok = auctionService.confirmPayment(req.auctionId);
        ServerToClientMessage resp = new ServerToClientMessage();
        resp.type = MessageType.CONFIRM_PAYMENT_RESULT;
        resp.success = ok;
        return resp;
    }

    private ServerToClientMessage handleDeleteAuction(ClientToServerMessage req) {
        boolean ok = auctionService.deleteAuction(req.auctionId);
        ServerToClientMessage resp = new ServerToClientMessage();
        resp.type = MessageType.AUCTION_LIST; // Can return anything simple
        resp.success = ok;
        return resp;
    }

    private ServerToClientMessage handleRegister(ClientToServerMessage req) {
        AuthService.RegisterResult result = authService.register(req.fullName, req.email, req.password, req.role);
        ServerToClientMessage resp = new ServerToClientMessage();
        resp.type = MessageType.AUTH_RESULT;
        resp.success = result.success();
        if (result.success()) resp.user = AuctionMapper.toUserDto(result.user());
        else resp.error = result.code();
        return resp;
    }

    private ServerToClientMessage handleLogin(ClientToServerMessage req, ClientSession session) {
        AuthService.LoginResult result = authService.login(req.email, req.password);
        ServerToClientMessage resp = new ServerToClientMessage();
        resp.type = MessageType.AUTH_RESULT;
        resp.success = result.success();
        if (result.success()) {
            authenticatedEmail = result.user().getEmail();
            session.email = authenticatedEmail;
            session.userId = result.user().getId();
            resp.user = AuctionMapper.toUserDto(result.user());
        } else {
            resp.error = result.code();
        }
        return resp;
    }

    private ServerToClientMessage handleLogout(ClientSession session) {
        authenticatedEmail = null;
        session.userId = null;
        session.email = null;
        ServerToClientMessage resp = new ServerToClientMessage();
        resp.type = MessageType.AUTH_RESULT;
        resp.success = true;
        return resp;
    }

    private ServerToClientMessage handleGetAuctions(ClientToServerMessage req) {
        ServerToClientMessage resp = new ServerToClientMessage();
        resp.type = MessageType.AUCTION_LIST;
        resp.success = true;
        if (req != null && req.page != null && req.limit != null) {
            resp.auctions = auctionService.getAuctions(req.page, req.limit);
        } else {
            resp.auctions = auctionService.getAllAuctions();
        }
        return resp;
    }

    private ServerToClientMessage handlePlaceBid(ClientToServerMessage req) {
        String email = authenticatedEmail;
        BidResult result = auctionService.placeBid(req.auctionId, email, req.bidAmount);
        ServerToClientMessage resp = new ServerToClientMessage();
        resp.type = MessageType.BID_RESULT;
        resp.success = result.code() == BidResultCode.OK;
        if (!resp.success) resp.error = result.code().name();
        resp.auction = result.auction();
        if (result.requiredTopUp() != null) {
            resp.requiredTopUp = result.requiredTopUp();
        }
        return resp;
    }

    private ServerToClientMessage handleGetWallet(ClientToServerMessage req) {
        String email = authenticatedEmail;
        User user = authService.findByEmail(email);
        if (user == null) return error("USER_NOT_FOUND");
        Wallet w = WalletService.getInstance().getWallet(user.getId());
        ServerToClientMessage resp = new ServerToClientMessage();
        resp.type = MessageType.WALLET_RESULT;
        resp.success = true;
        resp.wallet = AuctionMapper.toWalletDto(w);
        return resp;
    }

    private ServerToClientMessage handleGetWalletTransactions(ClientToServerMessage req) {
        String email = authenticatedEmail;
        User user = authService.findByEmail(email);
        if (user == null) return error("USER_NOT_FOUND");

        List<WalletTransaction> txs = WalletService.getInstance().getTransactions(user.getId());
        List<WalletTransactionDto> dtos = new ArrayList<>();
        java.time.format.DateTimeFormatter iso = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        for (WalletTransaction tx : txs) {
            WalletTransactionDto dto = new WalletTransactionDto();
            dto.id = tx.getId();
            dto.userId = tx.getUserId();
            dto.type = tx.getType().name();
            dto.amount = tx.getAmount().doubleValue();
            dto.refAuctionId = tx.getRefAuctionId();
            if (tx.getRefAuctionId() != null) {
                com.ltnc.auction.server.model.Auction a = auctionService.findCachedAuction(tx.getRefAuctionId());
                if (a != null) {
                    dto.refAuctionTitle = a.getTitle();
                }
            }
            dto.createdAt = tx.getCreatedAt() != null ? tx.getCreatedAt().format(iso) : null;
            dtos.add(dto);
        }

        ServerToClientMessage resp = new ServerToClientMessage();
        resp.type = MessageType.WALLET_TRANSACTIONS_RESULT;
        resp.success = true;
        resp.walletTransactions = dtos;
        return resp;
    }

    private ServerToClientMessage handleDeposit(ClientToServerMessage req, ClientSession session) {
        String email = authenticatedEmail;
        if (req.amount == null || req.amount <= 0) return error("INVALID_AMOUNT");
        User user = authService.findByEmail(email);
        if (user == null) return error("USER_NOT_FOUND");
        WalletService.getInstance().deposit(user.getId(), BigDecimal.valueOf(req.amount));
        pushWalletBroadcast(user.getId());
        ServerToClientMessage resp = new ServerToClientMessage();
        resp.type = MessageType.DEPOSIT_RESULT;
        resp.success = true;
        Wallet w = WalletService.getInstance().getWallet(user.getId());
        resp.wallet = AuctionMapper.toWalletDto(w);
        return resp;
    }

    private ServerToClientMessage handleWithdraw(ClientToServerMessage req, ClientSession session) {
        String email = authenticatedEmail;
        if (req.amount == null || req.amount <= 0) return error("INVALID_AMOUNT");
        User user = authService.findByEmail(email);
        if (user == null) return error("USER_NOT_FOUND");
        WalletService.WithdrawResultCode wc =
                WalletService.getInstance().withdraw(user.getId(), BigDecimal.valueOf(req.amount));
        ServerToClientMessage resp = new ServerToClientMessage();
        resp.type = MessageType.WITHDRAW_RESULT;
        resp.success = wc == WalletService.WithdrawResultCode.OK;
        if (!resp.success) resp.error = "WITHDRAW_INSUFFICIENT_AVAILABLE";
        Wallet w = WalletService.getInstance().getWallet(user.getId());
        resp.wallet = AuctionMapper.toWalletDto(w);
        if (resp.success) pushWalletBroadcast(user.getId());
        return resp;
    }

    private void pushWalletBroadcast(Long userId) {
        Wallet w = WalletService.getInstance().getWallet(userId);
        ServerToClientMessage msg = new ServerToClientMessage();
        msg.type = MessageType.WALLET_UPDATE;
        msg.eventType = "WALLET_UPDATE";
        msg.userId = userId;
        msg.balance = w.getBalance().doubleValue();
        msg.reserved = w.getReserved().doubleValue();
        msg.available = w.getAvailable().doubleValue();
        msg.wallet = AuctionMapper.toWalletDto(w);
        msg.serverCurrentTimeMs = System.currentTimeMillis();
        broadcastManager.broadcastToUser(userId, msg);
    }

    private ServerToClientMessage handleRegisterAutoBid(ClientToServerMessage req) {
        String email = authenticatedEmail;

        boolean ok = auctionService.registerAutoBid(req.auctionId, email, req.maxBid, req.increment);
        ServerToClientMessage resp = new ServerToClientMessage();
        resp.type = MessageType.AUTO_BID_RESULT;
        resp.success = ok;
        if (!ok) {
            resp.error = "INVALID_PARAMETERS";
        }
        return resp;
    }

    private ServerToClientMessage handleCreateAuction(ClientToServerMessage req) {
        String email = authenticatedEmail;
        java.time.LocalDateTime start = null;
        java.time.LocalDateTime end = null;
        java.time.format.DateTimeFormatter iso = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        try {
            if (req.startTime != null && !req.startTime.isBlank()) {
                start = java.time.LocalDateTime.parse(req.startTime, iso);
            }
            if (req.endTime != null && !req.endTime.isBlank()) {
                end = java.time.LocalDateTime.parse(req.endTime, iso);
            }
        } catch (Exception e) {
            LOG.warn("Failed to parse start or end time: {} / {}", req.startTime, req.endTime);
        }
        
        if (start == null) start = java.time.LocalDateTime.now();
        if (end == null) {
            int duration = req.durationMinutes != null ? req.durationMinutes : 120;
            end = start.plusMinutes(duration);
        }

        AuctionDto auction = auctionService.createAuction(req.itemId, email, start, end);
        ServerToClientMessage resp = new ServerToClientMessage();
        resp.type = MessageType.CREATE_AUCTION_RESULT;
        resp.success = auction != null;
        resp.auction = auction;
        if (auction == null) resp.error = "Failed to create auction";
        return resp;
    }

    private ServerToClientMessage handleCancelAuction(ClientToServerMessage req) {
        boolean ok = auctionService.cancelAuction(req.auctionId);
        ServerToClientMessage resp = new ServerToClientMessage();
        resp.type = MessageType.CANCEL_AUCTION_RESULT;
        resp.success = ok;
        if (!ok) resp.error = "Failed to cancel auction";
        return resp;
    }

    private ServerToClientMessage handleCreateItem(ClientToServerMessage req) {
        String email = authenticatedEmail;
        ItemDto item = itemService.createItem(req.itemType, req.itemName, req.itemDescription,
                req.itemStartingBid != null ? req.itemStartingBid : 0, req.itemImageUrl, email);
        ServerToClientMessage resp = new ServerToClientMessage();
        resp.type = MessageType.ITEM_RESULT;
        resp.success = item != null;
        resp.item = item;
        return resp;
    }

    private ServerToClientMessage handleGetItemsBySeller(ClientToServerMessage req) {
        String email = authenticatedEmail;
        User user = authService.findByEmail(email);
        ServerToClientMessage resp = new ServerToClientMessage();
        resp.type = MessageType.ITEM_LIST;
        resp.success = true;
        resp.items = (user != null && user.getRole() == com.ltnc.auction.server.model.UserRole.SELLER) 
                ? itemService.getItemsBySeller(email) 
                : itemService.getAllItems();
        return resp;
    }

    private ServerToClientMessage handleUpdateItem(ClientToServerMessage req) {
        ItemDto item = itemService.updateItem(req.itemId, req.itemType, req.itemName,
                req.itemDescription, req.itemStartingBid != null ? req.itemStartingBid : 0, req.itemImageUrl);
        ServerToClientMessage resp = new ServerToClientMessage();
        resp.type = MessageType.ITEM_RESULT;
        resp.success = item != null;
        resp.item = item;
        return resp;
    }

    private ServerToClientMessage handleDeleteItem(ClientToServerMessage req) {
        boolean ok = itemService.deleteItem(req.itemId);
        ServerToClientMessage resp = new ServerToClientMessage();
        resp.type = MessageType.ITEM_RESULT;
        resp.success = ok;
        return resp;
    }

    private ServerToClientMessage handleGetAllUsers() {
        ServerToClientMessage resp = new ServerToClientMessage();
        resp.type = MessageType.USER_LIST;
        resp.success = true;
        resp.users = authService.getAllUsers().stream().map(AuctionMapper::toUserDto).collect(Collectors.toList());
        return resp;
    }

    private ServerToClientMessage handleUpdateUser(ClientToServerMessage req) {
        User currentUser = authService.findByEmail(authenticatedEmail);
        String requestedRole = req.newRole;
        
        // Security: Only Admin can change roles
        if (currentUser == null || currentUser.getRole() != com.ltnc.auction.server.model.UserRole.ADMIN) {
            requestedRole = null; // Ignore role change attempt from non-admin
        }
        
        boolean ok = authService.updateUser(req.userId, req.fullName, req.newPassword, requestedRole);
        ServerToClientMessage resp = new ServerToClientMessage();
        resp.type = MessageType.AUTH_RESULT;
        resp.success = ok;
        return resp;
    }

    private ServerToClientMessage handleDeleteUser(ClientToServerMessage req) {
        String result = authService.deleteUser(req.userId);
        boolean ok = "OK".equals(result);
        
        if (ok && req.userId != null) {
            ServerToClientMessage forceLogout = new ServerToClientMessage();
            forceLogout.type = MessageType.FORCE_LOGOUT;
            forceLogout.error = "Your account has been deleted by administrator.";
            broadcastManager.broadcastToUser(req.userId, forceLogout);
        }
        
        ServerToClientMessage resp = new ServerToClientMessage();
        resp.type = MessageType.AUTH_RESULT;
        resp.success = ok;
        if (!ok) {
            resp.error = result;
        }
        return resp;
    }

    private ServerToClientMessage handleGetBidLogs() {
        ServerToClientMessage resp = new ServerToClientMessage();
        resp.type = MessageType.BID_LOG_LIST;
        resp.success = true;
        resp.bids = auctionService.getBidLogs();
        return resp;
    }

    private ServerToClientMessage handleGetNotifications(ClientSession session) {
        ServerToClientMessage resp = new ServerToClientMessage();
        resp.type = MessageType.NOTIFICATIONS_RESULT;
        resp.success = true;
        resp.notifications = com.ltnc.auction.server.service.NotificationService.getInstance().getUserNotifications(session.userId);
        return resp;
    }

    private ServerToClientMessage handleMarkNotificationRead(ClientToServerMessage req, ClientSession session) {
        ServerToClientMessage resp = new ServerToClientMessage();
        resp.type = MessageType.NOTIFICATIONS_RESULT;
        if (req.notificationId != null) {
            resp.success = com.ltnc.auction.server.service.NotificationService.getInstance().markAsRead(req.notificationId, session.userId);
            resp.notifications = com.ltnc.auction.server.service.NotificationService.getInstance().getUserNotifications(session.userId);
        } else {
            resp.success = false;
        }
        return resp;
    }

    private ServerToClientMessage handleMarkNotificationUnread(ClientToServerMessage req, ClientSession session) {
        ServerToClientMessage resp = new ServerToClientMessage();
        resp.type = MessageType.NOTIFICATIONS_RESULT;
        if (req.notificationId != null) {
            resp.success = com.ltnc.auction.server.service.NotificationService.getInstance().markAsUnread(req.notificationId, session.userId);
            resp.notifications = com.ltnc.auction.server.service.NotificationService.getInstance().getUserNotifications(session.userId);
        } else {
            resp.success = false;
        }
        return resp;
    }

    private ServerToClientMessage handleMarkAllNotificationsRead(ClientSession session) {
        ServerToClientMessage resp = new ServerToClientMessage();
        resp.type = MessageType.NOTIFICATIONS_RESULT;
        resp.success = com.ltnc.auction.server.service.NotificationService.getInstance().markAllAsRead(session.userId);
        resp.notifications = com.ltnc.auction.server.service.NotificationService.getInstance().getUserNotifications(session.userId);
        return resp;
    }

    private ServerToClientMessage error(String message) {
        ServerToClientMessage resp = new ServerToClientMessage();
        resp.type = MessageType.ERROR;
        resp.success = false;
        resp.error = message;
        return resp;
    }
}