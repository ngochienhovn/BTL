package com.ltnc.auction.server.service;

import com.ltnc.auction.server.dao.NotificationDAO;
import com.ltnc.auction.server.model.Notification;
import com.ltnc.auction.server.service.BroadcastManager;
import com.ltnc.auction.shared.dto.NotificationDto;
import com.ltnc.auction.shared.protocol.MessageType;
import com.ltnc.auction.shared.protocol.ServerToClientMessage;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class NotificationService {
    private static final NotificationService INSTANCE = new NotificationService();
    private final NotificationDAO dao;
    private final BroadcastManager broadcastManager;

    private NotificationService() {
        this.dao = new NotificationDAO();
        this.broadcastManager = BroadcastManager.getInstance();
    }
    
    public static NotificationService getInstance() {
        return INSTANCE;
    }

    public void createAndSendNotification(Long userId, String title, String message, String type) {
        if (userId == null) return;
        
        Notification n = new Notification(userId, title, message, type);
        Long id = dao.insert(n);
        if (id != null) {
            n.setId(id);
            // push to client via Socket
            NotificationDto dto = mapToDto(n);
            ServerToClientMessage msg = new ServerToClientMessage();
            msg.type = MessageType.NEW_NOTIFICATION;
            msg.notification = dto;
            broadcastManager.broadcastToUser(userId, msg);
            System.out.println("[NOTIFICATION] Created for user " + userId + ": " + title);
        }
    }

    public List<NotificationDto> getUserNotifications(Long userId) {
        List<Notification> list = dao.findByUserId(userId);
        return list.stream().map(this::mapToDto).toList();
    }

    public boolean markAsRead(Long id, Long userId) {
        return dao.markAsRead(id, userId);
    }

    public boolean markAsUnread(Long id, Long userId) {
        return dao.markAsUnread(id, userId);
    }

    public boolean markAllAsRead(Long userId) {
        return dao.markAllAsRead(userId);
    }

    private NotificationDto mapToDto(Notification n) {
        NotificationDto dto = new NotificationDto();
        dto.id = n.getId();
        dto.userId = n.getUserId();
        dto.title = n.getTitle();
        dto.message = n.getMessage();
        dto.type = n.getType();
        dto.isRead = n.isRead();
        dto.createdAt = n.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        return dto;
    }
}
