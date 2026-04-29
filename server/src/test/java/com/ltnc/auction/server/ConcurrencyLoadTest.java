package com.ltnc.auction.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
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
import com.ltnc.auction.server.network.AuctionBroadcaster;
import com.ltnc.auction.server.services.AuctionService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConcurrencyLoadTest {

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

    @Mock
    private AuctionBroadcaster broadcaster;

    private AuctionService auctionService;

    @BeforeEach
    void setUp() {
        auctionService = new AuctionService(
                auctionDAO,
                bidDAO,
                userDAO,
                walletDAO,
                walletTransactionDAO
        );

        auctionService.setBroadcaster(broadcaster);
    }

    @Test
    void testConcurrentBidsWithBroadcasts() throws Exception {
        Long auctionId = 1L;

        Auction sharedAuction = buildRunningAuction(auctionId, 100.0, null);
        AtomicReference<BigDecimal> maxBid = new AtomicReference<>(BigDecimal.valueOf(100.0));
        AtomicReference<Exception> threadError = new AtomicReference<>();

        Map<Long, Wallet> wallets = new ConcurrentHashMap<>();

        int threadCount = 50;

        for (long i = 1; i <= threadCount; i++) {
            wallets.put(i, buildWallet(i, "10000.00", "0.00"));
        }

        when(auctionDAO.findById(auctionId)).thenReturn(sharedAuction);

        when(userDAO.findByEmail(any())).thenAnswer(invocation -> {
            String email = invocation.getArgument(0);

            long userId = Long.parseLong(
                    email.replace("user", "").replace("@mail.com", "")
            );

            return Optional.of(buildUser(userId, email, "User " + userId));
        });

        when(walletDAO.findOrCreateByUserId(anyLong())).thenAnswer(invocation -> {
            Long userId = invocation.getArgument(0);
            return wallets.get(userId);
        });

        when(walletDAO.updateBalances(anyLong(), any(), any())).thenAnswer(invocation -> {
            Long userId = invocation.getArgument(0);
            BigDecimal newBalance = invocation.getArgument(1);
            BigDecimal newReserved = invocation.getArgument(2);

            Wallet wallet = wallets.get(userId);
            wallet.setBalance(newBalance);
            wallet.setReserved(newReserved);

            return true;
        });

        when(auctionDAO.updateCurrentBid(eq(auctionId), any(), anyLong())).thenAnswer(invocation -> {
            BigDecimal newBid = invocation.getArgument(1);
            Long highestBidderId = invocation.getArgument(2);

            synchronized (sharedAuction) {
                if (newBid.compareTo(maxBid.get()) > 0) {
                    maxBid.set(newBid);
                    sharedAuction.setCurrentBid(newBid);
                    sharedAuction.setHighestBidderId(highestBidderId);
                }
            }

            return true;
        });

        ExecutorService pool = Executors.newFixedThreadPool(10);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        long startTime = System.nanoTime();

        for (int i = 1; i <= threadCount; i++) {
            final int userNumber = i;
            final String email = "user" + userNumber + "@mail.com";
            final double bidAmount = 100.0 + userNumber;

            pool.submit(() -> {
                try {
                    startLatch.await();
                    auctionService.placeBid(auctionId, email, bidAmount);
                } catch (Exception e) {
                    threadError.compareAndSet(null, e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();

        boolean completed = doneLatch.await(15, TimeUnit.SECONDS);

        pool.shutdownNow();

        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);

        assertTrue(completed, "Test bị timeout, có thể đang bị deadlock");
        assertTrue(durationMs < 15000, "Test phải hoàn thành dưới 15 giây");
        assertNull(threadError.get(), "Không được có exception trong thread");

        assertEquals(150.0, maxBid.get().doubleValue(), 0.0001);
        assertEquals(150.0, sharedAuction.getCurrentBid().doubleValue(), 0.0001);

        for (Wallet wallet : wallets.values()) {
            assertTrue(
                    wallet.getReserved().compareTo(BigDecimal.ZERO) >= 0,
                    "Reserved money không được âm"
            );
        }

        verify(broadcaster, atLeastOnce()).broadcast(any());

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(broadcaster, atLeastOnce()).broadcast(captor.capture());

        List<String> payloads = captor.getAllValues();

        assertTrue(
                payloads.stream().anyMatch(payload -> payload.contains("AUCTION_UPDATE")),
                "Phải có ít nhất một broadcast AUCTION_UPDATE"
        );

        assertTrue(
                payloads.stream().anyMatch(payload -> payload.contains("150")),
                "Broadcast phải chứa giá cao nhất cuối cùng là 150"
        );
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