package com.ltnc.auction.server.protocol;

public class ClientToServerMessage 
{
    public MessageType type;
    // Auth
    public String email;
    public String password;
    // Auction/Bid
    public Long auctionId;
    public Integer bidAmount;
    // Create auction
    public String itemType;
    public String itemName;
    public String itemDescription;
    public Integer itemStartingBid;
    public String URLImage;
}