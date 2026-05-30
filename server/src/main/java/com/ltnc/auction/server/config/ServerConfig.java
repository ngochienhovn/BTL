package com.ltnc.auction.server.config;

/**
 * Cấu hình runtime server.
 *
 * <p>Thứ tự cổng lắng nghe: tham số dòng lệnh {@code args[0]} (nếu có),
 * sau đó {@code AUCTION_SERVER_PORT}, cuối cùng {@code 9090}.</p>
 */
public final class ServerConfig {

    private ServerConfig() {}

    public static int resolveListenPort(String[] args) {
        if (args != null && args.length >= 1 && args[0] != null && !args[0].isBlank()) {
            return parsePort(args[0].trim(), 9090);
        }
        return parsePort(System.getenv("AUCTION_SERVER_PORT"), 9090);
    }

    private static int parsePort(String raw, int defaultPort) {
        if (raw == null || raw.isBlank()) {
            return defaultPort;
        }
        try {
            int p = Integer.parseInt(raw.trim());
            if (p >= 1 && p <= 65535) {
                return p;
            }
        } catch (NumberFormatException ignored) {
            // fall through
        }
        return defaultPort;
    }
}
