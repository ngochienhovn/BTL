package com.ltnc.auction.server.network;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.ltnc.auction.server.model.Auction;
import com.ltnc.auction.shared.protocol.ServerToClientMessage;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

public class AuctionBroadcasterTest {

    @Test
    void testBroadcasterWithManyListeners() {
        AuctionBroadcaster broadcaster = new AuctionBroadcaster();
        Auction auction = buildAuction(1L, 123.0, 10L, "RUNNING");

        ClientHandler[] listeners = new ClientHandler[100];
        for (int i = 0; i < 100; i++) {
            listeners[i] = mock(ClientHandler.class);
            broadcaster.registerListener(listeners[i]);
        }

        broadcaster.broadcastAuctionUpdate(auction.getId(), auction);

        for (ClientHandler listener : listeners) {
            verify(listener, times(1)).sendBroadcast(any(ServerToClientMessage.class));
        }
    }

    @Test
    void testBroadcasterListenerCleanup() {
        AuctionBroadcaster broadcaster = new AuctionBroadcaster();

        ClientHandler client = mock(ClientHandler.class);

        broadcaster.registerListener(client);
        broadcaster.unregisterListener(client);
        broadcaster.broadcastAuctionStateChange(1L, "RUNNING");

        verify(client, never()).sendBroadcast(any());
    }

    @Test
    void testBroadcasterPerformance() {
        AuctionBroadcaster broadcaster = new AuctionBroadcaster();
        Auction auction = buildAuction(1L, 200.0, 2L, "RUNNING");

        ClientHandler[] listeners = new ClientHandler[100];
        for (int i = 0; i < 100; i++) {
            listeners[i] = mock(ClientHandler.class);
            broadcaster.registerListener(listeners[i]);
        }

        long startTime = System.currentTimeMillis();
        broadcaster.broadcastAuctionUpdate(auction.getId(), auction);

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        assertTrue(duration < 100, "Broadcast 100 messages phải dưới 100ms, thực tế: " + duration + "ms");
        for (ClientHandler listener : listeners) {
            verify(listener, times(1)).sendBroadcast(any(ServerToClientMessage.class));
        }
    }

    private Auction buildAuction(Long id, double currentBid, Long highestBidderId, String status) {
        Auction auction = new Auction();
        auction.setId(id);
        auction.setCurrentBid(BigDecimal.valueOf(currentBid));
        auction.setHighestBidderId(highestBidderId);
        auction.setStatus(status);
        return auction;
    }
}