package btd.util;

import btd.dao.PlayerDAO;

import java.sql.SQLException;

/**
 * [PURPOSE]       Separate little program (its own main()) to check that MySQL works, without starting the game.
 * [OOP / PATTERN] Smoke Test / separate entry point.
 * [CALLED BY]     A developer running it by hand. The game itself never calls it.
 * [CONNECTS TO]   btd/dao/PlayerDAO.java, btd/config/DatabaseConnection.java.
 * [DEFENSE NOTE]  Read-only (only SELECTs). It looks up "Sam", "sam" (to show the search is not case-sensitive),
 *                 and a name that doesn't exist (to show Optional.empty() -> "not found").
 */
public class DbCheck {

    /**
     * [PURPOSE]       Executes read-only queries testing player retrieval by name (both existing and non-existent).
     * [OOP / PATTERN] Main Entry Point / Smoke Test.
     * [CALLED BY]     Run by hand (e.g. from the IDE's Run button on this file).
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
