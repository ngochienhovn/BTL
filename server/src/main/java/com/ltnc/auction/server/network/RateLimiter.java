package com.ltnc.auction.server.network;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Token Bucket Rate Limiter.
 */
public class RateLimiter {
    private final long capacity;
    private final long refillRatePerMs;
    private final AtomicLong tokens;
    private final AtomicLong lastRefillTimestamp;

    public RateLimiter(long tokensPerSecond) {
        this.capacity = tokensPerSecond;
        this.refillRatePerMs = tokensPerSecond; // Simplify: rate = capacity per second
        this.tokens = new AtomicLong(tokensPerSecond);
        this.lastRefillTimestamp = new AtomicLong(System.currentTimeMillis());
    }

    public boolean tryAcquire() {
        refill();
        long currentTokens = tokens.get();
        while (currentTokens > 0) {
            if (tokens.compareAndSet(currentTokens, currentTokens - 1)) {
                return true;
            }
            currentTokens = tokens.get();
        }
        return false;
    }

    private void refill() {
        long now = System.currentTimeMillis();
        long last = lastRefillTimestamp.get();
        long deltaMs = now - last;
        
        if (deltaMs > 0) {
            long newTokens = (deltaMs * capacity) / 1000;
            if (newTokens > 0) {
                if (lastRefillTimestamp.compareAndSet(last, now)) {
                    tokens.updateAndGet(t -> Math.min(capacity, t + newTokens));
                }
            }
        }
    }
}
