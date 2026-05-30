package com.ltnc.auction.server.service;

import com.ltnc.auction.server.dao.WalletDAO;
import com.ltnc.auction.server.dao.WalletTransactionDAO;
import com.ltnc.auction.server.model.Wallet;
import com.ltnc.auction.server.model.WalletTransaction;
import com.ltnc.auction.server.model.WalletTxType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Wallet mutations with per-user locking. Used during bids (reserve/release) and deposit/withdraw.
 */
public class WalletService {

    public record WalletApplyResult(boolean success, BigDecimal requiredTopUp) {
        public static WalletApplyResult succeeded() {
            return new WalletApplyResult(true, null);
        }

        public static WalletApplyResult insufficient(BigDecimal requiredTopUp) {
            return new WalletApplyResult(false, requiredTopUp);
        }
    }

    public enum WithdrawResultCode {
        OK, INSUFFICIENT_AVAILABLE, INVALID_AMOUNT
    }

    private static volatile WalletService instance;

    /** Non-final for unit tests (reflection injection). */
    private WalletDAO walletDAO = new WalletDAO();

    private WalletTransactionDAO txDAO = new WalletTransactionDAO();
    private final ConcurrentHashMap<Long, ReentrantLock> userLocks = new ConcurrentHashMap<>();

    private WalletService() {}

    public static WalletService getInstance() {
        if (instance == null) {
            synchronized (WalletService.class) {
                if (instance == null) {
                    instance = new WalletService();
                }
            }
        }
        return instance;
    }

    private ReentrantLock lockFor(Long userId) {
        return userLocks.computeIfAbsent(userId, k -> new ReentrantLock());
    }

    public Wallet getWallet(Long userId) {
        return walletDAO.findOrCreateByUserId(userId);
    }

