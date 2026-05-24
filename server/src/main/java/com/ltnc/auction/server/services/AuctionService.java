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
        OK, BID_TOO_LOW, AUCTION_CLOSED, AUCTION_NOT_FOUND, INVALID_AMOUNT, INSUFFICIENT_FUNDS
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
                    return AuctionMapper.toDto(mem != null ? mem : a);
                })
                .collect(Collectors.toList())
        );
    }

    public AuctionDto getAuctionById(Long id) {
        Auction mem = activeAuctions.get(id);
        if (mem != null) return AuctionMapper.toDto(mem);
        
        Auction cached = auctionCache.get(id, auctionDAO::findById);
        return AuctionMapper.toDto(cached);
    }

    public BidResult placeBid(Long auctionId, String bidderEmail, double amount) {
        Auction auction = activeAuctions.get(auctionId);
        if (auction == null) {
            return new BidResult(BidResultCode.AUCTION_NOT_FOUND, null, null);
        }

        auction.getBidLock().lock();
        try {
            if (!"RUNNING".equals(auction.getStatus())) {
                return new BidResult(BidResultCode.AUCTION_CLOSED, AuctionMapper.toDto(auction), null);
            }
            if (amount <= 0) {
                return new BidResult(BidResultCode.INVALID_AMOUNT, AuctionMapper.toDto(auction), null);
            }
            if (auction.getCurrentBid() != null && amount <= auction.getCurrentBid().doubleValue()) {
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

            WalletService ws = WalletService.getInstance();
            WalletService.WalletApplyResult wr =
                    ws.applyForBid(bidderId, prevLeaderId, prevBid, BigDecimal.valueOf(amount), auctionId);
            if (!wr.success()) {
                double topUp = wr.requiredTopUp() != null ? wr.requiredTopUp().doubleValue() : 0;
                return new BidResult(BidResultCode.INSUFFICIENT_FUNDS, AuctionMapper.toDto(auction), topUp);
            }

            BigDecimal newBid = BigDecimal.valueOf(amount);
            auction.setCurrentBid(newBid);
            auction.setHighestBidderId(bidderId);
            auctionDAO.updateCurrentBid(auctionId, newBid, bidderId);

            BidTransaction bidTx = new BidTransaction(auctionId, bidderId, bidderEmail, bidderName, newBid);
            bidTx.setId(bidDAO.insert(bidTx));
            auction.getBids().add(bidTx);

            boolean extended = bidProcessor.maybeApplyAntiSniping(auction, auctionDAO);
            bidProcessor.processAutoBids(
                    auction, bidderEmail, autoBidConfigs, auctionDAO, bidDAO, WalletService.getInstance(), userDAO);

            broadcastAuctionUpdate(auction);
            if (extended) {
                broadcastAuctionStateChange(auction, true);
            }
            broadcastWalletUpdatesAfterBid(auction, prevLeaderId, bidderId);

            listCache.invalidateAll();
            return new BidResult(BidResultCode.OK, AuctionMapper.toDto(auction), null);
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

    public void registerAutoBid(Long auctionId, String bidderEmail, double maxBid, double increment) {
        if (maxBid <= 0 || increment <= 0) return;
        Auction auction = activeAuctions.get(auctionId);
        if (auction == null) return;

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
    }

    public AuctionDto createAuction(Long itemId, String sellerEmail, int durationMinutes) {
        Item item = itemDAO.findById(itemId);
        if (item == null) return null;

        Auction auction = new Auction();
        auction.setItemId(itemId);
        auction.setTitle(item.getName());
        auction.setDescription(item.getDescription());
        auction.setCategory(item.getType());
        auction.setStartingBid(item.getStartingBid());
        auction.setCurrentBid(item.getStartingBid());
        auction.setStartTime(LocalDateTime.now());
        auction.setEndTime(LocalDateTime.now().plusMinutes(durationMinutes));
        auction.setStatus("RUNNING");
        auction.setSellerEmail(sellerEmail);
        auction.setImageUrl(item.getImageUrl());

        auction.setId(auctionDAO.insert(auction));
        activeAuctions.put(auction.getId(), auction);
        broadcastAuctionUpdate(auction);

        return AuctionMapper.toDto(auction);
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
    }

    public List<BidDto> getBidLogs() {
        return bidDAO.findAll().stream()
                .map(AuctionMapper::toBidDto)
                .collect(Collectors.toList());
    }

    private void broadcastAuctionUpdate(Auction auction) {
        ServerToClientMessage msg = new ServerToClientMessage();
        msg.type = MessageType.AUCTION_UPDATE;
        msg.eventType = "AUCTION_UPDATE";
        msg.auctionId = auction.getId();
        msg.currentBid = auction.getCurrentBid() != null ? auction.getCurrentBid().doubleValue() : null;
        msg.status = auction.getStatus();
        msg.winnerEmail = auction.getWinnerEmail();
        msg.newEndTime = auction.getEndTime() != null ? auction.getEndTime().format(ISO_FORMATTER) : null;
        msg.serverCurrentTimeMs = System.currentTimeMillis();
        BroadcastManager.getInstance().broadcastAll(msg);
    }
}
