package btd.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * [PURPOSE]       Manages the single shared JDBC connection to the MySQL database (btd_case_db).
 * [OOP / PATTERN] Singleton Pattern (Lazy Initialization with synchronized access).
 * [CALLED BY]     btd/dao/PlayerDAO.java, btd/dao/TowerDAO.java, btd/dao/CaseDAO.java, btd/util/DbCheck.java.
 * [CONNECTS TO]   data/db_config.properties (reads db.url, db.user, db.password), MySQL Server.
 * [DEFENSE NOTE]  Prevents socket exhaustion and latency spikes by reusing a single Connection across all DAOs.
 */
public class DatabaseConnection {

    private static DatabaseConnection instance;

    private final String url;
    private final String user;
    private final String password;
    private Connection connection;

    /**
     * [PURPOSE]       Private constructor reading database credentials from properties file.
     * [OOP / PATTERN] Encapsulation / Private Construction (Singleton Enforcement).
     * [CALLED BY]     getInstance() internally.
     * [CONNECTS TO]   data/db_config.properties via FileInputStream.
     * [DEFENSE NOTE]  Throws IllegalStateException with a descriptive message if the config file is missing.
     */
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

    /**
     * [PURPOSE]       Global access point returning the unique singleton instance.
     * [OOP / PATTERN] Singleton Pattern (Thread-safe synchronized accessor).
     * [CALLED BY]     btd/dao/PlayerDAO.java, btd/dao/TowerDAO.java, btd/dao/CaseDAO.java.
     * [DEFENSE NOTE]  Synchronized keyword prevents multi-threaded race conditions during initial instantiation.
     */
    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    /**
     * [PURPOSE]       Returns the active SQL Connection, establishing or reconnecting if closed.
     * [OOP / PATTERN] Lazy Reconnection / Resource Management.
     * [CALLED BY]     All DAO query methods.
     * [CONNECTS TO]   java.sql.DriverManager.
     * [DEFENSE NOTE]  Do NOT close this connection in DAO try-with-resources blocks; closing it breaks other DAOs.
     */
    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(url, user, password);
        }
        return connection;
    }
}
