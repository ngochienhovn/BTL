package com.ltnc.auction.server.service;

import com.google.gson.Gson;
import com.ltnc.auction.server.network.ClientSession;
import com.ltnc.auction.shared.protocol.ServerToClientMessage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread-safe broadcast hub: all clients for auction updates; targeted sends for wallet updates.
 * Now using Asynchronous Broadcasting to prevent slow sockets from blocking the core auction logic.
 */
public class BroadcastManager {
    private static final Logger LOG = LoggerFactory.getLogger(BroadcastManager.class);
    private static volatile BroadcastManager instance;
    private final CopyOnWriteArrayList<ClientSession> sessions = new CopyOnWriteArrayList<>();
    private final Gson gson = new Gson();
    
    // Asynchronous Broadcast Pool to prevent slow clients from blocking the server
    private final ExecutorService asyncExecutor = Executors.newFixedThreadPool(16);
    private final ScheduledExecutorService sweeper;

    private final AtomicLong broadcastAllCount = new AtomicLong();
    private final AtomicLong broadcastUserCount = new AtomicLong();
    private final AtomicLong failedSends = new AtomicLong();
    private final AtomicLong totalLatencyNanos = new AtomicLong();

    private BroadcastManager() {
        // Start PingSweepTask to cleanup dead connections
        sweeper = Executors.newSingleThreadScheduledExecutor();
        sweeper.scheduleAtFixedRate(this::sweepDeadConnections, 30, 30, TimeUnit.SECONDS);
    }

    private void sweepDeadConnections() {
        long now = System.currentTimeMillis();
        long timeoutMs = 45000; // 45 seconds without any message (including PING) means dead
        int removedCount = 0;
        for (ClientSession s : sessions) {
            if (now - s.lastActiveTime > timeoutMs) {
                try {
                    s.socket.close();
                } catch (Exception ignored) {}
                sessions.remove(s);
                removedCount++;
            }
        }
        if (removedCount > 0) {
            LOG.info("Swept {} dead connections. Active sessions left: {}", removedCount, sessions.size());
        }
    }

    public static BroadcastManager getInstance() {
        if (instance == null) {
            synchronized (BroadcastManager.class) {
                if (instance == null) {
                    instance = new BroadcastManager();
                }
            }
        }
        return instance;
    }

    public void register(ClientSession session) {
        sessions.add(session);
    }

    public void unregister(ClientSession session) {
        sessions.remove(session);
    }

    public void broadcastAll(ServerToClientMessage msg) {
        long start = System.nanoTime();
        String json = gson.toJson(msg);
        for (ClientSession s : sessions) {
            asyncExecutor.submit(() -> sendLine(s, json));
        }
        broadcastAllCount.incrementAndGet();
        totalLatencyNanos.addAndGet(System.nanoTime() - start);
    }

    /** Push wallet state to every connected session for this user (multiple tabs). */
    public void broadcastToUser(Long userId, ServerToClientMessage msg) {
        if (userId == null) {
            return;
        }
        long start = System.nanoTime();
        String json = gson.toJson(msg);
        for (ClientSession s : sessions) {
            if (userId.equals(s.userId)) {
                asyncExecutor.submit(() -> sendLine(s, json));
            }
        }
        broadcastUserCount.incrementAndGet();
        totalLatencyNanos.addAndGet(System.nanoTime() - start);
    }

    private void sendLine(ClientSession s, String json) {
        try {
            s.writer.println(json);
            if (s.writer.checkError()) {
                sessions.remove(s);
                failedSends.incrementAndGet();
            }
        } catch (Exception e) {
            sessions.remove(s);
            failedSends.incrementAndGet();
        }
    }

    public long getBroadcastAllCount() {
        return broadcastAllCount.get();
    }

    public long getBroadcastUserCount() {
        return broadcastUserCount.get();
    }

    public long getFailedSends() {
        return failedSends.get();
    }

    public double getAverageLatencyMs() {
        long n = broadcastAllCount.get() + broadcastUserCount.get();
        if (n == 0) {
            return 0;
        }
        return totalLatencyNanos.get() / 1_000_000.0 / n;
    }

    public void shutdown() {
        LOG.info("Shutting down BroadcastManager...");
        try {
            if (sweeper != null) {
                sweeper.shutdownNow();
            }
            asyncExecutor.shutdown();
            if (!asyncExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                asyncExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            asyncExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
