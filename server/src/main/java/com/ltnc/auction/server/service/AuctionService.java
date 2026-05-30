package com.ltnc.auction.server.service;

import com.ltnc.auction.server.dao.AuctionDAO;
import com.ltnc.auction.server.dao.AutoBidDAO;
import com.ltnc.auction.server.dao.BidDAO;
import com.ltnc.auction.server.dao.ItemDAO;
import com.ltnc.auction.server.dao.UserDAO;
import com.ltnc.auction.server.model.Auction;
import com.ltnc.auction.server.model.AutoBidConfig;
import com.ltnc.auction.server.model.BidTransaction;
import com.ltnc.auction.server.model.Item;
import com.ltnc.auction.server.model.User;
import com.ltnc.auction.shared.dto.AuctionDto;
import com.ltnc.auction.shared.dto.BidDto;
import com.ltnc.auction.shared.protocol.MessageType;
import com.ltnc.auction.shared.protocol.ServerToClientMessage;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.TimeUnit;

/**
 * ============================================================
 *  REFACTORED AUCTION SERVICE
 *  Orchestrates auction operations by delegating to specialized components.
 * ============================================================
 */
public class AuctionService {
    private static final Logger LOG = LoggerFactory.getLogger(AuctionService.class);

    private static volatile AuctionService instance;

    private final AuctionDAO auctionDAO = new AuctionDAO();
    private final BidDAO bidDAO = new BidDAO();
    private final AutoBidDAO autoBidDAO = new AutoBidDAO();
    private final ItemDAO itemDAO = new ItemDAO();
    private final UserDAO userDAO = new UserDAO();

    // Specialized Processors (Stateless)
    private final BidProcessor bidProcessor = new BidProcessor();

    // In-memory Cache (Repository)
    private final ConcurrentHashMap<Long, Auction> activeAuctions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, PriorityQueue<AutoBidConfig>> autoBidConfigs = new ConcurrentHashMap<>();
    
    // Performance Cache (for non-active or frequently accessed lookups)
    private final Cache<Long, Auction> auctionCache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();

    private final Cache<String, List<AuctionDto>> listCache = Caffeine.newBuilder()
            .expireAfterWrite(30, TimeUnit.SECONDS)
            .maximumSize(10)
            .build();

    private static final Comparator<AutoBidConfig> AUTO_BID_COMPARATOR =
            Comparator.comparing(AutoBidConfig::getMaxBid).reversed()
                    .thenComparing(AutoBidConfig::getRegisteredAt);

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private AuctionService() {}

    public static AuctionService getInstance() {
        if (instance == null) {
            synchronized (AuctionService.class) {
                if (instance == null) {
                    instance = new AuctionService();
                }
            }
        }
        return instance;
    }

    public enum BidResultCode {
        OK, BID_TOO_LOW, AUCTION_CLOSED, AUCTION_NOT_FOUND, INVALID_AMOUNT, INSUFFICIENT_FUNDS, FORBIDDEN_SELF_BID
    }

    public record BidResult(BidResultCode code, AuctionDto auction, Double requiredTopUp) {}

    public void initialize() {
        List<Auction> all = new ArrayList<>();
        all.addAll(auctionDAO.findByStatus("OPEN"));
        all.addAll(auctionDAO.findByStatus("RUNNING"));

        for (Auction auction : all) {
            auction.setBids(bidDAO.findByAuction(auction.getId()));
            activeAuctions.put(auction.getId(), auction);

            List<AutoBidConfig> configs = autoBidDAO.findByAuction(auction.getId());
            if (!configs.isEmpty()) {
                PriorityQueue<AutoBidConfig> pq = new PriorityQueue<>(AUTO_BID_COMPARATOR);
                pq.addAll(configs);
                autoBidConfigs.put(auction.getId(), pq);
            }
        }
        LOG.info("AuctionService initialized with {} active auctions", activeAuctions.size());
    }

