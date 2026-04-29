package com.ltnc.auction.server.services;

import com.ltnc.auction.server.dao.AuctionDAO;
import com.ltnc.auction.server.dao.BidDAO;
import com.ltnc.auction.server.dao.UserDAO;
import com.ltnc.auction.server.dao.WalletDAO;
import com.ltnc.auction.server.dao.WalletTransactionDAO;
import com.ltnc.auction.server.model.Auction;
import com.ltnc.auction.server.model.BidTransaction;
import com.ltnc.auction.server.model.User;
import com.ltnc.auction.server.model.Wallet;
import com.ltnc.auction.server.model.WalletTransaction;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class AuctionService {
    private final AuctionDAO auctionDAO;
    private final BidDAO bidDAO;
    private final UserDAO userDAO;
    private final WalletDAO walletDAO;
    private final WalletTransactionDAO walletTransactionDAO;

    public AuctionService(
            AuctionDAO auctionDAO,
            BidDAO bidDAO,
            UserDAO userDAO,
            WalletDAO walletDAO,
            WalletTransactionDAO walletTransactionDAO
    ) {
        this.auctionDAO = auctionDAO;
        this.bidDAO = bidDAO;
        this.userDAO = userDAO;
        this.walletDAO = walletDAO;
        this.walletTransactionDAO = walletTransactionDAO;
    }

    public record BidResult(boolean success, String code, Auction auction, double currentBid, Double requiredTopUp) {}

    public List<Auction> getAllAuctions() {
        return auctionDAO.findAll();
    }

    public BidResult placeBid(Long auctionId, String bidderEmail, double amount) {
        Auction auction = auctionDAO.findById(auctionId);
        if (auction == null) {
            return new BidResult(false, "AUCTION_NOT_FOUND", null, 0, null);
        }

        auction.getBidLock().lock();
        try {
            if (!"RUNNING".equalsIgnoreCase(auction.getStatus())) {
                return new BidResult(false, "AUCTION_CLOSED", auction, toDouble(auction.getCurrentBid()), null);
            }
            if (amount <= 0) {
                return new BidResult(false, "INVALID_AMOUNT", auction, toDouble(auction.getCurrentBid()), null);
            }

            BigDecimal bidAmount = BigDecimal.valueOf(amount);
            BigDecimal currentBid = safe(auction.getCurrentBid());
            if (bidAmount.compareTo(currentBid) <= 0) {
                return new BidResult(false, "BID_TOO_LOW", auction, toDouble(currentBid), null);
            }

            Optional<User> bidderOpt = userDAO.findByEmail(bidderEmail);
            if (bidderOpt.isEmpty()) {
                return new BidResult(false, "AUCTION_NOT_FOUND", auction, toDouble(currentBid), null);
            }
            User bidder = bidderOpt.get();

            Wallet bidderWallet = walletDAO.findOrCreateByUserId(bidder.getId());
            BigDecimal available = bidderWallet.getAvailable();
            if (bidAmount.compareTo(available) > 0) {
                BigDecimal required = bidAmount.subtract(available);
                return new BidResult(false, "INSUFFICIENT_FUNDS", auction, toDouble(currentBid), toDouble(required));
            }

            Long oldLeaderId = auction.getHighestBidderId();
            BigDecimal oldCurrentBid = currentBid;

            if (oldLeaderId != null) {
                Wallet oldLeaderWallet = walletDAO.findOrCreateByUserId(oldLeaderId);
                oldLeaderWallet.setReserved(safe(oldLeaderWallet.getReserved()).subtract(oldCurrentBid).max(BigDecimal.ZERO));
                walletDAO.updateBalances(oldLeaderId, safe(oldLeaderWallet.getBalance()), safe(oldLeaderWallet.getReserved()));
                walletTransactionDAO.insert(new WalletTransaction(oldLeaderId, "RELEASE", oldCurrentBid, auctionId, LocalDateTime.now()));
            }

            bidderWallet.setReserved(safe(bidderWallet.getReserved()).add(bidAmount));
            walletDAO.updateBalances(bidder.getId(), safe(bidderWallet.getBalance()), safe(bidderWallet.getReserved()));
            walletTransactionDAO.insert(new WalletTransaction(bidder.getId(), "RESERVE", bidAmount, auctionId, LocalDateTime.now()));

            auctionDAO.updateCurrentBid(auctionId, bidAmount, bidder.getId());
            auction.setCurrentBid(bidAmount);
            auction.setHighestBidderId(bidder.getId());

            BidTransaction tx = new BidTransaction(
                    auctionId,
                    bidder.getId(),
                    bidder.getEmail(),
                    bidder.getFullName(),
                    bidAmount,
                    LocalDateTime.now()
            );
            bidDAO.insert(tx);
            return new BidResult(true, "OK", auction, toDouble(bidAmount), null);
        } finally {
            auction.getBidLock().unlock();
        }
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private double toDouble(BigDecimal value) {
        return safe(value).doubleValue();
    }
}