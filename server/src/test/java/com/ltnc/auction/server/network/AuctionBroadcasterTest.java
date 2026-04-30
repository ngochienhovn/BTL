package com.ltnc.auction.server.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;


public class AuctionBroadcasterTest {

    @Test
    void testBroadcasterWithManyListeners() {
        AuctionBroadcaster broadcaster = new AuctionBroadcaster();

        for (int i = 0; i < 100; i++) {
            broadcaster.broadcast("AUCTION_UPDATE client-" + i);
        }

        assertEquals(100, broadcaster.getMessages().size());
        assertTrue(broadcaster.getMessages().contains("AUCTION_UPDATE client-0"));
        assertTrue(broadcaster.getMessages().contains("AUCTION_UPDATE client-99"));
    }

    @Test
    void testBroadcasterListenerCleanup() {
        AuctionBroadcaster broadcaster = new AuctionBroadcaster();

        ClientHandler client = mock(ClientHandler.class);

        broadcaster.registerListener(client);
        broadcaster.unregisterListener(client);
        broadcaster.broadcastAuctionStateChange(1L, "RUNNING");

<<<<<<< Updated upstream
        broadcaster.clear();

        assertEquals(0, broadcaster.getMessages().size());
        assertTrue(broadcaster.getMessages().isEmpty());
=======
        verify(client, never()).sendBroadcast(any());
>>>>>>> Stashed changes
    }

    @Test
    void testBroadcasterPerformance() {
        AuctionBroadcaster broadcaster = new AuctionBroadcaster();

        List<String> messages = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            messages.add("AUCTION_UPDATE client-" + i);
        }

        long startTime = System.nanoTime();

        for (String message : messages) {
            broadcaster.broadcast(message);
        }

        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000;

        assertTrue(
                durationMs < 100,
                "Broadcast 100 messages phải dưới 100ms, thực tế: " + durationMs + "ms"
        );

        assertEquals(100, broadcaster.getMessages().size());
        assertTrue(broadcaster.getMessages().contains("AUCTION_UPDATE client-0"));
        assertTrue(broadcaster.getMessages().contains("AUCTION_UPDATE client-99"));
    }
}