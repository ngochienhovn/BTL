package app.service;

import app.model.Admin;
import app.model.Bidder;
import app.model.Seller;
import app.model.User;
import app.model.UserRole;
import com.ltnc.auction.shared.protocol.MessageType;
import com.ltnc.auction.shared.protocol.ClientToServerMessage;
import com.ltnc.auction.shared.protocol.ServerToClientMessage;
import com.ltnc.auction.shared.dto.UserDto;
import app.net.SocketClient;
import javafx.application.Platform;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class NetworkUserService implements IUserService {
    private static final NetworkUserService INSTANCE = new NetworkUserService();
    private User currentUser;
    private String lastPassword;
    
    private final List<UserObserver> observers = new CopyOnWriteArrayList<>();

    private NetworkUserService() {
        SocketClient.getInstance().addBroadcastListener(this::onBroadcastMessage);
    }

    public static NetworkUserService getInstance() { return INSTANCE; }

    public void registerObserver(UserObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
        }
    }

    public void unregisterObserver(UserObserver observer) {
        observers.remove(observer);
    }

    private void notifyObservers() {
        Platform.runLater(() -> {
            for (UserObserver observer : observers) {
                try {
                    observer.onUsersUpdated();
                } catch (Exception ignored) {}
            }
        });
    }

    private void onBroadcastMessage(ServerToClientMessage resp) {
        if (resp == null || resp.type == null) return;
        
        if (resp.type == MessageType.USER_CREATED) {
            notifyObservers();
        }
    }

    @Override
    public RegisterResult register(String fullName, String email, String password, UserRole role) {
        ClientToServerMessage req = new ClientToServerMessage();
        req.type = MessageType.REGISTER;
        req.fullName = fullName;
        req.email = email;
        req.password = password;
        req.role = role != null ? role.name() : "BIDDER";

        ServerToClientMessage resp = SocketClient.getInstance().sendAndReceive(req);
        if (resp == null) return new RegisterResult(false, "NO_RESPONSE", null);
        if (!resp.success) return new RegisterResult(false, resp.error != null ? resp.error : "FAILED", null);
        User user = mapUser(resp.user);
        notifyObservers();
        return new RegisterResult(true, "OK", user);
    }

    @Override
    public LoginResult login(String email, String password) {
        ClientToServerMessage req = new ClientToServerMessage();
        req.type = MessageType.LOGIN;
        req.email = email;
        req.password = password;

        ServerToClientMessage resp = SocketClient.getInstance().sendAndReceive(req);
        if (resp == null) return new LoginResult(false, "NO_RESPONSE", null);
        if (!resp.success) return new LoginResult(false, resp.error != null ? resp.error : "FAILED", null);
        User user = mapUser(resp.user);
        currentUser = user;
        lastPassword = password;
        return new LoginResult(true, "OK", user);
    }

    @Override
    public void logout() {
        ClientToServerMessage req = new ClientToServerMessage();
        req.type = MessageType.LOGOUT;
        SocketClient.getInstance().sendAndReceive(req);
        currentUser = null;
        lastPassword = null;
    }

    public void silentReLogin() {
        if (currentUser != null && lastPassword != null) {
            ClientToServerMessage req = new ClientToServerMessage();
            req.type = MessageType.LOGIN;
            req.email = currentUser.getEmail();
            req.password = lastPassword;
            SocketClient.getInstance().sendAndReceive(req);
        }
    }

    @Override
    public User getCurrentUser() { return currentUser; }

    @Override
    public boolean isLoggedIn() { return currentUser != null; }

    @Override
    public Collection<User> getAllUsers() {
        return getAllUsersList();
    }

    @Override
    public List<User> getAllUsersList() {
        ClientToServerMessage req = new ClientToServerMessage();
        req.type = MessageType.GET_ALL_USERS;
        ServerToClientMessage resp = SocketClient.getInstance().sendAndReceive(req);
        if (resp == null || !resp.success || resp.users == null) return new ArrayList<>();
        List<User> users = new ArrayList<>();
        for (UserDto info : resp.users) {
            User u = mapUser(info);
            if (u != null) users.add(u);
        }
        return users;
    }

    @Override
    public String deleteUser(String email) {
        // Find user by email first to get id
        List<User> all = getAllUsersList();
        User target = all.stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(email))
                .findFirst().orElse(null);
        if (target == null) return "USER_NOT_FOUND";

        ClientToServerMessage req = new ClientToServerMessage();
        req.type = MessageType.DELETE_USER;
        try {
            req.userId = Long.parseLong(target.getId());
        } catch (NumberFormatException e) {
            return "INVALID_USER_ID";
        }
        ServerToClientMessage resp = SocketClient.getInstance().sendAndReceive(req);
        if (resp != null && resp.success) {
            if (currentUser != null && currentUser.getEmail().equalsIgnoreCase(email)) {
                currentUser = null;
            }
            return "OK";
        }
        return (resp != null && resp.error != null) ? resp.error : "DELETE_FAILED";
    }

    @Override
    public boolean updateUser(String originalEmail, String fullName, String newPassword, UserRole role) {
        String targetId = null;
        if (currentUser != null && currentUser.getEmail().equalsIgnoreCase(originalEmail)) {
            targetId = currentUser.getId();
        } else {
            List<User> all = getAllUsersList();
            User target = all.stream()
                    .filter(u -> u.getEmail().equalsIgnoreCase(originalEmail))
                    .findFirst().orElse(null);
            if (target != null) {
                targetId = target.getId();
            }
        }
        if (targetId == null) return false;

        ClientToServerMessage req = new ClientToServerMessage();
        req.type = MessageType.UPDATE_USER;
        try {
            req.userId = Long.parseLong(targetId);
        } catch (NumberFormatException e) {
            return false;
        }
        req.fullName = fullName;
        req.newPassword = newPassword;
        req.newRole = role != null ? role.name() : null;
        ServerToClientMessage resp = SocketClient.getInstance().sendAndReceive(req);
        boolean success = resp != null && resp.success;
        if (success && currentUser != null && currentUser.getEmail().equalsIgnoreCase(originalEmail)) {
            UserDto info = new UserDto();
            info.id = Long.parseLong(currentUser.getId());
            info.fullName = (fullName != null && !fullName.isBlank()) ? fullName : currentUser.getFullName();
            info.email = currentUser.getEmail();
            info.role = (role != null) ? role.name() : currentUser.getRole().name();
            currentUser = mapUser(info);
            if (newPassword != null && !newPassword.isBlank()) {
                lastPassword = newPassword;
            }
        }
        return success;
    }

    private User mapUser(UserDto info) {
        if (info == null) return null;
        String idStr = info.id != null ? info.id.toString() : "0";
        return switch (info.role != null ? info.role : "BIDDER") {
            case "SELLER" -> new Seller(idStr, info.fullName, info.email, "");
            case "ADMIN" -> new Admin(idStr, info.fullName, info.email, "");
            default -> new Bidder(idStr, info.fullName, info.email, "");
        };
    }
}