    public List<AuctionDto> getAllAuctions() {
        return listCache.get("ALL", key -> 
            auctionDAO.findAll().stream()
                .map(a -> {
                    Auction mem = activeAuctions.get(a.getId());
                    if (mem != null) return AuctionMapper.toDto(mem);
                    if (a.getBids() == null || a.getBids().isEmpty()) {
                        a.setBids(bidDAO.findByAuction(a.getId()));
                    }
                    return AuctionMapper.toDto(a);
                })
                .collect(Collectors.toList())
        );
    }

    public List<AuctionDto> getAuctions(int page, int limit) {
        if (page <= 0) page = 1;
        if (limit <= 0) limit = 20;
        int offset = (page - 1) * limit;
        final int finalOffset = offset;
        final int finalLimit = limit;
        String cacheKey = "PAGE_" + page + "_" + limit;
        return listCache.get(cacheKey, key -> 
            auctionDAO.findAll(finalOffset, finalLimit).stream()
                .map(a -> {
                    Auction mem = activeAuctions.get(a.getId());
                    if (mem != null) return AuctionMapper.toDto(mem);
                    if (a.getBids() == null || a.getBids().isEmpty()) {
                        a.setBids(bidDAO.findByAuction(a.getId()));
                    }
                    return AuctionMapper.toDto(a);
                })
                .collect(Collectors.toList())
        );
    }


    public AuctionDto getAuctionById(Long id) {
        Auction mem = activeAuctions.get(id);
        if (mem != null) return AuctionMapper.toDto(mem);
        
        Auction cached = auctionCache.get(id, key -> {
            Auction a = auctionDAO.findById(key);
            if (a != null) {
                a.setBids(bidDAO.findByAuction(key));
            }
            return a;
        });
        return AuctionMapper.toDto(cached);
    }

    public Auction findCachedAuction(Long id) {
        Auction mem = activeAuctions.get(id);
        if (mem != null) return mem;
        return auctionCache.get(id, auctionDAO::findById);
    }

