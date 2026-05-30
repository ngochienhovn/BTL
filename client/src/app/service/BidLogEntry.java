package app.service;

import java.time.LocalDateTime;

public record BidLogEntry(
        String auctionId,
        String auctionTitle,
        String bidder,
        double amount,
        LocalDateTime timestamp) {}
