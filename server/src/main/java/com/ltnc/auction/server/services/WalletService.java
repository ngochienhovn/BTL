package com.ltnc.auction.server.services;

import com.ltnc.auction.server.dao.WalletDAO;
import com.ltnc.auction.server.dao.WalletTransactionDAO;
import com.ltnc.auction.server.model.Wallet;
import com.ltnc.auction.server.model.WalletTransaction;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WalletService {
    private final WalletDAO walletDAO;
    private final WalletTransactionDAO walletTransactionDAO;

    public WalletService(WalletDAO walletDAO, WalletTransactionDAO walletTransactionDAO) {
        this.walletDAO = walletDAO;
        this.walletTransactionDAO = walletTransactionDAO;
    }

    public Wallet getWallet(Long userId) {
        return walletDAO.findOrCreateByUserId(userId);
    }

    public boolean deposit(Long userId, double amount) {
        if (amount <= 0) {
            return false;
        }
        Wallet wallet = walletDAO.findOrCreateByUserId(userId);
        BigDecimal newBalance = wallet.getBalance().add(BigDecimal.valueOf(amount));
        boolean updated = walletDAO.updateBalances(userId, newBalance, wallet.getReserved());
        if (updated) {
            walletTransactionDAO.insert(
                    new WalletTransaction(userId, "DEPOSIT", BigDecimal.valueOf(amount), null, LocalDateTime.now())
            );
        }
        return updated;
    }

    public boolean withdraw(Long userId, double amount) {
        if (amount <= 0) {
            return false;
        }
        Wallet wallet = walletDAO.findOrCreateByUserId(userId);
        BigDecimal available = wallet.getAvailable();
        BigDecimal withdrawAmount = BigDecimal.valueOf(amount);
        if (withdrawAmount.compareTo(available) > 0) {
            return false;
        }
        BigDecimal newBalance = wallet.getBalance().subtract(withdrawAmount);
        boolean updated = walletDAO.updateBalances(userId, newBalance, wallet.getReserved());
        if (updated) {
            walletTransactionDAO.insert(
                    new WalletTransaction(userId, "WITHDRAW", withdrawAmount, null, LocalDateTime.now())
            );
        }
        return updated;
    }
}
