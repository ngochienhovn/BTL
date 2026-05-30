package app.service;

import app.MainApp;
import app.model.Auction;
import app.model.AuctionStatus;
import app.model.Bid;
import app.model.Item;
import app.model.User;
import com.ltnc.auction.shared.dto.AuctionDto;
import com.ltnc.auction.shared.dto.BidDto;
import com.ltnc.auction.shared.protocol.MessageType;
import com.ltnc.auction.shared.protocol.ClientToServerMessage;
import com.ltnc.auction.shared.protocol.ServerToClientMessage;
import app.net.ServerMessageListener;
import app.net.SocketClient;
import com.ltnc.auction.shared.dto.WalletDto;
import com.ltnc.auction.shared.dto.WalletTransactionDto;
import javafx.application.Platform;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * ============================================================
 *  TẦNG SERVICE / MODEL (Client) – CẦU NỐI GIỮA NETWORK VÀ UI
 *  Phụ trách: Thành viên Network (TV3)
 * ============================================================
 *
 *  NetworkAuctionService là lớp Model trong MVC phía Client:
 *  - Giao tiếp với server qua {@link SocketClient} (gửi request, nhận response).
 *  - Cache dữ liệu auction tại client để UI không cần hỏi server liên tục.
 *  - Lắng nghe broadcast AUCTION_UPDATE → cập nhật cache → thông báo UI.
 *
 *  Pattern: Singleton + Observer
 *  - Singleton: một instance duy nhất quản lý toàn bộ dữ liệu auction ở client.
 *  - Observer: Controller đăng ký là Observer, khi dữ liệu thay đổi
 *    → Service gọi observer.onAuctionsUpdated() → UI tự cập nhật.
 *
 *  Sơ đồ luồng khi nhận broadcast:
 *
 *    [Server] ──AUCTION_UPDATE──► [SocketClient.Reader Thread]
 *                                       │
 *                                       ▼
 *                               handleBroadcast()
 *                                       │ cập nhật localAuctions
 *                                       ▼
 *                               notifyObservers()
 *                                       │
 *                                       ▼
 *                          [HomePageController / AuctionDetailController]
 *                               Platform.runLater() → cập nhật UI
 */
public class NetworkAuctionService implements IAuctionService {
    private static final NetworkAuctionService INSTANCE = new NetworkAuctionService();

    /**
     * Cache danh sách auction tại client.
     * CopyOnWriteArrayList cho phép đọc an toàn từ nhiều thread
     * (Reader Thread ghi, UI Thread đọc).
     */
    private final List<Auction> localAuctions = new CopyOnWriteArrayList<>();

    /**
     * Danh sách Observer (thường là các Controller).
     * Khi dữ liệu thay đổi → gọi onAuctionsUpdated() trên mỗi Observer.
     */
    private final List<AuctionObserver> observers = new CopyOnWriteArrayList<>();

    /** Cache ví người đăng nhập (đồng bộ qua WALLET_UPDATE / GET_WALLET). */
    private volatile WalletDto cachedWallet;
    /** Độ lệch: serverMillis - clientMillis tại thời điểm nhận push gần nhất. */
    private volatile long serverClockOffsetMs;
    private volatile Double lastBidRequiredTopUp;
    /** Thông báo bot auto-bid thiếu tiền (hiển thị UI). */
    private volatile String autoBidWalletNotice;

    private NetworkAuctionService() {
        SocketClient.getInstance().addBroadcastListener(ServerMessageListener.forAuctionService(this));
    }

    public static NetworkAuctionService getInstance() { return INSTANCE; }

    @Override
    public void initialize(List<Auction> initialAuctions) {
        // No-op: server manages its own data
    }

    /** Lấy bản cache theo id (không gọi mạng). */
    public Auction findCachedAuction(String id) {
        if (id == null) return null;
        for (Auction a : localAuctions) {
            if (id.equals(a.getId())) {
                return a;
            }
        }
        return null;
    }

