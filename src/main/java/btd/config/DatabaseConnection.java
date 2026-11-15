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
 * [CALLED BY]     btd/dao/PlayerDAO.java, btd/dao/TowerDAO.java, btd/dao/CaseDAO.java
 *                 (btd/util/DbCheck.java uses it indirectly through PlayerDAO).
 * [CONNECTS TO]   data/db_config.properties (reads db.url, db.user, db.password), MySQL Server.
 * [DEFENSE NOTE]  Only ONE object of this class ever exists, and all DAOs share its one Connection.
 *                 Opening a new MySQL connection is slow, so reusing one is faster.
 *                 The password is in db_config.properties (not in the code and not committed to git).
 */
public class DatabaseConnection {

    private static DatabaseConnection instance;

    private final String url;
    private final String user;
    private final String password;
    private Connection connection;

    /**
     * [PURPOSE]       Reads the DB url, user and password from the properties file. Does NOT connect yet.
     * [OOP / PATTERN] Private Constructor (Singleton: no other class can write "new DatabaseConnection()").
     * [CALLED BY]     getInstance() only, the first time.
     * [CONNECTS TO]   data/db_config.properties via FileInputStream.
     * [DEFENSE NOTE]  Throws IllegalStateException with a helpful message if the file is missing.
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
     * [PURPOSE]       Returns the one shared instance, creating it the first time (lazy initialization).
     * [OOP / PATTERN] Singleton (static instance + private constructor + static getInstance()).
     * [CALLED BY]     btd/dao/PlayerDAO.java, btd/dao/TowerDAO.java, btd/dao/CaseDAO.java.
     * [DEFENSE NOTE]  'synchronized' lets only one thread in at a time, so two threads can't both see
     *                 instance == null and create two objects.
     */
    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    /**
     * [PURPOSE]       Returns the open Connection. Connects the first time, and reconnects if it was closed.
     * [OOP / PATTERN] Lazy Connection / Resource Management.
     * [CALLED BY]     All DAO query methods (DatabaseConnection.getInstance().getConnection()).
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
