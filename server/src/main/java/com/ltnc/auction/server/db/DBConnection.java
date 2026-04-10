package com.ltnc.auction.server.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private static final String URL = "jdbc:h2:./data/auction_db;AUTO_SERVER=FALSE";
    private static final String USER = "sa";
    private static final String PASS = "";

    private DBConnection() {}

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}