    public BidResult placeBid(Long auctionId, String bidderEmail, double amount) {
        Auction auction = activeAuctions.get(auctionId);
        if (auction == null) {
            return new BidResult(BidResultCode.AUCTION_NOT_FOUND, null, null);
        }

        auction.getBidLock().lock();
        try {
            if (auction.getSellerEmail() != null && auction.getSellerEmail().equalsIgnoreCase(bidderEmail)) {
                return new BidResult(BidResultCode.FORBIDDEN_SELF_BID, AuctionMapper.toDto(auction), null);
            }
            if (!"RUNNING".equals(auction.getStatus())) {
                return new BidResult(BidResultCode.AUCTION_CLOSED, AuctionMapper.toDto(auction), null);
            }
            if (amount <= 0) {
                return new BidResult(BidResultCode.INVALID_AMOUNT, AuctionMapper.toDto(auction), null);
            }
            BigDecimal newBid = BigDecimal.valueOf(amount).setScale(2, java.math.RoundingMode.HALF_UP);
            double minIncrement = 10.0;
            if (auction.getCurrentBid() != null && newBid.doubleValue() < auction.getCurrentBid().doubleValue() + minIncrement) {
                return new BidResult(BidResultCode.BID_TOO_LOW, AuctionMapper.toDto(auction), null);
            }

            User bidder = userDAO.findByEmail(bidderEmail);
            if (bidder == null || bidder.getId() == null) {
                return new BidResult(BidResultCode.INVALID_AMOUNT, AuctionMapper.toDto(auction), null);
            }
            Long bidderId = bidder.getId();
            String bidderName = bidder.getFullName() != null ? bidder.getFullName() : bidderEmail;

            Long prevLeaderId = auction.getHighestBidderId();
            BigDecimal prevBid =
                    auction.getCurrentBid() != null ? auction.getCurrentBid() : BigDecimal.ZERO;

            Long oldHighestBidderId = auction.getHighestBidderId();
            BigDecimal oldCurrentBid = auction.getCurrentBid();
            List<BidTransaction> oldBids = new ArrayList<>(auction.getBids());
            LocalDateTime oldEndTime = auction.getEndTime();

            try {
                com.ltnc.auction.server.db.DBConnection.startTransaction();
                
                WalletService ws = WalletService.getInstance();
                WalletService.WalletApplyResult wr =
                        ws.applyForBid(bidderId, prevLeaderId, prevBid, newBid, auctionId);
                if (!wr.success()) {
                    double topUp = wr.requiredTopUp() != null ? wr.requiredTopUp().doubleValue() : 0;
                    com.ltnc.auction.server.db.DBConnection.rollbackTransaction();
                    return new BidResult(BidResultCode.INSUFFICIENT_FUNDS, AuctionMapper.toDto(auction), topUp);
                }

                auction.setCurrentBid(newBid);
                auction.setHighestBidderId(bidderId);
                auctionDAO.updateCurrentBid(auctionId, newBid, bidderId);

                BidTransaction bidTx = new BidTransaction(auctionId, bidderId, bidderEmail, bidderName, newBid);
                bidTx.setId(bidDAO.insert(bidTx));
                auction.getBids().add(bidTx);

                boolean extended = bidProcessor.maybeApplyAntiSniping(auction, auctionDAO);
                bidProcessor.processAutoBids(
                        auction, bidderEmail, autoBidConfigs, auctionDAO, bidDAO, WalletService.getInstance(), userDAO);

                com.ltnc.auction.server.db.DBConnection.commitTransaction();

                List<BidTransaction> newBids = auction.getBids().subList(oldBids.size(), auction.getBids().size());
                broadcastAuctionUpdate(auction, newBids);
                if (extended) {
                    broadcastAuctionStateChange(auction, true);
                    auction.getBids().stream().map(com.ltnc.auction.server.model.BidTransaction::getBidderId).distinct().forEach(uid -> {
                        NotificationService.getInstance().createAndSendNotification(
                            uid,
                            "Auction Extended! ⏱️",
                            "Intense bidding on " + auction.getTitle() + " has extended the end time by 1 minute.",
                            "INFO"
                        );
                    });
                }
                if (prevLeaderId != null && !prevLeaderId.equals(bidderId)) {
                    NotificationService.getInstance().createAndSendNotification(
                        prevLeaderId,
                        "Outbid!",
                        "You have been outbid on: " + auction.getTitle(),
                        "WARNING"
                    );
                }
                broadcastWalletUpdatesAfterBid(auction, prevLeaderId, bidderId);

                listCache.invalidateAll();
                return new BidResult(BidResultCode.OK, AuctionMapper.toDto(auction), null);
            } catch (Exception e) {
                try {
                    com.ltnc.auction.server.db.DBConnection.rollbackTransaction();
                } catch (Exception ignored) {}
                
                // Revert in-memory state on failure
                auction.setCurrentBid(oldCurrentBid);
                auction.setHighestBidderId(oldHighestBidderId);
                auction.getBids().clear();
                auction.getBids().addAll(oldBids);
                auction.setEndTime(oldEndTime);
                
                throw new RuntimeException("Error processing bid transaction", e);
            }
        } finally {
            auction.getBidLock().unlock();
        }
    }

    private void broadcastWalletUpdatesAfterBid(Auction auction, Long prevLeaderId, Long manualBidderId) {
        Long winner = auction.getHighestBidderId();
        if (prevLeaderId != null) {
            broadcastWalletUpdate(prevLeaderId);
        }
        if (manualBidderId != null) {
            broadcastWalletUpdate(manualBidderId);
        }
        if (winner != null && !winner.equals(prevLeaderId) && !winner.equals(manualBidderId)) {
            broadcastWalletUpdate(winner);
        }
    }