    public void deposit(Long userId, BigDecimal amount) {
        if (userId == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        amount = amount.setScale(2, RoundingMode.HALF_UP);
        lockFor(userId).lock();
        try {
            com.ltnc.auction.server.db.DBConnection.startTransaction();
            Wallet w = walletDAO.findOrCreateByUserId(userId);
            BigDecimal nb = w.getBalance().add(amount);
            walletDAO.updateBalances(userId, nb, w.getReserved());
            WalletTransaction tx = new WalletTransaction(userId, WalletTxType.DEPOSIT, amount, null);
            txDAO.insert(tx);
            com.ltnc.auction.server.db.DBConnection.commitTransaction();
        } catch (Exception e) {
            try { com.ltnc.auction.server.db.DBConnection.rollbackTransaction(); } catch (Exception ignored) {}
            throw new RuntimeException("Failed to deposit funds", e);
        } finally {
            lockFor(userId).unlock();
        }
    }

    public WithdrawResultCode withdraw(Long userId, BigDecimal amount) {
        if (userId == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return WithdrawResultCode.INVALID_AMOUNT;
        }

        amount = amount.setScale(2, RoundingMode.HALF_UP);
        lockFor(userId).lock();
        try {
            com.ltnc.auction.server.db.DBConnection.startTransaction();
            Wallet w = walletDAO.findOrCreateByUserId(userId);
            if (w.getAvailable().compareTo(amount) < 0) {
                com.ltnc.auction.server.db.DBConnection.rollbackTransaction();
                return WithdrawResultCode.INSUFFICIENT_AVAILABLE;
            }
            BigDecimal nb = w.getBalance().subtract(amount);
            walletDAO.updateBalances(userId, nb, w.getReserved());
            WalletTransaction tx = new WalletTransaction(userId, WalletTxType.WITHDRAW, amount, null);
            txDAO.insert(tx);
            com.ltnc.auction.server.db.DBConnection.commitTransaction();
            return WithdrawResultCode.OK;
        } catch (Exception e) {
            try { com.ltnc.auction.server.db.DBConnection.rollbackTransaction(); } catch (Exception ignored) {}
            throw new RuntimeException("Failed to withdraw funds", e);
        } finally {
            lockFor(userId).unlock();
        }
    }

    /**
     * Reserve funds for a winning bid: release previous leader, add reserve for bidder / delta for same bidder.
     */
    public WalletApplyResult applyForBid(Long bidderId, Long previousLeaderId, BigDecimal previousWinningBid,
            BigDecimal newBidAmount, Long auctionId) {
        if (bidderId == null || newBidAmount == null) {
            return WalletApplyResult.insufficient(BigDecimal.ZERO);
        }
        newBidAmount = newBidAmount.setScale(2, RoundingMode.HALF_UP);
        BigDecimal prevBid = previousWinningBid != null ? previousWinningBid.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        boolean sameLeaderRaising =
                previousLeaderId != null && previousLeaderId.equals(bidderId);
        BigDecimal needed;
        if (sameLeaderRaising) {
            needed = newBidAmount.subtract(prevBid);
            if (needed.compareTo(BigDecimal.ZERO) < 0) {
                needed = BigDecimal.ZERO;
            }
        } else {
            needed = newBidAmount;
        }

        boolean releaseDifferent =
                previousLeaderId != null && !previousLeaderId.equals(bidderId)
                        && prevBid.compareTo(BigDecimal.ZERO) > 0;

        long a = releaseDifferent ? Math.min(bidderId, previousLeaderId) : bidderId;
        long b = releaseDifferent ? Math.max(bidderId, previousLeaderId) : bidderId;

        if (a != b) lockFor(a).lock();
        lockFor(b).lock();
        try {
            Wallet bw = walletDAO.findOrCreateByUserId(bidderId);
            if (bw.getAvailable().compareTo(needed) < 0) {
                BigDecimal topUp = needed.subtract(bw.getAvailable());
                if (topUp.compareTo(BigDecimal.ZERO) < 0) {
                    topUp = BigDecimal.ZERO;
                }
                return WalletApplyResult.insufficient(topUp.setScale(2, RoundingMode.HALF_UP));
            }

            if (releaseDifferent) {
                release(previousLeaderId, auctionId, prevBid);
            }
            addReserve(bidderId, auctionId, needed);
            return WalletApplyResult.succeeded();
        } finally {
            lockFor(b).unlock();
            if (a != b) lockFor(a).unlock();
        }
    }

    public void releaseReserve(Long userId, Long auctionId, BigDecimal amount) {
        if (userId == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        lockFor(userId).lock();
        try {
            release(userId, auctionId, amount);
        } finally {
            lockFor(userId).unlock();
        }
    }

    /**
     * Completes a payment by deducting from both balance and reserved.
     * Called when an auction is confirmed as PAID.
     */
    public void completePayment(Long userId, Long auctionId, BigDecimal amount) {
        if (userId == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        amount = amount.setScale(2, RoundingMode.HALF_UP);
        lockFor(userId).lock();
        try {
            // NOTE: participates in the outer transaction opened by AuctionService.confirmPayment
            // Do NOT open a new transaction here to avoid nested-transaction corruption.
            Wallet w = walletDAO.findOrCreateByUserId(userId);
            BigDecimal newBalance = w.getBalance().subtract(amount);
            BigDecimal newReserved = w.getReserved().subtract(amount);

            if (newBalance.compareTo(BigDecimal.ZERO) < 0) newBalance = BigDecimal.ZERO;
            if (newReserved.compareTo(BigDecimal.ZERO) < 0) newReserved = BigDecimal.ZERO;

            walletDAO.updateBalances(userId, newBalance, newReserved);

            WalletTransaction tx = new WalletTransaction(userId, WalletTxType.WITHDRAW, amount, auctionId);
            txDAO.insert(tx);
        } catch (Exception e) {
            throw new RuntimeException("Failed to complete payment", e);
        } finally {
            lockFor(userId).unlock();
        }
    }

    private void release(Long userId, Long auctionId, BigDecimal amount) {
        if (userId == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        try {
            com.ltnc.auction.server.db.DBConnection.startTransaction();
            Wallet w = walletDAO.findOrCreateByUserId(userId);
            BigDecimal newReserved = w.getReserved().subtract(amount);
            if (newReserved.compareTo(BigDecimal.ZERO) < 0) {
                newReserved = BigDecimal.ZERO;
            }
            walletDAO.updateBalances(userId, w.getBalance(), newReserved.setScale(2, RoundingMode.HALF_UP));
            WalletTransaction tx = new WalletTransaction(userId, WalletTxType.RELEASE, amount, auctionId);
            txDAO.insert(tx);
            com.ltnc.auction.server.db.DBConnection.commitTransaction();
        } catch (Exception e) {
            try { com.ltnc.auction.server.db.DBConnection.rollbackTransaction(); } catch (Exception ignored) {}
            throw new RuntimeException("Failed to release reserve", e);
        }
    }

    private void addReserve(Long userId, Long auctionId, BigDecimal delta) {
        if (delta == null || delta.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        delta = delta.setScale(2, RoundingMode.HALF_UP);
        try {
            com.ltnc.auction.server.db.DBConnection.startTransaction();
            Wallet w = walletDAO.findOrCreateByUserId(userId);
            BigDecimal newReserved = w.getReserved().add(delta);
            walletDAO.updateBalances(userId, w.getBalance(), newReserved.setScale(2, RoundingMode.HALF_UP));
            WalletTransaction tx = new WalletTransaction(userId, WalletTxType.RESERVE, delta, auctionId);
            txDAO.insert(tx);
            com.ltnc.auction.server.db.DBConnection.commitTransaction();
        } catch (Exception e) {
            try { com.ltnc.auction.server.db.DBConnection.rollbackTransaction(); } catch (Exception ignored) {}
            throw new RuntimeException("Failed to add reserve", e);
        }
    }

    public List<WalletTransaction> getTransactions(Long userId) {
        return txDAO.findByUserId(userId);
    }
}
