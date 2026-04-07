package src.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection 
{
    private static final String URL = "jdbc:h2:./data/auction_db;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1";
    private static final String USER = "team";
    private static final String PASSWORD = "";

    public DBConnection() {}

    public static Connection getConnection() throws SQLException
    {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