    private void broadcastWalletUpdate(Long userId) {
        if (userId == null) {
            return;
        }
        var w = WalletService.getInstance().getWallet(userId);
        ServerToClientMessage msg = new ServerToClientMessage();
        msg.type = MessageType.WALLET_UPDATE;
        msg.eventType = "WALLET_UPDATE";
        msg.userId = userId;
        msg.balance = w.getBalance().doubleValue();
        msg.reserved = w.getReserved().doubleValue();
        msg.available = w.getAvailable().doubleValue();
        msg.wallet = AuctionMapper.toWalletDto(w);
        msg.serverCurrentTimeMs = System.currentTimeMillis();
        BroadcastManager.getInstance().broadcastToUser(userId, msg);
    }

    private void broadcastAuctionStateChange(Auction auction, Boolean extended) {
        ServerToClientMessage msg = new ServerToClientMessage();
        msg.type = MessageType.AUCTION_STATE_CHANGE;
        msg.eventType = "AUCTION_STATE_CHANGE";
        msg.auctionId = auction.getId();
        msg.status = auction.getStatus();
        msg.newEndTime = auction.getEndTime() != null ? auction.getEndTime().format(ISO_FORMATTER) : null;
        msg.serverCurrentTimeMs = System.currentTimeMillis();
        msg.auctionExtended = extended;
        BroadcastManager.getInstance().broadcastAll(msg);
    }

    public boolean registerAutoBid(Long auctionId, String bidderEmail, double maxBid, double increment) {
        Auction auction = activeAuctions.get(auctionId);
        if (auction == null) return false;
        
        if (auction.getSellerEmail() != null && auction.getSellerEmail().equalsIgnoreCase(bidderEmail)) {
            return false;
        }

        double currentPrice = auction.getCurrentBid() != null ? auction.getCurrentBid().doubleValue() : auction.getStartingBid().doubleValue();
        if (maxBid <= currentPrice || increment <= 0) {
            return false;
        }

        User bidder = userDAO.findByEmail(bidderEmail);
        Long bidderId = bidder != null ? bidder.getId() : 0L;

        AutoBidConfig config = new AutoBidConfig(auctionId, bidderId, bidderEmail,
                BigDecimal.valueOf(maxBid), BigDecimal.valueOf(increment));
        autoBidDAO.upsert(config);

        autoBidConfigs.compute(auctionId, (k, existing) -> {
            PriorityQueue<AutoBidConfig> pq = existing != null ? existing : new PriorityQueue<>(AUTO_BID_COMPARATOR);
            pq.removeIf(c -> c.getBidderEmail().equalsIgnoreCase(bidderEmail));
            pq.offer(config);
            return pq;
        });
        return true;
    }

    public AuctionDto createAuction(Long itemId, String sellerEmail, LocalDateTime startTime, LocalDateTime endTime) {
        Item item = itemDAO.findById(itemId);
        if (item == null) return null;

        Auction auction = new Auction();
        auction.setItemId(itemId);
        auction.setTitle(item.getName());
        auction.setDescription(item.getDescription());
        auction.setCategory(item.getType());
        auction.setStartingBid(item.getStartingBid());
        auction.setCurrentBid(item.getStartingBid());
        auction.setStartTime(startTime != null ? startTime : LocalDateTime.now());
        auction.setEndTime(endTime != null ? endTime : LocalDateTime.now().plusHours(2));
        
        if (auction.getStartTime().isAfter(LocalDateTime.now())) {
            auction.setStatus("OPEN");
        } else {
            auction.setStatus("RUNNING");
        }
        
        auction.setSellerEmail(sellerEmail);
        auction.setImageUrl(item.getImageUrl());

        auction.setId(auctionDAO.insert(auction));
        activeAuctions.put(auction.getId(), auction);
        broadcastAuctionCreated(auction);

        return AuctionMapper.toDto(auction);
    }

    private void broadcastAuctionCreated(Auction auction) {
        ServerToClientMessage msg = new ServerToClientMessage();
        msg.type = MessageType.AUCTION_CREATED;
        msg.auction = AuctionMapper.toDto(auction);
        msg.success = true;
        msg.serverCurrentTimeMs = System.currentTimeMillis();
        BroadcastManager.getInstance().broadcastAll(msg);
    }

