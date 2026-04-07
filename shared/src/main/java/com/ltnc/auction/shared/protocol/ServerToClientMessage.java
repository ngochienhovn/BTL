package com.ltnc.auction.shared.protocol;

import com.ltnc.auction.shared.AuctionState;
import java.math.BigDecimal;

/**
 * Format message kiểu "line-delimited JSON" (mỗi JSON 1 dòng).
 */
public class ServerToClientMessage {
  public MessageType type;
  public String error;

  // realtime fields (sẽ dùng dần ở các sprint sau)
  public Long auctionId;
  public AuctionState auctionState;
  public BigDecimal currentPrice;
  public Long highestBidderId;
}

