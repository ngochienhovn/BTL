package com.ltnc.auction.server.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Wallet {
    private Long userId;
<<<<<<< HEAD
    private BigDecimal balance = BigDecimal.ZERO;
    private BigDecimal reserved = BigDecimal.ZERO;
    private LocalDateTime updatedAt;

=======
    private BigDecimal balance;
    private BigDecimal reserved;
    private LocalDateTime updatedAt;

    public Wallet() {
        this.balance = BigDecimal.ZERO;
        this.reserved = BigDecimal.ZERO;
        this.updatedAt = LocalDateTime.now();
    }

    public Wallet(Long userId, BigDecimal balance, BigDecimal reserved, LocalDateTime updatedAt) {
        this.userId = userId;
        this.balance = balance != null ? balance : BigDecimal.ZERO;
        this.reserved = reserved != null ? reserved : BigDecimal.ZERO;
        this.updatedAt = updatedAt;
    }

>>>>>>> a4a9980ce3593461ea601ec5d280f231fec24645
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

<<<<<<< HEAD
    /** balance - reserved, never negative */
    public BigDecimal getAvailable() {
        BigDecimal a = getBalance().subtract(getReserved());
        return a.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : a;
    }
=======
    public BigDecimal getAvailable() {
        BigDecimal available = balance.subtract(reserved);
        return available.max(BigDecimal.ZERO);
    }
    
>>>>>>> a4a9980ce3593461ea601ec5d280f231fec24645
}
