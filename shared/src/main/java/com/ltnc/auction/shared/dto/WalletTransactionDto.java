package com.ltnc.auction.shared.dto;

public class WalletTransactionDto {
    public Long id;
    public Long userId;
    public String type; // DEPOSIT, WITHDRAW, RESERVE, RELEASE
    public double amount;
    public Long refAuctionId;
    public String refAuctionTitle;
    public String createdAt;

    public WalletTransactionDto() {}
}
