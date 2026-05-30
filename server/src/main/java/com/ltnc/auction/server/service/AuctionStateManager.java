package com.ltnc.auction.server.service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Quản lý chuyển trạng thái phiên theo thời gian (OPEN→RUNNING→FINISHED) qua {@link AuctionService#tick()}.
 */
public class AuctionStateManager {
    private static final Logger LOG = LoggerFactory.getLogger(AuctionStateManager.class);

    private static volatile AuctionStateManager instance;
    private ScheduledExecutorService scheduler;

    private AuctionStateManager() {}

    public static AuctionStateManager getInstance() {
        if (instance == null) {
            synchronized (AuctionStateManager.class) {
                if (instance == null) {
                    instance = new AuctionStateManager();
                }
            }
        }
        return instance;
    }

    public void start() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "auction-state-manager");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(() -> {
            try {
                AuctionService.getInstance().tick();
            } catch (Exception e) {
                LOG.warn("AuctionStateManager tick error", e);
            }
        }, 1, 1, TimeUnit.SECONDS);
        LOG.info("AuctionStateManager started (1s tick)");
    }

    public void stop() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }
}
