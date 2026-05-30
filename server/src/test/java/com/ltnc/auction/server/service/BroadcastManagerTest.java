package com.ltnc.auction.server.service;

import com.ltnc.auction.server.network.ClientSession;
import com.ltnc.auction.shared.protocol.MessageType;
import com.ltnc.auction.shared.protocol.ServerToClientMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

public class BroadcastManagerTest {

    @BeforeEach
    void resetSingleton() throws Exception {
        Field f = BroadcastManager.class.getDeclaredField("instance");
        f.setAccessible(true);
        f.set(null, null);
    }

    @Test
    void broadcastAll_reachesRegisteredSessions() {
        BroadcastManager bm = BroadcastManager.getInstance();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw, true);
        bm.register(new ClientSession(null, pw));

        ServerToClientMessage msg = new ServerToClientMessage();
        msg.type = MessageType.AUCTION_UPDATE;
        msg.eventType = "AUCTION_UPDATE";
        bm.broadcastAll(msg);

        try { Thread.sleep(100); } catch (Exception ignored) {}
        pw.flush();
        assertTrue(sw.toString().contains("AUCTION_UPDATE"));
        assertTrue(bm.getBroadcastAllCount() >= 1);
    }

    @Test
    void broadcastToUser_onlyMatchingUserId() {
        BroadcastManager bm = BroadcastManager.getInstance();

        StringWriter sw1 = new StringWriter();
        ClientSession s1 = new ClientSession(null, new PrintWriter(sw1, true));
        s1.userId = 1L;

        StringWriter sw2 = new StringWriter();
        ClientSession s2 = new ClientSession(null, new PrintWriter(sw2, true));
        s2.userId = 2L;

        bm.register(s1);
        bm.register(s2);

        ServerToClientMessage msg = new ServerToClientMessage();
        msg.type = MessageType.WALLET_UPDATE;
        bm.broadcastToUser(1L, msg);

        try { Thread.sleep(100); } catch (Exception ignored) {}
        assertTrue(sw1.toString().contains("WALLET_UPDATE"));
        assertFalse(sw2.toString().contains("WALLET_UPDATE"));
    }
}
