package btd.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Singleton that holds the one shared database connection.
 * The login comes from data/db_config.properties, so run the app from the project root.
 */
public class DatabaseConnection {

    private static DatabaseConnection instance;

    private final String url;
    private final String user;
    private final String password;
    private Connection connection;

    private DatabaseConnection() {
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream("data/db_config.properties")) {
            props.load(in);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Cannot read data/db_config.properties. Copy db_config.example.properties and run from the project root.", e);
        }
        url = props.getProperty("db.url");
        user = props.getProperty("db.user");
        password = props.getProperty("db.password", "");
    }

    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    /** Returns the shared connection. Do not close it; it is reused by every DAO. */
    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(url, user, password);
        }
        return connection;
    }
}
