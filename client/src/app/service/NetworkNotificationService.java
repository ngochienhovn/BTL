package app.service;

import app.model.User;
import com.ltnc.auction.shared.dto.NotificationDto;
import com.ltnc.auction.shared.protocol.ClientToServerMessage;
import com.ltnc.auction.shared.protocol.MessageType;
import com.ltnc.auction.shared.protocol.ServerToClientMessage;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import app.net.SocketClient;

public class NetworkNotificationService {
    private static final NetworkNotificationService INSTANCE = new NetworkNotificationService();
    private final ObservableList<NotificationDto> notifications = FXCollections.observableArrayList();
    private Runnable unreadCountObserver;

    private NetworkNotificationService() {
        SocketClient.getInstance().addBroadcastListener(msg -> {
            if (msg.type == MessageType.NOTIFICATIONS_RESULT) {
                if (msg.notifications != null) {
                    Platform.runLater(() -> {
                        boolean listChanged = false;
                        if (notifications.size() != msg.notifications.size()) {
                            notifications.clear();
                            notifications.addAll(msg.notifications);
                            listChanged = true;
                        } else {
                            for (int i = 0; i < msg.notifications.size(); i++) {
                                NotificationDto oldN = notifications.get(i);
                                NotificationDto newN = msg.notifications.get(i);
                                if (!oldN.id.equals(newN.id) || oldN.isRead != newN.isRead || !oldN.title.equals(newN.title)) {
                                    notifications.set(i, newN);
                                    listChanged = true;
                                }
                            }
                        }
                        if (listChanged) {
                            notifyObserver();
                        }
                    });
                }
            } else if (msg.type == MessageType.NEW_NOTIFICATION && msg.notification != null) {
                Platform.runLater(() -> {
                    notifications.add(0, msg.notification);
                    notifyObserver();
                    // Also show toast
                    String toastType = msg.notification.type;
                    NotificationService.NotificationType nt = NotificationService.NotificationType.INFO;
                    if ("SUCCESS".equals(toastType)) nt = NotificationService.NotificationType.SUCCESS;
                    if ("WARNING".equals(toastType)) nt = NotificationService.NotificationType.WARNING;
                    if ("ERROR".equals(toastType)) nt = NotificationService.NotificationType.ERROR;
                    NotificationService.getInstance().showNotification(msg.notification.title, msg.notification.message, nt);
                });
            }
        });
    }

    public static NetworkNotificationService getInstance() {
        return INSTANCE;
    }

    public void setUnreadCountObserver(Runnable observer) {
        this.unreadCountObserver = observer;
    }

    private void notifyObserver() {
        if (unreadCountObserver != null) {
            unreadCountObserver.run();
        }
    }

    public ObservableList<NotificationDto> getNotifications() {
        return notifications;
    }

    public long getUnreadCount() {
        return notifications.stream().filter(n -> !n.isRead).count();
    }

    public void fetchNotifications() {
        User user = NetworkUserService.getInstance().getCurrentUser();
        if (user == null) return;
        ClientToServerMessage req = new ClientToServerMessage();
        req.type = MessageType.GET_NOTIFICATIONS;
        SocketClient.getInstance().sendMessage(req);
    }

    public void markAsRead(Long notificationId) {
        ClientToServerMessage req = new ClientToServerMessage();
        req.type = MessageType.MARK_NOTIFICATION_READ;
        req.notificationId = notificationId;
        SocketClient.getInstance().sendMessage(req);
        
        // Optimistic update
        notifications.stream().filter(n -> n.id.equals(notificationId)).findFirst().ifPresent(n -> {
            n.isRead = true;
            notifyObserver();
            int idx = notifications.indexOf(n);
            if (idx >= 0) {
                NotificationDto copy = new NotificationDto(n.id, n.userId, n.title, n.message, n.type, true, n.createdAt);
                Platform.runLater(() -> notifications.set(idx, copy));
            }
        });
    }

    public void markAsUnread(Long notificationId) {
        ClientToServerMessage req = new ClientToServerMessage();
        req.type = MessageType.MARK_NOTIFICATION_UNREAD;
        req.notificationId = notificationId;
        SocketClient.getInstance().sendMessage(req);
        
        // Optimistic update
        notifications.stream().filter(n -> n.id.equals(notificationId)).findFirst().ifPresent(n -> {
            n.isRead = false;
            notifyObserver();
            int idx = notifications.indexOf(n);
            if (idx >= 0) {
                NotificationDto copy = new NotificationDto(n.id, n.userId, n.title, n.message, n.type, false, n.createdAt);
                Platform.runLater(() -> notifications.set(idx, copy));
            }
        });
    }

    public void markAllAsRead() {
        ClientToServerMessage req = new ClientToServerMessage();
        req.type = MessageType.MARK_ALL_NOTIFICATIONS_READ;
        SocketClient.getInstance().sendMessage(req);

        // Optimistic update
        boolean changed = false;
        for (NotificationDto n : notifications) {
            if (!n.isRead) {
                n.isRead = true;
                changed = true;
            }
        }
        if (changed) {
            notifyObserver();
            Platform.runLater(() -> {
                java.util.List<NotificationDto> copies = notifications.stream()
                    .map(n -> new NotificationDto(n.id, n.userId, n.title, n.message, n.type, n.isRead, n.createdAt))
                    .toList();
                notifications.setAll(copies);
            });
        }
    }
}