    @Override
    public List<Auction> getAuctions() {
        ClientToServerMessage req = new ClientToServerMessage();
        req.type = MessageType.GET_AUCTIONS;
        ServerToClientMessage resp = SocketClient.getInstance().sendAndReceive(req);
        if (resp == null || !resp.success || resp.auctions == null) return getCachedAuctions();

        syncClock(resp.serverCurrentTimeMs);

        List<Auction> fetched = new ArrayList<>();
        for (AuctionDto info : resp.auctions) {
            fetched.add(mapAuction(info));
        }
        localAuctions.clear();
        localAuctions.addAll(fetched);
        return getCachedAuctions();
    }

    @Override
    public List<Auction> getAuctions(int page, int limit) {
        ClientToServerMessage req = new ClientToServerMessage();
        req.type = MessageType.GET_AUCTIONS;
        req.page = page;
        req.limit = limit;
        ServerToClientMessage resp = SocketClient.getInstance().sendAndReceive(req);
        if (resp == null || !resp.success || resp.auctions == null) return new ArrayList<>();

        syncClock(resp.serverCurrentTimeMs);

        List<Auction> fetched = new ArrayList<>();
        for (AuctionDto info : resp.auctions) {
            fetched.add(mapAuction(info));
        }

        // Merge newly fetched auctions into localAuctions cache
        for (Auction newAuc : fetched) {
            boolean found = false;
            for (int i = 0; i < localAuctions.size(); i++) {
                if (localAuctions.get(i).getId().equals(newAuc.getId())) {
                    localAuctions.set(i, newAuc);
                    found = true;
                    break;
                }
            }
            if (!found) {
                localAuctions.add(newAuc);
            }
        }
        return fetched;
    }


    @Override
    public List<Auction> getCachedAuctions() {
        return new ArrayList<>(localAuctions);
    }

    @Override
    public Auction createAuctionFromItem(Item item, LocalDateTime startTime, LocalDateTime endTime) {
        if (item == null) return null;
        long durationMinutes = 120;
        if (startTime != null && endTime != null) {
            durationMinutes = java.time.Duration.between(startTime, endTime).toMinutes();
        }
        ClientToServerMessage req = new ClientToServerMessage();
        req.type = MessageType.CREATE_AUCTION;
        try {
            req.itemId = Long.parseLong(item.getId());
        } catch (NumberFormatException e) {
            return null;
        }
        req.sellerEmail = item.getSellerEmail();
        req.durationMinutes = (int) durationMinutes;
        
        java.time.format.DateTimeFormatter iso = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        if (startTime != null) {
            req.startTime = startTime.format(iso);
        }
        if (endTime != null) {
            req.endTime = endTime.format(iso);
        }

        ServerToClientMessage resp = SocketClient.getInstance().sendAndReceive(req);
        if (resp == null || !resp.success || resp.auction == null) return null;
        Auction auction = mapAuction(resp.auction);
        localAuctions.add(auction);
        notifyObservers();
        return auction;
    }

