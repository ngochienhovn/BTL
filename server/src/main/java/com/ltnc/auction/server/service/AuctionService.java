package com.ltnc.auction.server.service;

import com.ltnc.auction.server.model.Auction;
import com.ltnc.auction.server.model.BidTransaction;
import com.ltnc.auction.server.model.User;
import com.ltnc.auction.server.model.Wallet;
import com.ltnc.auction.server.model.WalletTransaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuctionService {

    private Auction[] fakeDataAuction;
    private Wallet[] fakeDataWallet;
    private BidTransaction[] fakeDataBidTransaction;
    private WalletTransaction[] fakeDataWalletTransaction;

    private int auctionCount;
    private int bidTransactionCount;
    private int walletTransactionCount;

    private static volatile AuctionService instance;

    private AuctionService() {
        fakeDataAuction = new Auction[100];
        fakeDataWallet = new Wallet[100];
        fakeDataBidTransaction = new BidTransaction[1000];
        fakeDataWalletTransaction = new WalletTransaction[1000];

        auctionCount = 0;
        bidTransactionCount = 0;
        walletTransactionCount = 0;
    }

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

    public record BidResult(
            boolean success,
            String code,
            Auction auction,
            double currentBid,
            double requiredTopUp
    ) {}

    public BidResult placeBid(Long auctionId, String bidderEmail, double amount) {
        Auction auction = findAuctionById(auctionId);
        if (auction == null) {
            return new BidResult(false, "AUCTION_NOT_FOUND", null, 0, 0);
        }

        auction.getBidLock().lock();
        try {
            if (auction.getStatus() == null || !"RUNNING".equalsIgnoreCase(auction.getStatus())) {
                return new BidResult(false, "AUCTION_CLOSED", auction, toDouble(auction.getCurrentBid()), 0);
            }

            BigDecimal bidAmount = BigDecimal.valueOf(amount);
            BigDecimal currentBid = safe(auction.getCurrentBid());

            if (bidAmount.compareTo(BigDecimal.ZERO) <= 0 || bidAmount.compareTo(currentBid) <= 0) {
                return new BidResult(false, "INVALID_AMOUNT", auction, toDouble(currentBid), 0);
            }

            User bidder = findBidderByEmail(bidderEmail);
            if (bidder == null) {
                return new BidResult(false, "BID_TOO_LOW", auction, toDouble(currentBid), 0);
            }

            Wallet bidderWallet = findWalletByUserId(bidder.getId());
            if (bidderWallet == null) {
                return new BidResult(false, "INSUFFICIENT_FUNDS", auction, toDouble(currentBid), amount);
            }

            BigDecimal available = bidderWallet.getAvailable();
            if (bidAmount.compareTo(available) > 0) {
                BigDecimal requiredTopUp = bidAmount.subtract(available);
                return new BidResult(
                        false,
                        "INSUFFICIENT_FUNDS",
                        auction,
                        toDouble(currentBid),
                        toDouble(requiredTopUp)
                );
            }

            String oldLeaderId = auction.getHighestBidderId();
            BigDecimal oldCurrentBid = currentBid;

            if (oldLeaderId != null && !oldLeaderId.isBlank()) {
                Wallet oldLeaderWallet = findWalletByUserId(oldLeaderId);
                if (oldLeaderWallet != null) {
                    oldLeaderWallet.setReserved(
                            safe(oldLeaderWallet.getReserved()).subtract(oldCurrentBid).max(BigDecimal.ZERO)
                    );
                    oldLeaderWallet.setUpdatedAt(LocalDateTime.now());

                    insertWalletTransaction(oldLeaderId, "RELEASE", oldCurrentBid, auctionId, LocalDateTime.now());
                }
            }

            bidderWallet.setReserved(safe(bidderWallet.getReserved()).add(bidAmount));
            bidderWallet.setUpdatedAt(LocalDateTime.now());

            auction.setCurrentBid(bidAmount);
            auction.setHighestBidderId(bidder.getId());

            BidTransaction bidTransaction = insertBidTransaction(
                    auctionId,
                    bidder.getId(),
                    bidder.getEmail(),
                    bidder.getFullname(),
                    bidAmount,
                    LocalDateTime.now()
            );

            if (auction.getBidTransactions() == null) {
                auction.setBidTransactions(new ArrayList<>());
            }
            auction.getBidTransactions().add(bidTransaction);

            insertWalletTransaction(bidder.getId(), "RESERVE", bidAmount, auctionId, LocalDateTime.now());

            updateAuction(auction);

            return new BidResult(true, "OK", auction, toDouble(bidAmount), 0);
        } finally {
            auction.getBidLock().unlock();
        }
    }

    public List<Auction> getAllAuctions() {
        List<Auction> result = new ArrayList<>();
        for (Auction auction : fakeDataAuction) {
            if (auction != null) {
                result.add(auction);
            }
        }
        return result;
    }

    private Auction findAuctionById(Long auctionId) {
        if (auctionId == null) {
            return null;
        }

        for (Auction auction : fakeDataAuction) {
            if (auction != null && auction.getId() != null) {
                try {
                    Long id = Long.parseLong(auction.getId());
                    if (auctionId.equals(id)) {
                        return auction;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return null;
    }

    private User findBidderByEmail(String email) {
        return AuthService.getInstance().findUserByEmailForAuction(email);
    }

    private Wallet findWalletByUserId(String userId) {
        for (Wallet wallet : fakeDataWallet) {
            if (wallet != null && wallet.getUserId() != null && wallet.getUserId().equals(userId)) {
                return wallet;
            }
        }
        return null;
    }

    private void updateAuction(Auction updatedAuction) {
        for (int i = 0; i < fakeDataAuction.length; i++) {
            Auction current = fakeDataAuction[i];
            if (current != null && current.getId() != null && current.getId().equals(updatedAuction.getId())) {
                fakeDataAuction[i] = updatedAuction;
                return;
            }
        }

        if (auctionCount < fakeDataAuction.length) {
            fakeDataAuction[auctionCount++] = updatedAuction;
        }
    }

    private BidTransaction insertBidTransaction(
            Long auctionId,
            String bidderId,
            String bidderEmail,
            String bidderName,
            BigDecimal amount,
            LocalDateTime createdAt
    ) {
        BidTransaction tx = new BidTransaction();
        tx.setId(String.valueOf(bidTransactionCount + 1));
        tx.setAuctionId(String.valueOf(auctionId));
        tx.setBidderId(bidderId);
        tx.setBidderEmail(bidderEmail);
        tx.setBidderName(bidderName);
        tx.setAmount(amount);
        tx.setCreatedAt(createdAt);

        if (bidTransactionCount < fakeDataBidTransaction.length) {
            fakeDataBidTransaction[bidTransactionCount++] = tx;
        }

        return tx;
    }

    private WalletTransaction insertWalletTransaction(
            String userId,
            String type,
            BigDecimal amount,
            Long refAuctionId,
            LocalDateTime createdAt
    ) {
        WalletTransaction tx = new WalletTransaction();
        tx.setId(String.valueOf(walletTransactionCount + 1));
        tx.setUserId(userId);
        tx.setType(type);
        tx.setAmount(amount);
        tx.setRefAuctionId(refAuctionId);
        tx.setCreatedAt(createdAt);

        if (walletTransactionCount < fakeDataWalletTransaction.length) {
            fakeDataWalletTransaction[walletTransactionCount++] = tx;
        }

        return tx;
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private double toDouble(BigDecimal value) {
        return safe(value).doubleValue();
    }
}