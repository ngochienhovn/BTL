package com.ltnc.auction.shared.dto;

import java.io.Serializable;

public class NotificationDto implements Serializable {
    public Long id;
    public Long userId;
    public String title;
    public String message;
    public String type; // e.g. "INFO", "WARNING", "SUCCESS"
    public boolean isRead;
    public String createdAt; // ISO Format string

    public NotificationDto() {}

    public NotificationDto(Long id, Long userId, String title, String message, String type, boolean isRead, String createdAt) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.type = type;
        this.isRead = isRead;
        this.createdAt = createdAt;
    }
}