    public AuctionDto createAuction(Long itemId, String sellerEmail, int durationMinutes) {
        return createAuction(itemId, sellerEmail, LocalDateTime.now(), LocalDateTime.now().plusMinutes(durationMinutes));
    }

    public void tick() {
        LocalDateTime now = LocalDateTime.now();
        for (Auction auction : activeAuctions.values()) {
            auction.getBidLock().lock();
            try {
                String currentStatus = auction.getStatus();
                if ("OPEN".equals(currentStatus)) {
                    if (auction.getStartTime() != null && !now.isBefore(auction.getStartTime())) {
                        updateAuctionStatus(auction, "RUNNING", null);
                    }
                } else if ("RUNNING".equals(currentStatus)) {
                    if (auction.getEndTime() != null && now.isAfter(auction.getEndTime())) {
                        String winner = determineWinner(auction);
                        updateAuctionStatus(auction, "FINISHED", winner);
                        activeAuctions.remove(auction.getId());
                        autoBidConfigs.remove(auction.getId());
                    }
                }
            } finally {
                auction.getBidLock().unlock();
            }
        }
    }

    private String determineWinner(Auction auction) {
        if (auction.getBids().isEmpty()) return null;
        return auction.getBids().stream()
                .max(Comparator.comparing(BidTransaction::getAmount))
                .map(BidTransaction::getBidderEmail)
                .orElse(null);
    }

    private void updateAuctionStatus(Auction auction, String status, String winnerEmail) {
        auction.setStatus(status);
        auction.setWinnerEmail(winnerEmail);
        auctionDAO.updateStatus(auction.getId(), status, winnerEmail);
        broadcastAuctionUpdate(auction);
        broadcastAuctionStateChange(auction, false);
        
        if ("FINISHED".equals(status) && winnerEmail != null) {
            User winner = userDAO.findByEmail(winnerEmail);
            if (winner != null && winner.getId() != null) {
                NotificationService.getInstance().createAndSendNotification(
                    winner.getId(),
                    "Congratulations! 🎉",
                    "You won the auction: " + auction.getTitle(),
                    "SUCCESS"
                );
            }
        }
    }

    public boolean deleteAuction(Long id) {
        Auction auction = activeAuctions.get(id);
        if (auction == null) {
            auction = auctionDAO.findById(id);
        }
        if (auction != null) {
            if ("RUNNING".equals(auction.getStatus()) || "OPEN".equals(auction.getStatus())) {
                Long highestBidderId = auction.getHighestBidderId();
                BigDecimal currentBid = auction.getCurrentBid();
                if (highestBidderId != null && currentBid != null && currentBid.compareTo(BigDecimal.ZERO) > 0) {
                    WalletService.getInstance().releaseReserve(highestBidderId, id, currentBid);
                    broadcastWalletUpdate(highestBidderId);
                }
            }
        }
        boolean ok = auctionDAO.delete(id);
        if (ok) {
            activeAuctions.remove(id);
            autoBidConfigs.remove(id);
            listCache.invalidateAll();
            
            // Broadcast a deletion message to all clients
            ServerToClientMessage msg = new ServerToClientMessage();
            msg.type = MessageType.AUCTION_DELETED;
            msg.auctionId = id;
            BroadcastManager.getInstance().broadcastAll(msg);
        }
        return ok;
    }

