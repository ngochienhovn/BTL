package com.ltnc.auction.server.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

<<<<<<< HEAD
public class BidTransaction extends Entity {
=======
public class BidTransaction {
    private Long id;
>>>>>>> a4a9980ce3593461ea601ec5d280f231fec24645
    private Long auctionId;
    private Long bidderId;
    private String bidderEmail;
    private String bidderName;
    private BigDecimal amount;
    private LocalDateTime createdAt;

    public BidTransaction() {}

    public BidTransaction(Long auctionId, Long bidderId, String bidderEmail, String bidderName, BigDecimal amount) {
<<<<<<< HEAD
=======
        this(auctionId, bidderId, bidderEmail, bidderName, amount, LocalDateTime.now());
    }

    public BidTransaction(Long auctionId, Long bidderId, String bidderEmail, String bidderName, BigDecimal amount, LocalDateTime createdAt) {
>>>>>>> a4a9980ce3593461ea601ec5d280f231fec24645
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.bidderEmail = bidderEmail;
        this.bidderName = bidderName;
        this.amount = amount;
<<<<<<< HEAD
        this.createdAt = LocalDateTime.now();
    }

    public Long getAuctionId() { return auctionId; }
    public Long getBidderId() { return bidderId; }
    public String getBidderEmail() { return bidderEmail; }
    public String getBidderName() { return bidderName; }
    public BigDecimal getAmount() { return amount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime t) { this.createdAt = t; }

    @Override
    public String printInfo() {
        return String.format("Bid[auction=%d, bidder=%s, amount=%s]", auctionId, bidderEmail, amount);
=======
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(Long auctionId) {
        this.auctionId = auctionId;
    }

    public Long getBidderId() {
        return bidderId;
    }

    public void setBidderId(Long bidderId) {
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
>>>>>>> a4a9980ce3593461ea601ec5d280f231fec24645
    }
}
