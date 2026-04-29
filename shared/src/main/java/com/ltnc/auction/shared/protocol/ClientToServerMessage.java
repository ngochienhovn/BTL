package com.ltnc.auction.shared.protocol;

import java.math.BigDecimal;

public class ClientToServerMessage {
    // Type of message
    public MessageType type;
    // Fields for LOGIN message
    public String email;
    public String fullName;
    public String password;
    public String role;
    public Long auctionId;     // id phiên đấu giá
    public Double bidAmount;   // số tiền đặt giá
    public Double amount;      // số tiền nạp / rút
    // Fields for CREATE_ITEM message
    public Long userId;
    public Long itemId;
    public Long sellerId;
    public String sellerEmail;
    public String typeOfItem;
    public String itemName;
    public String itemDescription;
    public BigDecimal itemStartingBid;
    public String imageUrl;
}

{"MessageType":"LOGIN","email":"user@example.com","password":"password","fullName":"John Doe","role":"USER"}
{"MessageType":"CREATE_ITEM","userId":1,"itemId":null,"sellerId":1,"sellerEmail":"
