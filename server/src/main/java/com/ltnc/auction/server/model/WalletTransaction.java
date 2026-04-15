package com.ltnc.auction.server.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WalletTransaction extend Entity {

     private String userId;
    private String type; 
    private BigDecimal amount;
    private Long refAuctionId;
    private LocalDateTime createdAt;

    public WalletTransaction() {
        this.amount = BigDecimal.ZERO;
        this.createdAt = LocalDateTime.now();
    }

    public WalletTransaction(String userId, String type, BigDecimal amount, Long refAuctionId, LocalDateTime createdAt) {
        this.userId = userId;
        this.type = type;
        this.amount = amount != null ? amount : BigDecimal.ZERO;
        this.refAuctionId = refAuctionId;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount != null ? amount : BigDecimal.ZERO;
    }

    public Long getRefAuctionId() {
        return refAuctionId;
    }

    public void setRefAuctionId(Long refAuctionId) {
        this.refAuctionId = refAuctionId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
}