    @Override
    public void registerObserver(AuctionObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
        }
    }

    @Override
    public void unregisterObserver(AuctionObserver observer) {
        observers.remove(observer);
    }

    @Override
    public BidResult placeBid(User user, String auctionId, double amount) {
        if (user == null) return BidResult.NOT_LOGGED_IN;

        ClientToServerMessage req = new ClientToServerMessage();
        req.type = MessageType.PLACE_BID;
        try {
            req.auctionId = Long.parseLong(auctionId);
        } catch (NumberFormatException e) {
            return BidResult.INVALID_AUCTION;
        }
        req.email = user.getEmail();
        req.bidAmount = amount;

        ServerToClientMessage resp = SocketClient.getInstance().sendAndReceive(req);
        if (resp == null) return BidResult.AUCTION_CLOSED;
        if (!resp.success) {
            if (resp.error == null) return BidResult.AUCTION_CLOSED;
            return switch (resp.error) {
                case "BID_TOO_LOW" -> BidResult.BID_TOO_LOW;
                case "INVALID_AMOUNT" -> BidResult.INVALID_AMOUNT;
                case "AUCTION_NOT_FOUND" -> BidResult.INVALID_AUCTION;
                case "FORBIDDEN_SELF_BID" -> BidResult.FORBIDDEN_SELF_BID;
                case "INSUFFICIENT_FUNDS" -> {
                    lastBidRequiredTopUp = resp.requiredTopUp;
                    yield BidResult.INSUFFICIENT_FUNDS;
                }
                default -> BidResult.AUCTION_CLOSED;
            };
        }
        // Update local auction if returned
        if (resp.auction != null) {
            updateLocalAuction(resp.auction);
        }
        notifyObservers();
        return BidResult.OK;
    }

    @Override
    public boolean registerAutoBid(String auctionId, String bidderEmail, double maxBid, double increment) {
        ClientToServerMessage req = new ClientToServerMessage();
        req.type = MessageType.REGISTER_AUTO_BID;
        try {
            req.auctionId = Long.parseLong(auctionId);
        } catch (NumberFormatException e) {
            return false;
        }
        req.email = bidderEmail;
        req.maxBid = maxBid;
        req.increment = increment;
        ServerToClientMessage resp = SocketClient.getInstance().sendAndReceive(req);
        return resp != null && resp.success;
    }

    @Override
    public List<WalletTransactionDto> getWalletTransactions(User user) {
        if (user == null) return new ArrayList<>();
        ClientToServerMessage req = new ClientToServerMessage();
        req.type = MessageType.GET_WALLET_TRANSACTIONS;
        req.email = user.getEmail();
        ServerToClientMessage resp = SocketClient.getInstance().sendAndReceive(req);
        if (resp != null && resp.success && resp.walletTransactions != null) {
            return resp.walletTransactions;
        }
        return new ArrayList<>();
    }

    @Override
    public void tick() {
        // No-op: server handles timing
    }

    @Override
    public List<Auction> getAdminViewAuctions() {
        getAuctions(); // Force fetch from server to get latest updates
        return localAuctions.stream()
                .sorted(Comparator.comparing(Auction::getEndTime,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    @Override
    public List<BidLogEntry> getBidLogs() {
        ClientToServerMessage req = new ClientToServerMessage();
        req.type = MessageType.GET_BID_LOGS;
        ServerToClientMessage resp = SocketClient.getInstance().sendAndReceive(req);
        if (resp == null || !resp.success || resp.bids == null) return new ArrayList<>();

        List<BidLogEntry> logs = new ArrayList<>();
        for (BidDto bid : resp.bids) {
            LocalDateTime ts = parseDateTime(bid.timestamp);
            logs.add(new BidLogEntry("", "", bid.bidderName != null ? bid.bidderName : bid.bidderEmail,
                    bid.amount, ts != null ? ts : LocalDateTime.now()));
        }
        return logs;
    }

    @Override
    public boolean deleteAuction(String auctionId) {
        if (auctionId == null) return false;
        ClientToServerMessage req = new ClientToServerMessage();
        req.type = MessageType.DELETE_AUCTION;
        try {
            req.auctionId = Long.parseLong(auctionId);
        } catch (NumberFormatException e) {
            return false;
        }
        ServerToClientMessage resp = SocketClient.getInstance().sendAndReceive(req);
        if (resp != null && resp.success) {
            localAuctions.removeIf(a -> auctionId.equals(a.getId()));
        }
        notifyObservers();
        return resp != null && resp.success;
    }

    @Override
    public boolean cancelAuction(String auctionId) {
        if (auctionId == null) return false;
        ClientToServerMessage req = new ClientToServerMessage();
        req.type = MessageType.CANCEL_AUCTION;
        try {
            req.auctionId = Long.parseLong(auctionId);
        } catch (NumberFormatException e) {
            return false;
        }
        ServerToClientMessage resp = SocketClient.getInstance().sendAndReceive(req);
        notifyObservers();
        return resp != null && resp.success;
    }

    @Override
    public boolean updateAuction(String auctionId, String title, String description, double startingBid, LocalDateTime endTime, String status) {
        if (auctionId == null) return false;
        ClientToServerMessage req = new ClientToServerMessage();
        req.type = MessageType.UPDATE_AUCTION;
        try {
            req.auctionId = Long.parseLong(auctionId);
        } catch (NumberFormatException e) {
            return false;
        }
        req.title = title;
        req.description = description;
        req.startingBid = startingBid;
        if (endTime != null) {
            req.endTime = endTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
        req.status = status;
        ServerToClientMessage resp = SocketClient.getInstance().sendAndReceive(req);
        notifyObservers();
        return resp != null && resp.success;
    }

    @Override
    public boolean confirmPayment(String auctionId) {
        if (auctionId == null) return false;
        ClientToServerMessage req = new ClientToServerMessage();
        req.type = MessageType.CONFIRM_PAYMENT;
        try {
            req.auctionId = Long.parseLong(auctionId);
        } catch (NumberFormatException e) {
            return false;
        }
        ServerToClientMessage resp = SocketClient.getInstance().sendAndReceive(req);
        notifyObservers();
        return resp != null && resp.success;
    }

    /**
     * Push từ server (Reader thread). UI qua {@link #notifyObservers()} + {@link Platform#runLater}.
     */
    public void onBroadcastMessage(ServerToClientMessage resp) {
        if (resp == null || resp.type == null) {
            return;
        }
        switch (resp.type) {
            case AUCTION_CREATED -> applyAuctionCreated(resp);
            case AUCTION_UPDATE -> applyAuctionUpdate(resp);
            case WALLET_UPDATE -> applyWalletUpdate(resp);
            case AUCTION_STATE_CHANGE -> applyAuctionStateChange(resp);
            case AUCTION_DELETED -> applyAuctionDeleted(resp);
            case FORCE_LOGOUT -> handleForceLogout(resp);
            default -> { }
        }
        notifyObservers();
    }

    private void applyAuctionCreated(ServerToClientMessage resp) {
        if (resp.auction == null) return;
        syncClock(resp.serverCurrentTimeMs);
        Auction auction = mapAuction(resp.auction);
        
        // Avoid duplicates
        localAuctions.removeIf(a -> a.getId().equals(auction.getId()));
        localAuctions.add(auction);
    }

    private void applyAuctionDeleted(ServerToClientMessage resp) {
        if (resp.auctionId == null) return;
        String idStr = resp.auctionId.toString();
        localAuctions.removeIf(a -> idStr.equals(a.getId()));
    }

    private void handleForceLogout(ServerToClientMessage resp) {
        Platform.runLater(() -> {
            MainApp.getUserService().logout();
            NotificationService.getInstance().showNotification(
                "Account Security", 
                resp.error != null ? resp.error : "Your account has been deleted.", 
                NotificationService.NotificationType.ERROR
            );
            MainApp.showSignInPage();
        });
    }

    private void applyAuctionUpdate(ServerToClientMessage resp) {
        Long auctionId = resp.auctionId;
        if (auctionId == null) return;
        syncClock(resp.serverCurrentTimeMs);

        String auctionIdStr = auctionId.toString();
        User cu = NetworkUserService.getInstance().getCurrentUser();
        String currentEmail = cu != null ? cu.getEmail() : null;

        for (int i = 0; i < localAuctions.size(); i++) {
            Auction a = localAuctions.get(i);
            if (auctionIdStr.equals(a.getId())) {
                boolean wasLeader = currentEmail != null && currentEmail.equalsIgnoreCase(a.getWinnerBidder());
                
                if (resp.currentBid != null) a.setCurrentBid(resp.currentBid);
                if (resp.status != null) a.setStatus(parseStatus(resp.status));
                if (resp.winnerEmail != null) a.setWinnerBidder(resp.winnerEmail);
                if (resp.newEndTime != null) {
                    LocalDateTime newEnd = parseDateTime(resp.newEndTime);
                    if (newEnd != null) a.setEndTime(newEnd);
                }
                if (resp.bids != null) {
                    List<Bid> bidsList = new ArrayList<>();
                    for (BidDto b : resp.bids) {
                        LocalDateTime ts = parseDateTime(b.timestamp);
                        bidsList.add(new Bid(
                                b.id != null ? b.id.toString() : java.util.UUID.randomUUID().toString(),
                                b.bidderName != null ? b.bidderName : b.bidderEmail,
                                b.bidderEmail,
                                b.amount,
                                ts != null ? ts : LocalDateTime.now()));
                    }
                    a.getBids().clear();
                    a.getBids().addAll(bidsList);
                } else if (resp.newBids != null) {
                    // Delta payload optimization
                    List<Bid> appendedBids = new ArrayList<>();
                    for (BidDto b : resp.newBids) {
                        LocalDateTime ts = parseDateTime(b.timestamp);
                        appendedBids.add(new Bid(
                                b.id != null ? b.id.toString() : java.util.UUID.randomUUID().toString(),
                                b.bidderName != null ? b.bidderName : b.bidderEmail,
                                b.bidderEmail,
                                b.amount,
                                ts != null ? ts : LocalDateTime.now()));
                    }
                    a.getBids().addAll(appendedBids);
                }

                // Outbid notification
                boolean isLeaderNow = currentEmail != null && currentEmail.equalsIgnoreCase(a.getWinnerBidder());
                if (wasLeader && !isLeaderNow) {
                    NotificationService.getInstance().showNotification(
                        "Outbid!", 
                        "You have been outbid on: " + a.getTitle(), 
                        NotificationService.NotificationType.WARNING
                    );
                }
                break;
            }
        }
    }

    private void applyWalletUpdate(ServerToClientMessage resp) {
        syncClock(resp.serverCurrentTimeMs);
        User cu = NetworkUserService.getInstance().getCurrentUser();
        if (cu == null || resp.userId == null) return;
        try {
            long uid = Long.parseLong(cu.getId());
            if (resp.userId != uid) return;
        } catch (NumberFormatException e) {
            return;
        }
        if (resp.wallet != null) {
            cachedWallet = resp.wallet;
        } else if (resp.balance != null) {
            WalletDto w = new WalletDto();
            w.userId = resp.userId;
            w.balance = resp.balance;
            w.reserved = resp.reserved != null ? resp.reserved : 0;
            w.available = resp.available != null ? resp.available : 0;
            cachedWallet = w;
        }
    }

    private void applyAuctionStateChange(ServerToClientMessage resp) {
        syncClock(resp.serverCurrentTimeMs);
        Long auctionId = resp.auctionId;
        if (auctionId == null) return;
        String auctionIdStr = auctionId.toString();
        
        User cu = NetworkUserService.getInstance().getCurrentUser();
        String currentEmail = cu != null ? cu.getEmail() : null;

        for (int i = 0; i < localAuctions.size(); i++) {
            Auction a = localAuctions.get(i);
            if (auctionIdStr.equals(a.getId())) {
                AuctionStatus oldStatus = a.getStatus();
                if (resp.status != null) a.setStatus(parseStatus(resp.status));
                if (resp.newEndTime != null) {
                    LocalDateTime newEnd = parseDateTime(resp.newEndTime);
                    if (newEnd != null) a.setEndTime(newEnd);
                }
                if (resp.auctionExtended != null) {
                    a.setExtendedByAntiSniping(Boolean.TRUE.equals(resp.auctionExtended));
                    if (Boolean.TRUE.equals(resp.auctionExtended)) {
                        NotificationService.getInstance().showNotification(
                            "Auction Extended! ⏱\ufe0f", 
                            "Intense bidding on " + a.getTitle() + " has extended the end time by 1 minute.", 
                            NotificationService.NotificationType.INFO
                        );
                    }
                }

                // Win Celebration
                if (oldStatus != AuctionStatus.FINISHED && a.getStatus() == AuctionStatus.FINISHED) {
                    if (currentEmail != null && currentEmail.equalsIgnoreCase(a.getWinnerBidder())) {
                        NotificationService.getInstance().showNotification(
                            "Congratulations! \uD83C\uDF89", 
                            "You won the auction: " + a.getTitle(), 
                            NotificationService.NotificationType.SUCCESS
                        );
                    }
                }
                break;
            }
        }
    }

    private void syncClock(Long serverCurrentTimeMs) {
        if (serverCurrentTimeMs != null) {
            serverClockOffsetMs = serverCurrentTimeMs - System.currentTimeMillis();
        }
    }

    /** Thời gian hiện tại ước lượng theo đồng hồ server (ms). */
    public long getAdjustedNowMillis() {
        return System.currentTimeMillis() + serverClockOffsetMs;
    }

    public WalletDto getCachedWallet() {
        return cachedWallet;
    }

    public Double consumeLastBidRequiredTopUp() {
        Double v = lastBidRequiredTopUp;
        lastBidRequiredTopUp = null;
        return v;
    }

    public String getAutoBidWalletNotice() {
        return autoBidWalletNotice;
    }

    public void clearAutoBidWalletNotice() {
        autoBidWalletNotice = null;
    }

    /**
     * Lấy snapshot ví từ server (GET_WALLET).
     */
    public WalletDto fetchWallet(User user) {
        if (user == null) return null;
        ClientToServerMessage req = new ClientToServerMessage();
        req.type = MessageType.GET_WALLET;
        req.email = user.getEmail();
        ServerToClientMessage resp = SocketClient.getInstance().sendAndReceive(req);
        if (resp != null && resp.success && resp.wallet != null) {
            cachedWallet = resp.wallet;
            return resp.wallet;
        }
        return cachedWallet;
    }

    public boolean deposit(User user, double amount) {
        if (user == null || amount <= 0) return false;
        ClientToServerMessage req = new ClientToServerMessage();
        req.type = MessageType.DEPOSIT;
        req.email = user.getEmail();
        req.amount = amount;
        ServerToClientMessage resp = SocketClient.getInstance().sendAndReceive(req);
        if (resp != null && resp.success && resp.wallet != null) {
            cachedWallet = resp.wallet;
            return true;
        }
        return false;
    }

    public boolean withdraw(User user, double amount) {
        if (user == null || amount <= 0) return false;
        ClientToServerMessage req = new ClientToServerMessage();
        req.type = MessageType.WITHDRAW;
        req.email = user.getEmail();
        req.amount = amount;
        ServerToClientMessage resp = SocketClient.getInstance().sendAndReceive(req);
        if (resp != null && resp.success && resp.wallet != null) {
            cachedWallet = resp.wallet;
            return true;
        }
        return false;
    }

    /**
     * Thông báo tất cả Observer rằng dữ liệu đã thay đổi.
     * Observer (thường là Controller) có trách nhiệm gọi Platform.runLater()
     * trước khi sửa UI component, vì method này được gọi từ Reader Thread.
     */
    private void notifyObservers() {
        Platform.runLater(() -> {
            for (AuctionObserver observer : observers) {
                try {
                    observer.onAuctionsUpdated();
                } catch (Exception ignored) { }
            }
        });
    }

    private void updateLocalAuction(AuctionDto info) {
        if (info == null || info.id == null) return;
        String idStr = info.id.toString();
        for (int i = 0; i < localAuctions.size(); i++) {
            if (idStr.equals(localAuctions.get(i).getId())) {
                localAuctions.set(i, mapAuction(info));
                return;
            }
        }
    }

    private Auction mapAuction(AuctionDto info) {
        if (info == null) return null;
        String id = info.id != null ? info.id.toString() : UUID.randomUUID().toString();
        LocalDateTime startTime = parseDateTime(info.startTime);
        LocalDateTime endTime = parseDateTime(info.endTime);

        List<Bid> bids = new ArrayList<>();
        if (info.bids != null) {
            for (BidDto b : info.bids) {
                LocalDateTime ts = parseDateTime(b.timestamp);
                bids.add(new Bid(
                        b.id != null ? b.id.toString() : UUID.randomUUID().toString(),
                        b.bidderName != null ? b.bidderName : b.bidderEmail,
                        b.bidderEmail,
                        b.amount,
                        ts != null ? ts : LocalDateTime.now()));
            }
        }

        Auction auction = new Auction(
                id,
                info.itemId != null ? info.itemId.toString() : "",
                info.title,
                info.description,
                info.category,
                info.startingBid,
                info.currentBid,
                startTime != null ? startTime : LocalDateTime.now(),
                endTime != null ? endTime : LocalDateTime.now().plusHours(2),
                info.imageUrl,
                info.sellerEmail,
                bids);
        auction.setStatus(parseStatus(info.status));
        auction.setWinnerBidder(info.winnerEmail);
        return auction;
    }

    private LocalDateTime parseDateTime(String dt) {
        if (dt == null || dt.isBlank()) return null;
        try {
            return LocalDateTime.parse(dt, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private AuctionStatus parseStatus(String status) {
        if (status == null) return AuctionStatus.OPEN;
        return switch (status) {
            case "RUNNING" -> AuctionStatus.RUNNING;
            case "FINISHED" -> AuctionStatus.FINISHED;
            case "PAID" -> AuctionStatus.PAID;
            case "CANCELED" -> AuctionStatus.CANCELED;
            default -> AuctionStatus.OPEN;
        };
    }
}
