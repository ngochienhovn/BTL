package com.ltnc.auction.server.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ltnc.auction.server.dao.AuctionDAO;
import com.ltnc.auction.server.dao.BidDAO;
import com.ltnc.auction.server.dao.UserDAO;
import com.ltnc.auction.server.dao.WalletDAO;
import com.ltnc.auction.server.dao.WalletTransactionDAO;
import com.ltnc.auction.server.model.Auction;
import com.ltnc.auction.server.model.User;
import com.ltnc.auction.server.model.Wallet;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuctionServiceTest {

    @Mock
    private AuctionDAO auctionDAO;

    @Mock
    private BidDAO bidDAO;

    @Mock
    private UserDAO userDAO;

    @Mock
    private WalletDAO walletDAO;

    @Mock
    private WalletTransactionDAO walletTransactionDAO;

    private AuctionService auctionService;

    @BeforeEach
    void setUp() {
        auctionService = new AuctionService(auctionDAO, bidDAO, userDAO, walletDAO, walletTransactionDAO);
    }

    @Test
    void placeBid_higherThanCurrentBid_shouldReturnOk() {
        Long auctionId = 1L;
        User bidder = buildUser(11L, "bidder1@mail.com", "Bidder 1");
        Auction auction = buildRunningAuction(auctionId, 100.0, null);
        Wallet wallet = buildWallet(11L, "1000.00", "0.00");

        when(auctionDAO.findById(auctionId)).thenReturn(auction);
        when(userDAO.findByEmail("bidder1@mail.com")).thenReturn(Optional.of(bidder));
        when(walletDAO.findOrCreateByUserId(11L)).thenReturn(wallet);
        when(walletDAO.updateBalances(eq(11L), any(), any())).thenReturn(true);

        AuctionService.BidResult result = auctionService.placeBid(auctionId, bidder.getEmail(), 150.0);

        assertTrue(result.success());
        assertEquals("OK", result.code());
        assertEquals(150.0, result.currentBid(), 0.0001);
        assertNotNull(result.auction());

        verify(auctionDAO, times(1))
                .updateCurrentBid(eq(auctionId), eq(BigDecimal.valueOf(150.0)), eq(11L));
        verify(bidDAO, times(1)).insert(any());
        verify(walletTransactionDAO, atLeastOnce()).insert(any());
    }

    @Test
    void placeBid_lowerOrEqualCurrentBid_shouldReturnBidTooLow() {
        Long auctionId = 1L;
        Auction auction = buildRunningAuction(auctionId, 100.0, null);

        when(auctionDAO.findById(auctionId)).thenReturn(auction);

        AuctionService.BidResult result = auctionService.placeBid(auctionId, "bidder1@mail.com", 90.0);

        assertFalse(result.success());
        assertEquals("BID_TOO_LOW", result.code());

        verify(userDAO, never()).findByEmail(any());
        verify(auctionDAO, never()).updateCurrentBid(anyLong(), any(), any());
        verify(bidDAO, never()).insert(any());
    }

    @Test
    void placeBid_insufficientFunds_shouldReturnInsufficientAndRequiredTopUp() {
        Long auctionId = 1L;
        User bidder = buildUser(22L, "bidder2@mail.com", "Bidder 2");
        Auction auction = buildRunningAuction(auctionId, 100.0, null);
        Wallet wallet = buildWallet(22L, "90.00", "10.00"); // available = 80

        when(auctionDAO.findById(auctionId)).thenReturn(auction);
        when(userDAO.findByEmail("bidder2@mail.com")).thenReturn(Optional.of(bidder));
        when(walletDAO.findOrCreateByUserId(22L)).thenReturn(wallet);

        AuctionService.BidResult result = auctionService.placeBid(auctionId, bidder.getEmail(), 120.0);

        assertFalse(result.success());
        assertEquals("INSUFFICIENT_FUNDS", result.code());
        assertEquals(40.0, result.requiredTopUp(), 0.0001);

        verify(auctionDAO, never()).updateCurrentBid(anyLong(), any(), any());
        verify(bidDAO, never()).insert(any());
    }

    @Test
    void placeBid_concurrency_shouldKeepMaxBidAndNoNegativeReserved() throws Exception {
        Long auctionId = 1L;
        Auction sharedAuction = buildRunningAuction(auctionId, 100.0, null);
        AtomicReference<BigDecimal> maxBid = new AtomicReference<>(BigDecimal.valueOf(100));

        Map<Long, Wallet> wallets = new ConcurrentHashMap<>();
        int n = 20;
        for (long i = 1; i <= n; i++) {
            wallets.put(i, buildWallet(i, "10000.00", "0.00"));
        }

        when(auctionDAO.findById(auctionId)).thenReturn(sharedAuction);
        when(walletDAO.findOrCreateByUserId(anyLong())).thenAnswer(inv -> wallets.get(inv.getArgument(0)));
        when(walletDAO.updateBalances(anyLong(), any(), any())).thenAnswer(inv -> {
            Long uid = inv.getArgument(0);
            BigDecimal bal = inv.getArgument(1);
            BigDecimal res = inv.getArgument(2);
            Wallet w = wallets.get(uid);
            w.setBalance(bal);
            w.setReserved(res);
            return true;
        });
        when(userDAO.findByEmail(any())).thenAnswer(inv -> {
            String email = inv.getArgument(0);
            long id = Long.parseLong(email.replace("user", "").replace("@mail.com", ""));
            return Optional.of(buildUser(id, email, "User " + id));
        });
        when(auctionDAO.updateCurrentBid(eq(auctionId), any(), anyLong())).thenAnswer(inv -> {
            BigDecimal bid = inv.getArgument(1);
            synchronized (sharedAuction) {
                if (bid.compareTo(maxBid.get()) > 0) {
                    maxBid.set(bid);
                    sharedAuction.setCurrentBid(bid);
                    sharedAuction.setHighestBidderId(inv.getArgument(2));
                }
            }
            return true;
        });

        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch done = new CountDownLatch(n);

        for (int i = 1; i <= n; i++) {
            final int bid = 100 + i;
            final String email = "user" + i + "@mail.com";
            pool.submit(() -> {
                try {
                    auctionService.placeBid(auctionId, email, bid);
                } finally {
                    done.countDown();
                }
            });
        }

        assertTrue(done.await(20, TimeUnit.SECONDS));
        pool.shutdownNow();

        assertEquals(120.0, maxBid.get().doubleValue(), 0.0001);
        assertEquals(120.0, sharedAuction.getCurrentBid().doubleValue(), 0.0001);
        for (Wallet wallet : wallets.values()) {
            assertTrue(wallet.getReserved().compareTo(BigDecimal.ZERO) >= 0);
        }
    }

    private Auction buildRunningAuction(Long id, double currentBid, Long highestBidderId) {
        Auction auction = new Auction();
        auction.setId(id);
        auction.setItemId(99L);
        auction.setTitle("Auction " + id);
        auction.setDescription("Test");
        auction.setStartingBid(BigDecimal.valueOf(100.0));
        auction.setCurrentBid(BigDecimal.valueOf(currentBid));
        auction.setStatus("RUNNING");
        auction.setHighestBidderId(highestBidderId);
        auction.setStartTime(LocalDateTime.now().minusMinutes(10));
        auction.setEndTime(LocalDateTime.now().plusMinutes(30));
        return auction;
    }

    private User buildUser(Long id, String email, String fullName) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setFullName(fullName);
        return user;
    }

    private Wallet buildWallet(Long userId, String balance, String reserved) {
        Wallet wallet = new Wallet();
        wallet.setUserId(userId);
        wallet.setBalance(new BigDecimal(balance));
        wallet.setReserved(new BigDecimal(reserved));
        wallet.setUpdatedAt(LocalDateTime.now());
        return wallet;
    }
}
