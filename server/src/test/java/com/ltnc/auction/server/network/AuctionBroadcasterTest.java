package com.ltnc.auction.server.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

public class AuctionBroadcasterTest {

    @Test
    void testBroadcasterWithManyListeners() {
        AuctionBroadcaster broadcaster = new AuctionBroadcaster();

        for (int i = 0; i < 100; i++) {
            broadcaster.broadcast("client-" + i);
        }

        assertEquals(100, broadcaster.getMessages().size());
        assertTrue(broadcaster.getMessages().contains("client-0"));
        assertTrue(broadcaster.getMessages().contains("client-99"));
    }

    @Test
    void testBroadcasterListenerCleanup() {
        AuctionBroadcaster broadcaster = new AuctionBroadcaster();

        broadcaster.broadcast("message-1");
        broadcaster.broadcast("message-2");

        assertEquals(2, broadcaster.getMessages().size());

        broadcaster.clear();

        assertEquals(0, broadcaster.getMessages().size());
    }

    @Test
    void testBroadcasterPerformance() {
        AuctionBroadcaster broadcaster = new AuctionBroadcaster();

        List<String> messages = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            messages.add("AUCTION_UPDATE client-" + i);
        }

        long startTime = System.currentTimeMillis();

        for (String message : messages) {
            broadcaster.broadcast(message);
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        assertTrue(duration < 100, "Broadcast 100 messages phải dưới 100ms, thực tế: " + duration + "ms");
        assertEquals(100, broadcaster.getMessages().size());
    }
}