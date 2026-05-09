package com.ltnc.auction.server.model;

import java.math.BigDecimal;

/**
 * Model lưu cấu hình Auto-Bid của người dùng.
 *
 * Ví dụ:
 * User 3 tham gia auction 1.
 * User muốn bot tự động đấu giá tối đa 2.000.000,
 * mỗi lần tăng 50.000.
 */
public class AutoBidConfig {
    private Long id;
    private Long auctionId;
    private Long userId;
    private BigDecimal maxBid;
    private BigDecimal increment;

    public AutoBidConfig() {
    }

    public AutoBidConfig(Long id, Long auctionId, Long userId, BigDecimal maxBid, BigDecimal increment) {
        this.id = id;
        this.auctionId = auctionId;
        this.userId = userId;
        this.maxBid = maxBid;
        this.increment = increment;
    }

    public Long getId() {
        return id;
    }

    public Long getAuctionId() {
        return auctionId;
    }

    public Long getUserId() {
        return userId;
    }

    public BigDecimal getMaxBid() {
        return maxBid;
    }

    public BigDecimal getIncrement() {
        return increment;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setAuctionId(Long auctionId) {
        this.auctionId = auctionId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setMaxBid(BigDecimal maxBid) {
        this.maxBid = maxBid;
    }

    public void setIncrement(BigDecimal increment) {
        this.increment = increment;
    }

    @Override
    public String toString() {
        return "AutoBidConfig{" +
                "id=" + id +
                ", auctionId=" + auctionId +
                ", userId=" + userId +
                ", maxBid=" + maxBid +
                ", increment=" + increment +
                '}';
    }
}