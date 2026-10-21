package btd.util;

import btd.dao.PlayerDAO;

import java.sql.SQLException;

/** Read-only check that the database connection and PlayerDAO work. Writes nothing. */
public class DbCheck {

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
