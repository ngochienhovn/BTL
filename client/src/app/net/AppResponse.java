package app.net;

import com.ltnc.auction.shared.dto.WalletDto;
import com.ltnc.auction.shared.dto.WalletTransactionDto;
import java.util.List;

public class AppResponse {
    public AppMessageType type;
    public boolean success;
    public String error;
    public AppUserInfo user;
    public List<AppAuctionInfo> auctions;
    public List<AppItemInfo> items;
    public List<AppUserInfo> users;
    public List<AppBidInfo> bids;
    public List<AppBidInfo> newBids; // Delta update
    public List<WalletTransactionDto> walletTransactions;
    public AppAuctionInfo auction;
    public AppItemInfo item;
    public WalletDto wallet;
    public Double requiredTopUp;
    // broadcast fields
    public String eventType;
    public Long userId;
    public Long auctionId;
    public Long itemId;
    public Double currentBid;
    public String status;
    public String winnerEmail;
    public String newEndTime;
    public Double balance;
    public Double reserved;
    public Double available;
    public Long serverCurrentTimeMs;
    /** Anti-sniping đã gia hạn */
    public Boolean auctionExtended;
}
