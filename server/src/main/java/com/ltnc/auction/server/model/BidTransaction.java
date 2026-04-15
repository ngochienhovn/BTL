package com.ltnc.auction.server.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BidTransaction extends Entity {

    private Long auctionId;
    private Long bidderId;
    private String bidderEmail;
    private String bidderName;
    private BigDecimal amount;
    private LocalDateTime createdAt;

    public BidTransaction() {
    }

    public BidTransaction (Long auctionId, Long bidderId, String bidderEmail, String bidderName, BigDecimal amount, LocalDateTime createdAt) {
        this.autionId = auctionId;
        this.bidderId = bidderId;
        this.bidderEmail = bidderEmail;
        this.bidderName = bidderName;
        this.amount = amount;
        this.createdAt = createdAt;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
    }

    public String getBidderId() {
        return bidderId;
    }

    public void setBidderId(String bidderId) {
        this.bidderId = bidderId;
    }

    public String getBidderEmail() {
        return bidderEmail;
    }

    public void setBidderEmail(String bidderEmail) {
        this.bidderEmail = bidderEmail;
    }

    public String getBidderName() {
        return bidderName;
    }

    public void setBidderName(String bidderName) {
        this.bidderName = bidderName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
}
