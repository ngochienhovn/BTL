package app.model;

import java.time.LocalDateTime;

public class Bid {
    private final String id;
    private final String bidder;
    private final String bidderEmail;
    private final double amount;
    private final LocalDateTime timestamp;

    public Bid(String id, String bidder, String bidderEmail, double amount, LocalDateTime timestamp) {
        this.id = id;
        this.bidder = bidder;
        this.bidderEmail = bidderEmail;
        this.amount = amount;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public String getBidder() {
        return bidder;
    }

    public String getBidderEmail() {
        return bidderEmail;
    }

    public double getAmount() {
        return amount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
