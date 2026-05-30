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

    public BidTransaction() {}

    public BidTransaction(Long auctionId, Long bidderId, String bidderEmail, String bidderName, BigDecimal amount) {
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.bidderEmail = bidderEmail;
        this.bidderName = bidderName;
        this.amount = amount;
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
    }
}
