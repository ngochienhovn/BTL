package com.ltnc.auction.server.network;

import com.google.gson.Gson;
import com.ltnc.auction.server.model.Item;
import com.ltnc.auction.server.model.User;
import com.ltnc.auction.server.model.UserRole;
import com.ltnc.auction.server.services.AuthService;
import com.ltnc.auction.server.services.ItemService;
import com.ltnc.auction.server.services.AuctionService; // Sprint 3
import com.ltnc.auction.server.services.WalletService; // Sprint 3
import com.ltnc.auction.shared.protocol.ClientToServerMessage;
import com.ltnc.auction.shared.protocol.MessageType;
import com.ltnc.auction.shared.protocol.ServerToClientMessage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final AuthService authService;
    private final ItemService itemService;
    private final AuctionService auctionService; // Sprint 3
    private final WalletService walletService; // Sprint 3
    private final Gson gson = new Gson();

    public ClientHandler(Socket socket, AuthService authService, ItemService itemService, AuctionService auctionService, WalletService walletService) {
        this.socket = socket;
        this.authService = authService;
        this.itemService = itemService;
        this.auctionService = auctionService;
        this.walletService = walletService;
    }

    @Override
    public void run() {
        try (
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[server] Received message from client: " + line);
                ClientToServerMessage request = gson.fromJson(line, ClientToServerMessage.class);
                ServerToClientMessage response = handleMessage(request);
                writer.println(gson.toJson(response));
            }
        } catch (IOException e) {
            System.err.println("[server] Error handling client: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private ServerToClientMessage handleMessage(ClientToServerMessage request) {
        ServerToClientMessage response = new ServerToClientMessage();
        if (request == null || request.type == null) {
            response.type = MessageType.ERROR;
            response.success = false;
            response.error = "Invalid request";
            return response;
        }

        switch (request.type) {
            case LOGIN -> response = handleLogin(request);
            case REGISTER -> response = handleRegister(request);
            case CREATE_ITEM -> response = handleCreateItem(request);
            case UPDATE_ITEM -> response = handleUpdateItem(request);
            case DELETE_ITEM -> response = handleDeleteItem(request);
            case GET_ITEMS_BY_SELLER -> response = handleGetItemsBySeller(request);

            case GET_AUCTIONS -> response = handleGetAuctions(request); // Sprint 3
            case PLACE_BID -> response = handlePlaceBid(request);
            case GET_WALLET -> response = handleGetWallet(request);
            case DEPOSIT -> response = handleDeposit(request);
            case WITHDRAW -> response = handleWithdraw(request);
            
            default -> {
                response.type = MessageType.ERROR;
                response.success = false;
                response.error = "Unsupported message type: " + request.type;
            }
        }
        return response;
    }

    private ServerToClientMessage handleLogin(ClientToServerMessage request) {
        AuthService.LoginResult result = authService.login(request.email, request.password);
        ServerToClientMessage response = new ServerToClientMessage();
        response.type = MessageType.LOGIN_RESULT;
        response.success = result.success();
        response.code = result.code();
        if (!result.success()) {
            response.error = result.code();
            return response;
        }
        response.data = userToMap(result.user());
        return response;
    }

     private ServerToClientMessage handleRegister(ClientToServerMessage request) {
        UserRole role = parseRole(request.role);
        AuthService.RegisterResult result = authService.register(
                request.fullName,
                request.email,
                request.password,
                role
        );
        ServerToClientMessage response = new ServerToClientMessage();
        response.type = MessageType.REGISTER_RESULT;
        response.success = result.success();
        response.code = result.code();
        if (!result.success()) {
            response.error = result.code();
            return response;
        }
        response.data = userToMap(result.user());
        return response;
    }

    private ServerToClientMessage handleCreateItem(ClientToServerMessage request) {
        User actor = actorFromRequest(request);
        ItemService.ServiceResult result = itemService.createItem(
                actor,
                request.typeOfItem,
                request.itemName,
                request.itemDescription,
                request.itemStartingBid,
                request.imageUrl
        );
        ServerToClientMessage response = new ServerToClientMessage();
        response.type = MessageType.CREATE_ITEM_RESULT;
        response.success = result.success();
        response.code = result.code();
        response.message = result.message();
        if (!result.success()) {
            response.error = result.message();
        } else {
            response.data = Map.of("itemId", result.itemId());
        }
        return response;
    }

    private ServerToClientMessage handleUpdateItem(ClientToServerMessage request) {
        User actor = actorFromRequest(request);
        ItemService.ServiceResult result = itemService.updateItem(
                actor,
                request.itemId,
                request.typeOfItem,
                request.itemName,
                request.itemDescription,
                request.itemStartingBid,
                request.imageUrl
        );
        ServerToClientMessage response = new ServerToClientMessage();
        response.type = MessageType.UPDATE_ITEM_RESULT;
        response.success = result.success();
        response.code = result.code();
        response.message = result.message();
        if (!result.success()) {
            response.error = result.message();
        }
        return response;
    }

    private ServerToClientMessage handleDeleteItem(ClientToServerMessage request) {
        User actor = actorFromRequest(request);
        ItemService.ServiceResult result = itemService.deleteItem(actor, request.itemId);
        ServerToClientMessage response = new ServerToClientMessage();
        response.type = MessageType.DELETE_ITEM_RESULT;
        response.success = result.success();
        response.code = result.code();
        response.message = result.message();
        if (!result.success()) {
            response.error = result.message();
        }
        return response;
    }

    private ServerToClientMessage handleGetItemsBySeller(ClientToServerMessage request) {
        User actor = actorFromRequest(request);
        List<Item> items = itemService.findBySeller(actor);
        ServerToClientMessage response = new ServerToClientMessage();
        response.type = MessageType.GET_ITEMS_BY_SELLER_RESULT;
        response.success = true;
        response.code = "OK";
        response.items = items.stream().map(this::itemToMap).toList();
        return response;
    }

    private User actorFromRequest(ClientToServerMessage request) {
        User user = new User();
        user.setId(request.userId == null ? request.sellerId : request.userId);
        user.setEmail(request.email == null ? request.sellerEmail : request.email);
        user.setRole(parseRole(request.role));
        return user;
    }

    private UserRole parseRole(String role) {
        try {
            return role == null ? UserRole.BIDDER : UserRole.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return UserRole.BIDDER;
        }
    }

    private Map<String, Object> userToMap(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("fullName", user.getFullName());
        map.put("email", user.getEmail());
        map.put("role", user.getRole().name());
        return map;
    }

    private Map<String, Object> itemToMap(Item item) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", item.getId());
        map.put("sellerId", item.getSellerId());
        map.put("sellerEmail", item.getSellerEmail());
        map.put("type", item.getType());
        map.put("name", item.getName());
        map.put("description", item.getDescription());
        map.put("startingBid", item.getStartingBid());
        map.put("imageUrl", item.getImageUrl());
        return map;
    }

    private ServerToClientMessage handleGetAuctions(ClientToServerMessage request) { // 
        ServerToClientMessage response = new ServerToClientMessage();

        response.type = MessageType.AUCTION_LIST;
        response.success = true;
        response.code = "OK";

        response.auctions = auctionService.getAllAuctions();

        return response;
    }

    private ServerToClientMessage handlePlaceBid(ClientToServerMessage request) {
        ServerToClientMessage response = new ServerToClientMessage();

        User actor = actorFromRequest(request);

        var result = auctionService.placeBid(
                request.auctionId,
                actor.getId(),
                request.bidAmount
        );

        response.type = MessageType.BID_RESULT;
        response.success = result.success();
        response.code = result.code();
        response.message = result.message();

        if (!result.success()) {
            response.error = result.message();
            response.requiredTopUp = result.requiredTopUp();
        }

        return response;
    }

    private ServerToClientMessage handleGetWallet(ClientToServerMessage request) {
        ServerToClientMessage response = new ServerToClientMessage();

        User actor = actorFromRequest(request);

        var wallet = walletService.getWallet(actor.getId());

        response.type = MessageType.WALLET_RESULT;
        response.success = true;
        response.code = "OK";

        response.balance = wallet.getBalance();
        response.reserved = wallet.getReserved();
        response.available = wallet.getAvailable();

        return response;
    }

    private ServerToClientMessage handleDeposit(ClientToServerMessage request) {
        ServerToClientMessage response = new ServerToClientMessage();

        User actor = actorFromRequest(request);

        boolean success = walletService.deposit(actor.getId(), request.amount);

        response.type = MessageType.DEPOSIT_RESULT;
        response.success = success;
        response.code = success ? "OK" : "FAILED";
        response.message = success ? "Nạp tiền thành công" : "Nạp tiền thất bại";

        return response;
    }

    private ServerToClientMessage handleWithdraw(ClientToServerMessage request) {
        ServerToClientMessage response = new ServerToClientMessage();

        User actor = actorFromRequest(request);

        boolean success = walletService.withdraw(actor.getId(), request.amount);

        response.type = MessageType.WITHDRAW_RESULT;
        response.success = success;
        response.code = success ? "OK" : "FAILED";
        response.message = success ? "Rút tiền thành công" : "Rút tiền thất bại";

        return response;
    }
}
