package com.ltnc.auction.shared.dto;

public class BidDto {
    public Long id;
    public String bidderName;
    public String bidderEmail;
    public double amount;
    public String timestamp; // ISO-8601

    public BidDto() {}
}
