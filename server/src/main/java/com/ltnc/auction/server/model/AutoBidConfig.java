package com.ltnc.auction.server.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AutoBidConfig extends Entity {
    private Long auctionId;
    private Long bidderId;
    private String bidderEmail;
    private BigDecimal maxBid;
    private BigDecimal increment;
    private LocalDateTime registeredAt;

    public AutoBidConfig() {}

    public AutoBidConfig(Long auctionId, Long bidderId, String bidderEmail, BigDecimal maxBid, BigDecimal increment) {
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.bidderEmail = bidderEmail;
        this.maxBid = maxBid;
        this.increment = increment;
        this.registeredAt = LocalDateTime.now();
    }

    public Long getAuctionId() { return auctionId; }
    public Long getBidderId() { return bidderId; }
    public String getBidderEmail() { return bidderEmail; }
    public BigDecimal getMaxBid() { return maxBid; }
    public BigDecimal getIncrement() { return increment; }
    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(LocalDateTime t) { this.registeredAt = t; }

    @Override
    public String printInfo() {
        return String.format("AutoBid[auction=%d, bidder=%s, max=%s]", auctionId, bidderEmail, maxBid);
    }
}