    public boolean updateAuction(Long auctionId, String title, String description, Double startingBid, LocalDateTime endTime, String status) {
        Auction auction = findCachedAuction(auctionId);
        if (auction == null) return false;

        auction.getBidLock().lock();
        try {
            com.ltnc.auction.server.db.DBConnection.startTransaction();
            
            if (title != null) auction.setTitle(title);
            if (description != null) auction.setDescription(description);
            if (startingBid != null && (auction.getBids() == null || auction.getBids().isEmpty())) {
                auction.setStartingBid(BigDecimal.valueOf(startingBid));
                auction.setCurrentBid(BigDecimal.valueOf(startingBid));
            }
            if (endTime != null) {
                if (endTime.isBefore(LocalDateTime.now()) && 
                    ("OPEN".equals(auction.getStatus()) || "RUNNING".equals(auction.getStatus()))) {
                    com.ltnc.auction.server.db.DBConnection.rollbackTransaction();
                    return false;
                }
                auction.setEndTime(endTime);
            }

            if (status != null && !status.isBlank()) {
                String oldStatus = auction.getStatus();
                if (!oldStatus.equalsIgnoreCase(status)) {
                    auction.setStatus(status);
                    auctionDAO.updateStatus(auctionId, status, auction.getWinnerEmail());

                    // Sync activeAuctions map in memory
                    boolean wasActive = "OPEN".equals(oldStatus) || "RUNNING".equals(oldStatus);
                    boolean isActiveNow = "OPEN".equals(status) || "RUNNING".equals(status);
                    if (wasActive && !isActiveNow) {
                        activeAuctions.remove(auctionId);
                        autoBidConfigs.remove(auctionId);
                        
                        if ("CANCELED".equalsIgnoreCase(status)) {
                            Long highestBidderId = auction.getHighestBidderId();
                            BigDecimal currentBid = auction.getCurrentBid();
                            if (highestBidderId != null && currentBid != null && currentBid.compareTo(BigDecimal.ZERO) > 0) {
                                WalletService.getInstance().releaseReserve(highestBidderId, auctionId, currentBid);
                            }
                        } else if ("PAID".equalsIgnoreCase(status)) {
                            Long highestBidderId = auction.getHighestBidderId();
                            BigDecimal currentBid = auction.getCurrentBid();
                            if (highestBidderId != null && currentBid != null && currentBid.compareTo(BigDecimal.ZERO) > 0) {
                                WalletService.getInstance().completePayment(highestBidderId, auctionId, currentBid);
                            }
                        }
                    } else if (!wasActive && isActiveNow) {
                        activeAuctions.put(auctionId, auction);
                    }
                    broadcastAuctionStateChange(auction, false);
                }
            }

            auctionDAO.update(auction);
            com.ltnc.auction.server.db.DBConnection.commitTransaction();
            
            broadcastAuctionUpdate(auction);
            listCache.invalidateAll();
            return true;
        } catch (Exception e) {
            try { com.ltnc.auction.server.db.DBConnection.rollbackTransaction(); } catch (Exception ignored) {}
            return false;
        } finally {
            auction.getBidLock().unlock();
        }
    }

    public boolean confirmPayment(Long auctionId) {
        Auction auction = findCachedAuction(auctionId);
        if (auction == null) return false;

        auction.getBidLock().lock();
        try {
            if (!"FINISHED".equals(auction.getStatus())) return false;
            
            Long winnerId = auction.getHighestBidderId();
            if (winnerId == null) return false;

            try {
                com.ltnc.auction.server.db.DBConnection.startTransaction();

                auctionDAO.updateStatus(auctionId, "PAID", auction.getWinnerEmail());

                // Actual deduction
                WalletService.getInstance().completePayment(winnerId, auctionId, auction.getCurrentBid());

                com.ltnc.auction.server.db.DBConnection.commitTransaction();

                // Only update in-memory status AFTER successful commit
                auction.setStatus("PAID");
                broadcastAuctionUpdate(auction);
                broadcastAuctionStateChange(auction, false);
                broadcastWalletUpdate(winnerId);
                listCache.invalidateAll();
                return true;
            } catch (Exception e) {
                try {
                    com.ltnc.auction.server.db.DBConnection.rollbackTransaction();
                } catch (Exception ignored) {}
                // Reset in-memory status back to FINISHED so admin can retry
                auction.setStatus("FINISHED");
                LOG.error("Failed to confirm payment for auction " + auctionId, e);
                return false;
            }
        } finally {
            auction.getBidLock().unlock();
        }
    }

