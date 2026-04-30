package com.ltnc.auction.server.services;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.ltnc.auction.server.dao.AuctionDAO;
import com.ltnc.auction.server.dao.BidDAO;
import com.ltnc.auction.server.dao.UserDAO;
import com.ltnc.auction.server.dao.WalletDAO;
import com.ltnc.auction.server.dao.WalletTransactionDAO;
import com.ltnc.auction.server.model.Auction;
import com.ltnc.auction.server.model.User;
import com.ltnc.auction.server.model.Wallet;
import com.ltnc.auction.server.network.AuctionBroadcaster;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuctionServiceBroadcastTest {

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
                walletTransactionDAO,
                broadcaster
        );
    }

    @Test
    void testBidBroadcastsToAllClients() 
    {
        Long auctionId = 1L;
        User user = buildUser(2L, "user@mail.com", "User Test");
        Long bidderId = user.getId();
        Auction auction = buildRunningAuction(auctionId, 100.0, null);
        Wallet wallet = buildWallet(2L, "1000.00", "0.00");

        when(auctionDAO.findById(auctionId)).thenReturn(auction);
        when(userDAO.findByEmail("user@mail.com")).thenReturn(Optional.of(user));
        when(walletDAO.findOrCreateByUserId(2L)).thenReturn(wallet);
        when(walletDAO.updateBalances(eq(2L), any(), any())).thenReturn(true);

        AuctionService.BidResult result = auctionService.placeBid(
                auctionId,
                user.getEmail(),
                150.0
        );

        assertTrue(result.success());

        verify(broadcaster, atLeastOnce()).broadcastAuctionUpdate(eq(auctionId), any());
        verify(broadcaster, atLeastOnce()).broadcastWalletUpdate(eq(bidderId), any());
        verify(bidDAO, times(1)).insert(any());
        verify(walletTransactionDAO, atLeastOnce()).insert(any());
    }

    @Test
    void testBroadcastIncludesCorrectData() {
        Long auctionId = 1L;
        User user = buildUser(3L, "bidder@mail.com", "Bidder Test");
        Auction auction = buildRunningAuction(auctionId, 100.0, null);
        Wallet wallet = buildWallet(3L, "1000.00", "0.00");

        when(auctionDAO.findById(auctionId)).thenReturn(auction);
        when(userDAO.findByEmail("bidder@mail.com")).thenReturn(Optional.of(user));
        when(walletDAO.findOrCreateByUserId(3L)).thenReturn(wallet);
        when(walletDAO.updateBalances(eq(3L), any(), any())).thenReturn(true);

        AuctionService.BidResult result = auctionService.placeBid(
                auctionId,
                user.getEmail(),
                200.0
        );

        assertTrue(result.success());

        verify(broadcaster, atLeastOnce()).broadcastAuctionUpdate(eq(auctionId), any());
        verify(broadcaster, atLeastOnce()).broadcastWalletUpdate(eq(user.getId()), any());

        assertTrue(
                payloads.stream().anyMatch(payload -> payload.contains("AUCTION_UPDATE")),
                "Broadcast phải có message AUCTION_UPDATE"
        );

        assertTrue(
                payloads.stream().anyMatch(payload -> payload.contains(String.valueOf(auctionId))),
                "Broadcast phải chứa auctionId"
        );

        assertTrue(
                payloads.stream().anyMatch(payload -> payload.contains("200")),
                "Broadcast phải chứa currentBid mới là 200"
        );

        assertTrue(
                payloads.stream().anyMatch(payload -> payload.contains(user.getEmail())
                        || payload.contains(user.getFullName())
                        || payload.contains(String.valueOf(user.getId()))),
                "Broadcast nên chứa thông tin bidder"
        );
    }

    @Test
    void testWalletBroadcastOnBid() {
        Long auctionId = 1L;
        User user = buildUser(4L, "wallet@mail.com", "Wallet User");
        Auction auction = buildRunningAuction(auctionId, 100.0, null);
        Wallet wallet = buildWallet(4L, "1000.00", "0.00");

        when(auctionDAO.findById(auctionId)).thenReturn(auction);
        when(userDAO.findByEmail("wallet@mail.com")).thenReturn(Optional.of(user));
        when(walletDAO.findOrCreateByUserId(4L)).thenReturn(wallet);
        when(walletDAO.updateBalances(eq(4L), any(), any())).thenReturn(true);

        AuctionService.BidResult result = auctionService.placeBid(
                auctionId,
                user.getEmail(),
                250.0
        );

        assertTrue(result.success());

        verify(broadcaster, atLeastOnce()).broadcastAuctionUpdate(eq(auctionId), any());
        verify(broadcaster, atLeastOnce()).broadcastWalletUpdate(eq(user.getId()), any());

        assertTrue(
                payloads.stream().anyMatch(payload -> payload.contains("WALLET_UPDATE")
                        || payload.contains("wallet")
                        || payload.contains("Wallet")),
                "Broadcast phải có message cập nhật ví"
        );

        assertTrue(
                payloads.stream().anyMatch(payload -> payload.contains(String.valueOf(user.getId()))
                        || payload.contains(user.getEmail())),
                "Broadcast ví phải chứa thông tin user"
        );

        verify(walletDAO, atLeastOnce()).updateBalances(eq(4L), any(), any());
        verify(walletTransactionDAO, atLeastOnce()).insert(any());
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