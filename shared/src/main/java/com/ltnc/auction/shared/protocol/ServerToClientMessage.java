package com.ltnc.auction.shared.protocol;

import com.ltnc.auction.shared.dto.AuctionDto;
import com.ltnc.auction.shared.dto.BidDto;
import com.ltnc.auction.shared.dto.ItemDto;
import com.ltnc.auction.shared.dto.UserDto;
import com.ltnc.auction.shared.dto.WalletDto;
import com.ltnc.auction.shared.dto.WalletTransactionDto;
import java.util.List;

public class ServerToClientMessage {
    public MessageType type;
    public boolean success;
    public String error;
    // auth
    public UserDto user;
    // lists
    public List<AuctionDto> auctions;
    public List<ItemDto> items;
    public List<UserDto> users;
    public List<BidDto> bids;
    public List<BidDto> newBids; // Delta update
    public List<WalletTransactionDto> walletTransactions;
    // single objects
    public AuctionDto auction;
    public ItemDto item;
    public WalletDto wallet;
    /** when BID_RESULT fails with insufficient funds */
    public Double requiredTopUp;

    // broadcast fields (AUCTION_UPDATE / WALLET_UPDATE / AUCTION_STATE_CHANGE)
    public String eventType;
    public Long auctionId;
    public Long itemId;
    public Double currentBid;
    public String status;
    public String winnerEmail;
    public String newEndTime; // ISO-8601
    public Long userId;
    public Double balance;
    public Double reserved;
    public Double available;
    public Long serverCurrentTimeMs;
    /** true when anti-sniping extended end time */
    public Boolean auctionExtended;
    // notification
    public List<com.ltnc.auction.shared.dto.NotificationDto> notifications;
    public com.ltnc.auction.shared.dto.NotificationDto notification;
}
