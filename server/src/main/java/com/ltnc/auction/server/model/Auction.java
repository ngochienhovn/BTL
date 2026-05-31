package com.ltnc.auction.server.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

<<<<<<< HEAD
public class Auction extends Entity {
    private Long itemId;
    private String title;
    private String description;
    private String category;
    private BigDecimal startingBid;
    private BigDecimal currentBid;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status; // "OPEN","RUNNING","FINISHED","PAID","CANCELED"
    private Long highestBidderId;
    private String winnerEmail;
    private String sellerEmail;
    private String imageUrl;
    private List<BidTransaction> bids = new ArrayList<>();
    private final transient ReentrantLock bidLock = new ReentrantLock();

    public Auction() {}

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public BigDecimal getStartingBid() { return startingBid; }
    public void setStartingBid(BigDecimal startingBid) { this.startingBid = startingBid; }
    public BigDecimal getCurrentBid() { return currentBid; }
    public void setCurrentBid(BigDecimal currentBid) { this.currentBid = currentBid; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getHighestBidderId() { return highestBidderId; }
    public void setHighestBidderId(Long highestBidderId) { this.highestBidderId = highestBidderId; }
    public String getWinnerEmail() { return winnerEmail; }
    public void setWinnerEmail(String winnerEmail) { this.winnerEmail = winnerEmail; }
    public String getSellerEmail() { return sellerEmail; }
    public void setSellerEmail(String sellerEmail) { this.sellerEmail = sellerEmail; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public List<BidTransaction> getBids() { return bids; }
    public void setBids(List<BidTransaction> bids) { this.bids = bids; }
    public ReentrantLock getBidLock() { return bidLock; }

    @Override
    public String printInfo() {
        return String.format("Auction[id=%d, title=%s, status=%s, currentBid=%s]", id, title, status, currentBid);
=======
public class Auction {
    private Long id;
    private Long itemId;
    private String title;
    private String description;
    private BigDecimal startingBid;
    private BigDecimal currentBid;
    private String status;
    private Long highestBidderId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<BidTransaction> bidTransactions = new ArrayList<>();
    private final ReentrantLock bidLock = new ReentrantLock();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getStartingBid() {
        return startingBid;
    }

    public void setStartingBid(BigDecimal startingBid) {
        this.startingBid = startingBid;
    }

    public BigDecimal getCurrentBid() {
        return currentBid;
    }

    public void setCurrentBid(BigDecimal currentBid) {
        this.currentBid = currentBid;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getHighestBidderId() {
        return highestBidderId;
    }

    public void setHighestBidderId(Long highestBidderId) {
        this.highestBidderId = highestBidderId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public List<BidTransaction> getBidTransactions() {
        return bidTransactions;
    }

    public void setBidTransactions(List<BidTransaction> bidTransactions) {
        this.bidTransactions = bidTransactions;
    }

    public ReentrantLock getBidLock() {
        return bidLock;
>>>>>>> a4a9980ce3593461ea601ec5d280f231fec24645
    }
}
