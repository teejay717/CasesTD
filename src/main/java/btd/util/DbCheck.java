package btd.util;

import btd.dao.PlayerDAO;

import java.sql.SQLException;

/**
 * [PURPOSE]       Independent console verification utility to test database connectivity and PlayerDAO queries.
 * [OOP / PATTERN] Diagnostic Harness / Smoke Test / Separation of Concerns.
 * [CALLED BY]     Developers and system administrators via command line.
 * [CONNECTS TO]   btd/dao/PlayerDAO.java, btd/config/DatabaseConnection.java.
 * [DEFENSE NOTE]  Read-only verification tool that issues SELECT queries without mutating database records.
 */
public class DbCheck {

    /**
     * [PURPOSE]       Executes read-only queries testing player retrieval by name (both existing and non-existent).
     * [OOP / PATTERN] Main Entry Point / Smoke Test.
     * [CALLED BY]     Command line execution.
     */
    public static void main(String[] args) {
        try {
            PlayerDAO dao = new PlayerDAO();
            System.out.println("Sam             -> " + dao.findByName("Sam").map(Object::toString).orElse("not found"));
            System.out.println("sam             -> " + dao.findByName("sam").map(Object::toString).orElse("not found"));
            System.out.println("NoSuchPlayer123 -> " + dao.findByName("NoSuchPlayer123").map(Object::toString).orElse("not found"));
        } catch (SQLException | IllegalStateException e) {
            System.err.println("Database check failed: " + e.getMessage());
        }
    }
}
