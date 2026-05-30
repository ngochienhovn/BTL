package com.ltnc.auction.server.service;

import com.ltnc.auction.server.model.Wallet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

public class WalletServiceTest {

    @BeforeEach
    void reset() throws Exception {
        Field wsInstance = WalletService.class.getDeclaredField("instance");
        wsInstance.setAccessible(true);
        wsInstance.set(null, null);
        WalletService ws = WalletService.getInstance();
        Field walletDaoField = WalletService.class.getDeclaredField("walletDAO");
        walletDaoField.setAccessible(true);
        walletDaoField.set(ws, new AuctionServiceTest.StubWalletDAO());
        Field walletTxField = WalletService.class.getDeclaredField("txDAO");
        walletTxField.setAccessible(true);
        walletTxField.set(ws, new AuctionServiceTest.StubWalletTxDAO());
    }

    @Test
    void deposit_increases_balance() {
        WalletService ws = WalletService.getInstance();
        ws.deposit(1L, new BigDecimal("25.00"));
        Wallet w = ws.getWallet(1L);
        assertEquals(0, w.getBalance().setScale(2).compareTo(new BigDecimal("100025.00")));
    }

    @Test
    void withdraw_fails_when_insufficient_available() {
        WalletService ws = WalletService.getInstance();
        WalletService.WithdrawResultCode c = ws.withdraw(1L, new BigDecimal("200000.00"));
        assertEquals(WalletService.WithdrawResultCode.INSUFFICIENT_AVAILABLE, c);
    }

    @Test
    void applyForBid_reserves_and_releases_between_users() {
        WalletService ws = WalletService.getInstance();
        WalletService.WalletApplyResult r =
                ws.applyForBid(1L, null, BigDecimal.ZERO, new BigDecimal("500.00"), 10L);
        assertTrue(r.success());
        assertTrue(ws.getWallet(1L).getReserved().compareTo(BigDecimal.ZERO) > 0);

        WalletService.WalletApplyResult r2 =
                ws.applyForBid(2L, 1L, new BigDecimal("500.00"), new BigDecimal("600.00"), 10L);
        assertTrue(r2.success());
        assertTrue(ws.getWallet(2L).getReserved().compareTo(BigDecimal.ZERO) >= 0);
        assertTrue(ws.getWallet(1L).getReserved().compareTo(BigDecimal.ZERO) >= 0);
    }

    @Test
    void concurrent_deposits_keep_non_negative_reserved() throws Exception {
        WalletService ws = WalletService.getInstance();
        int threads = 50;
        ExecutorService ex = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            ex.submit(() -> {
                try {
                    ws.deposit(1L, new BigDecimal("1.00"));
                    ws.withdraw(1L, new BigDecimal("0.50"));
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        ex.shutdown();
        Wallet w = ws.getWallet(1L);
        assertTrue(w.getReserved().compareTo(BigDecimal.ZERO) >= 0);
        assertTrue(w.getAvailable().compareTo(BigDecimal.ZERO) >= 0);
    }
}
