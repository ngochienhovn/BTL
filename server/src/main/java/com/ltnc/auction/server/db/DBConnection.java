package com.ltnc.auction.server.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class DBConnection {
    private static final String URL = "jdbc:h2:./data/auction_db;AUTO_SERVER=FALSE";
    private static final String USER = "sa";
    private static final String PASS = "";

    private static final ThreadLocal<Connection> threadConnection = new ThreadLocal<>();
    private static final ThreadLocal<Integer> transactionDepth = ThreadLocal.withInitial(() -> 0);
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(URL);
        config.setUsername(USER);
        config.setPassword(PASS);
        
        // Performance optimizations for H2/General
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(30000);
        config.setConnectionTimeout(5000);
        config.setPoolName("BidMasterHiPool");
        
        dataSource = new HikariDataSource(config);
    }

    private DBConnection() {}

    public static Connection getConnection() throws SQLException {
        Connection conn = threadConnection.get();
        if (conn != null && !conn.isClosed()) {
            return (Connection) java.lang.reflect.Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    if ("close".equals(method.getName())) {
                        return null; // Ignore close during transaction
                    }
                    try {
                        return method.invoke(conn, args);
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        throw e.getCause();
                    }
                }
            );
        }
        return dataSource.getConnection();
    }

    public static void startTransaction() throws SQLException {
        int depth = transactionDepth.get();
        if (depth == 0) {
            Connection conn = dataSource.getConnection();
            conn.setAutoCommit(false);
            threadConnection.set(conn);
        }
        transactionDepth.set(depth + 1);
    }

    public static void commitTransaction() throws SQLException {
        int depth = transactionDepth.get();
        if (depth > 1) {
            transactionDepth.set(depth - 1);
            return;
        }
        Connection conn = threadConnection.get();
        if (conn != null) {
            try {
                conn.commit();
            } finally {
                closeThreadConnection();
            }
        }
    }

    public static void rollbackTransaction() {
        transactionDepth.remove();
        Connection conn = threadConnection.get();
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException ignored) {
            } finally {
                closeThreadConnection();
            }
        }
    }

    private static void closeThreadConnection() {
        Connection conn = threadConnection.get();
        threadConnection.remove();
        transactionDepth.remove();
        if (conn != null) {
            try {
                if (!conn.isClosed()) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException ignored) {}
        }
    }

    public static void closePool() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}

