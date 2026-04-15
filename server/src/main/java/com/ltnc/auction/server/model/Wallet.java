package com.ltnc.auction.server.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Wallet extends Entity {

    private String userId;
    private BigDecimal balance;
    private BigDecimal reserved;
    private LocalDateTime updatedAt;

    public Wallet() {
        this.balance = BigDecimal.ZERO;
        this.reserved = BigDecimal.ZERO;
        this.updatedAt = LocalDateTime.now();
    }

    public Wallet(String userId, BigDecimal balance, BigDecimal reserved, LocalDateTime updatedAt) {
        this.userId = userId;
        this.balance = balance != null ? balance : BigDecimal.ZERO;
        this.reserved = reserved != null ? reserved : BigDecimal.ZERO;
        this.updatedAt = updatedAt;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance != null ? balance : BigDecimal.ZERO;
    }

    public BigDecimal getReserved() {
        return reserved;
    }

    public void setReserved(BigDecimal reserved) {
        this.reserved = reserved != null ? reserved : BigDecimal.ZERO;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public BigDecimal getAvailable() {
        BigDecimal available = balance.subtract(reserved);
        return available.max(BigDecimal.ZERO);
    }
    
}