    public boolean cancelAuction(Long auctionId) {
        Auction auction = activeAuctions.get(auctionId);
        if (auction == null) {
            auction = auctionDAO.findById(auctionId);
        }
        if (auction == null) return false;

        auction.getBidLock().lock();
        try {
            if ("FINISHED".equals(auction.getStatus()) || "CANCELED".equals(auction.getStatus()) || "PAID".equals(auction.getStatus())) {
                return false;
            }
            
            Long highestBidderId = auction.getHighestBidderId();
            BigDecimal currentBid = auction.getCurrentBid();
            if (highestBidderId != null && currentBid != null && currentBid.compareTo(BigDecimal.ZERO) > 0) {
                WalletService.getInstance().releaseReserve(highestBidderId, auctionId, currentBid);
            }

            auction.setStatus("CANCELED");
            auctionDAO.updateStatus(auctionId, "CANCELED", null);
            activeAuctions.remove(auctionId);
            autoBidConfigs.remove(auctionId);
            
            broadcastAuctionUpdate(auction);
            broadcastAuctionStateChange(auction, false);
            if (highestBidderId != null) {
                broadcastWalletUpdate(highestBidderId);
            }
            listCache.invalidateAll();
            return true;
        } finally {
            auction.getBidLock().unlock();
        }
    }

    public void syncAuctionWithItem(Long itemId, String title, String description, String category, BigDecimal startingBid, String imageUrl) {
        // 1. Update DB
        auctionDAO.updateByItemId(itemId, title, description, category, startingBid, imageUrl);

        // 2. Update in-memory active auctions
        for (Auction a : activeAuctions.values()) {
            if (itemId.equals(a.getItemId())) {
                a.setTitle(title);
                a.setDescription(description);
                a.setCategory(category);
                a.setImageUrl(imageUrl);
                if (a.getHighestBidderId() == null) {
                    a.setStartingBid(startingBid);
                    a.setCurrentBid(startingBid);
                }
                broadcastAuctionUpdate(a);
            }
        }
        listCache.invalidateAll();
    }

    public void deleteAuctionByItem(Long itemId) {
        List<Auction> toDelete = auctionDAO.findAll().stream()
                .filter(a -> itemId.equals(a.getItemId()))
                .toList();
        for (Auction a : toDelete) {
            deleteAuction(a.getId());
        }
    }

    public List<BidDto> getBidLogs() {
        return bidDAO.findAll().stream()
                .map(AuctionMapper::toBidDto)
                .collect(Collectors.toList());
    }

    public List<Auction> getAllActiveAuctionsForUser(Long userId) {
        return activeAuctions.values().stream()
                .filter(a -> userId.equals(a.getHighestBidderId()))
                .collect(Collectors.toList());
    }

    public boolean hasActiveAuctionsAsSeller(String email) {
        if (email == null) return false;
        return activeAuctions.values().stream()
                .anyMatch(a -> email.equalsIgnoreCase(a.getSellerEmail()));
    }

    private void broadcastAuctionUpdate(Auction auction) {
        broadcastAuctionUpdate(auction, null);
    }

    private void broadcastAuctionUpdate(Auction auction, List<BidTransaction> newBids) {
        ServerToClientMessage msg = new ServerToClientMessage();
        msg.type = MessageType.AUCTION_UPDATE;
        msg.eventType = "AUCTION_UPDATE";
        msg.auctionId = auction.getId();
        msg.currentBid = auction.getCurrentBid() != null ? auction.getCurrentBid().doubleValue() : null;
        msg.status = auction.getStatus();
        msg.winnerEmail = auction.getWinnerEmail();
        msg.newEndTime = auction.getEndTime() != null ? auction.getEndTime().format(ISO_FORMATTER) : null;
        
        if (newBids != null && !newBids.isEmpty()) {
            msg.newBids = newBids.stream().map(AuctionMapper::toBidDto).collect(Collectors.toList());
        }
        // Do NOT send the entire bids list to save bandwidth (Delta Payload Optimization)
        
        msg.serverCurrentTimeMs = System.currentTimeMillis();
        BroadcastManager.getInstance().broadcastAll(msg);
    }
}
