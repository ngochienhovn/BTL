package com.ltnc.auction.server.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WalletTransaction {
    private Long id;
    private Long userId;
<<<<<<< HEAD
    private WalletTxType type;
=======
    private String type; 
>>>>>>> a4a9980ce3593461ea601ec5d280f231fec24645
    private BigDecimal amount;
    private Long refAuctionId;
    private LocalDateTime createdAt;

<<<<<<< HEAD
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
=======
    public WalletTransaction() 
    {
        this.amount = BigDecimal.ZERO;
        this.createdAt = LocalDateTime.now();
    }

    public WalletTransaction(Long userId, String type, BigDecimal amount, Long refAuctionId, LocalDateTime createdAt)
    {
        this.userId = userId;
        this.type = type;
        this.amount = amount != null ? amount : BigDecimal.ZERO;
        this.refAuctionId = refAuctionId;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getUserId() 
    {
>>>>>>> a4a9980ce3593461ea601ec5d280f231fec24645
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

<<<<<<< HEAD
    public WalletTxType getType() {
        return type;
    }

    public void setType(WalletTxType type) {
=======
    public String getType() {
        return type;
    }

    public void setType(String type) {
>>>>>>> a4a9980ce3593461ea601ec5d280f231fec24645
        this.type = type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
<<<<<<< HEAD
        this.amount = amount;
=======
        this.amount = amount != null ? amount : BigDecimal.ZERO;
>>>>>>> a4a9980ce3593461ea601ec5d280f231fec24645
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
<<<<<<< HEAD
=======
    
>>>>>>> a4a9980ce3593461ea601ec5d280f231fec24645
}
