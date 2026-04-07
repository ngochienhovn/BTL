package com.ltnc.auction.shared.protocol;

import java.math.BigDecimal;

/**
 * Format message kiểu "line-delimited JSON" (mỗi JSON 1 dòng).
 * Các field tối thiểu cho Sprint 1; về sau bổ sung cho bid/state machine.
 */
public class ClientToServerMessage {
  public MessageType type;
  public Long auctionId;
  public Long userId;
  public BigDecimal bidAmount;
}

