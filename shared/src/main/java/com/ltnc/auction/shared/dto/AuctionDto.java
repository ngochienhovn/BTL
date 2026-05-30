package com.ltnc.auction.shared.dto;

import java.util.List;

public class AuctionDto {
    public Long id;
    public Long itemId;
    public String title;
    public String description;
    public String category;
    public double startingBid;
    public double currentBid;
    public String startTime; // ISO-8601
    public String endTime;   // ISO-8601
    public String status;    // OPEN,RUNNING,FINISHED,PAID,CANCELED
    public String sellerEmail;
    public String winnerEmail;
    public String imageUrl;
    public List<BidDto> bids;

    public AuctionDto() {}
}
