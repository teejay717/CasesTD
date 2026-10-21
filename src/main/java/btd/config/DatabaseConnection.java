package btd.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Singleton that owns the one shared JDBC connection for the whole application.
 * Credentials are read from data/db_config.properties (git-ignored), so run the
 * application from the project root folder.
 */
public final class DatabaseConnection {

    private static final Path CONFIG_PATH = Paths.get("data", "db_config.properties");

    private static DatabaseConnection instance;

    private final String url;
    private final String user;
    private final String password;
    private Connection connection;

    private DatabaseConnection() {
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(CONFIG_PATH)) {
            props.load(in);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Could not read " + CONFIG_PATH.toAbsolutePath()
                            + ". Copy data/db_config.example.properties to data/db_config.properties"
                            + " and run the application from the project root.", e);
        }
        this.url = props.getProperty("db.url");
        this.user = props.getProperty("db.user");
        this.password = props.getProperty("db.password", "");
        if (url == null || user == null) {
            throw new IllegalStateException("db.url and db.user must be set in " + CONFIG_PATH);
        }
    }

    /** Returns the single instance, creating it on first use. */
    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    /** Returns the shared connection, opening a new one if it was never opened or has dropped. */
    public synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed() || !connection.isValid(2)) {
            connection = DriverManager.getConnection(url, user, password);
        }
        return connection;
    }

    /** Closes the shared connection, for example when the application exits. */
    public synchronized void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
                // nothing useful to do while shutting down
            }
            connection = null;
        }
    }
}
