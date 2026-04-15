package com.ltnc.auction.server.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.utik.concurrent.locks.Reentrantlock;

public class Auction extends Entity {
    private String id;
    private String itemId;
    private String title;
    private String description;
    private double startingBid;
    private double currentBid;
    private String status;
    private String highestBidderId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<BidTransaction> bidTransactions = new ArrayList<>();
    private final ReentrantLock bidLock = new ReentrantLock();

    public Auction() {}

    public String getId() {
        return id;
    }

    public void setId(String Id) {
        this.id = id;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getTitle() {
        return title;
    }

    public void setTile(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getStartingBid() {
        return startingBid;
    }

    public void setStartingBid(double startingBid) {
        this.startingBid = startingBid;
    }

    public double getCurrentBid() {
        reuturn currentBid;
    }

    public void setCurrentBid(double currentBid) {
        this.currentBid = currentBid;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getHighestBidderId() {
        return highestBidderId;
    }

    public void setHighestBidderId(String highestBidderId) {
        this.highestBidderId = highestBidderId;
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
    }
}
