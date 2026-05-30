package com.ltnc.auction.server.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WalletTransaction {
    private Long id;
    private Long userId;
    private WalletTxType type;
    private BigDecimal amount;
    private Long refAuctionId;
    private LocalDateTime createdAt;

    public WalletTransaction() {}

    public WalletTransaction(Long userId, WalletTxType type, BigDecimal amount, Long refAuctionId) {
        this.userId = userId;
        this.type = type;
        this.amount = amount;
        this.refAuctionId = refAuctionId;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public WalletTxType getType() {
        return type;
    }

    public void setType(WalletTxType type) {
        this.type = type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
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
