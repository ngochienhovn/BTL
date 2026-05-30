package app.service;

import app.model.Auction;
import app.model.Item;
import app.model.User;
import com.ltnc.auction.shared.dto.WalletTransactionDto;
import java.time.LocalDateTime;
import java.util.List;

public interface IAuctionService {
    void initialize(List<Auction> initialAuctions);
    List<Auction> getAuctions();
    List<Auction> getAuctions(int page, int limit);
    List<Auction> getCachedAuctions();
    Auction createAuctionFromItem(Item item, LocalDateTime startTime, LocalDateTime endTime);
    void registerObserver(AuctionObserver observer);
    void unregisterObserver(AuctionObserver observer);
    BidResult placeBid(User user, String auctionId, double amount);
    boolean registerAutoBid(String auctionId, String bidderEmail, double maxBid, double increment);
    List<WalletTransactionDto> getWalletTransactions(User user);
    void tick();
    List<Auction> getAdminViewAuctions();
    List<BidLogEntry> getBidLogs();
    boolean deleteAuction(String auctionId);
    boolean cancelAuction(String auctionId);
    boolean updateAuction(String auctionId, String title, String description, double startingBid, LocalDateTime endTime, String status);
    boolean confirmPayment(String auctionId);
}
