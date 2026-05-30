package com.ltnc.auction.shared.protocol;

public class ClientToServerMessage {
    public MessageType type;
    // auth
    public String fullName;
    public String email;
    public String password;
    public String role; // "BIDDER","SELLER","ADMIN"
    // auction/bid
    public Long auctionId;
    public Double bidAmount;
    /** deposit / withdraw amount */
    public Double amount;
    // auto-bid
    public Double maxBid;
    public Double increment;
    // item
    public Long itemId;
    public String itemType; // "Electronics","Art","Vehicle"
    public String itemName;
    public String itemDescription;
    public Double itemStartingBid;
    public String itemImageUrl;
    public String sellerEmail;
    // create auction
    public Integer durationMinutes;
    public String startTime;
    public String endTime;
    // update auction
    public String title;
    public String description;
    public Double startingBid;
    public String status;
    // admin
    public Long userId;
    public String newPassword;
    public String newRole;
    public Integer page;
    public Integer limit;
    // notification
    public Long notificationId;
}
