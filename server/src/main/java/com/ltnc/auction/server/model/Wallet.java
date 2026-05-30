package com.ltnc.auction.server.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Wallet {
    private Long userId;
    private BigDecimal balance = BigDecimal.ZERO;
    private BigDecimal reserved = BigDecimal.ZERO;
    private LocalDateTime updatedAt;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
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

    /** balance - reserved, never negative */
    public BigDecimal getAvailable() {
        BigDecimal a = getBalance().subtract(getReserved());
        return a.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : a;
    }
}
