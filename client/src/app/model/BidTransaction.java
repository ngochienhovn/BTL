package app.model;

import java.time.LocalDateTime;

public class BidTransaction extends Entity {
    private final String auctionId;
    private final String bidderEmail;
    private final double amount;
    private final LocalDateTime createdAt;

    public BidTransaction(String id, String auctionId, String bidderEmail, double amount, LocalDateTime createdAt) {
        super(id);
        this.auctionId = auctionId;
        this.bidderEmail = bidderEmail;
        this.amount = amount;
        this.createdAt = createdAt;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public String getBidderEmail() {
        return bidderEmail;
    }

    public double getAmount() {
        return amount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
