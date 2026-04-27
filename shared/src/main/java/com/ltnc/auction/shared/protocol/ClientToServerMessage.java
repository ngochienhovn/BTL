package com.ltnc.auction.shared.protocol;

import java.math.BigDecimal;

/**
 * Format message kiểu "line-delimited JSON" (mỗi JSON 1 dòng).
 * Các field tối thiểu cho Sprint 1; về sau bổ sung cho bid/state machine.
 */
public class ClientToServerMessage {
<<<<<<< Updated upstream
  public MessageType type;
  public Long auctionId;
  public Long userId;
  public BigDecimal bidAmount;
=======
    // Type of message
    public MessageType type;
    // Fields for LOGIN message
    public String result;
    public String eventType;   
    public String email;
    public String fullName;
    public String password;
    public String role;
    public Long auctionId;     // id phiên đấu giá
    public Double bidAmount;   // số tiền đặt giá
    public Double amount;      // số tiền nạp / rút
    public Double currentBid;
    public Long highestBidderId;
    public String auctionStatus;
    // Fields for CREATE_ITEM message
    public Long userId;
    public Double balance;
    public Double reserved;
    public Double available;
    public Long serverCurrentTimeMs;
    public Long itemId;
    public Long sellerId;
    public String sellerEmail;
    public String typeOfItem;
    public String itemName;
    public String itemDescription;
    public BigDecimal itemStartingBid;
    public String imageUrl;


>>>>>>> Stashed changes
}

