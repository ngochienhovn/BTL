package app.config;

/**
 * Cấu hình kết nối client — ưu tiên biến môi trường (chuẩn triển khai / container).
 *
 * <ul>
 *   <li>{@code AUCTION_SERVER_HOST} — mặc định {@code localhost}</li>
 *   <li>{@code AUCTION_SERVER_PORT} — mặc định {@code 9090}</li>
 *   <li>{@code AUCTION_SOCKET_TIMEOUT_MS} — timeout chờ response (1000–120000), mặc định 5000</li>
 * </ul>
 */
public final class ClientConfig {

    private ClientConfig() {}

    public static String getServerHost() {
        String h = System.getenv("AUCTION_SERVER_HOST");
        if (h == null || h.isBlank()) {
            return "localhost";
        }
        return h.trim();
    }

    public static int getServerPort() {
        return parsePort(System.getenv("AUCTION_SERVER_PORT"), 9090);
    }

    public static int getSocketTimeoutMs() {
        String raw = System.getenv("AUCTION_SOCKET_TIMEOUT_MS");
        if (raw == null || raw.isBlank()) {
            return 5000;
        }
        try {
            int v = Integer.parseInt(raw.trim());
            return Math.max(1000, Math.min(v, 120_000));
        } catch (NumberFormatException e) {
            return 5000;
        }
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
