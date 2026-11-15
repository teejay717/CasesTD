package btd.dao;

import btd.config.DatabaseConnection;
import btd.model.player.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * [PURPOSE]       Encapsulates all JDBC operations on the MySQL 'players' table.
 * [OOP / PATTERN] Data Access Object (DAO) Pattern / Separation of Concerns.
 * [CALLED BY]     btd/ui/screens/LandingPanel.java (openCrate(): findOrCreate()),
 *                 btd/ui/screens/GamePanel.java (finishRun(): updateHighestScore()), btd/util/DbCheck.java.
 * [CONNECTS TO]   btd/config/DatabaseConnection.java, MySQL 'players' table (player_id, player_name, highest_score).
 * [DEFENSE NOTE]  Completely isolates SQL from GUI screens; prevents SQL injection using PreparedStatement.
 */
public class PlayerDAO {

    /**
     * [PURPOSE]       Finds a player by name. Not case-sensitive ("sam" finds "Sam") because of MySQL's default collation.
     * [OOP / PATTERN] PreparedStatement (parameterized query) / Optional return.
     * [CALLED BY]     findOrCreate(), btd/util/DbCheck.java.
     * [CONNECTS TO]   btd/config/DatabaseConnection.java.
     * [DEFENSE NOTE]  Returns Optional.empty() instead of null when the player does not exist,
     *                 so the caller must handle "not found" and cannot get a NullPointerException by accident.
     */
    public Optional<Player> findByName(String name) throws SQLException {
        Connection conn = DatabaseConnection.getInstance().getConnection();
        String sql = "SELECT player_id, player_name, highest_score FROM players WHERE player_name = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Player(
                            rs.getInt("player_id"),
                            rs.getString("player_name"),
                            rs.getInt("highest_score")));
                }
                return Optional.empty();
            }
        }
    }

    /**
     * [PURPOSE]       Returns the player with this name, creating them first (INSERT) if they are new.
     * [OOP / PATTERN] Find-or-Create (calling it twice with the same name gives the same player).
     * [CALLED BY]     btd/ui/screens/LandingPanel.java (openCrate(), when the crate is clicked).
     * [CONNECTS TO]   btd/model/player/Player.java, MySQL 'players' table.
     * [DEFENSE NOTE]  "Login" with just a name, no password. New players start with highest_score = 0 (the column default).
     *                 After the INSERT it reads the row back to get the new auto-increment player_id.
     */
    public Player findOrCreate(String name) throws SQLException {
        Optional<Player> existing = findByName(name);
        if (existing.isPresent()) {
            return existing.get();
        }
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO players (player_name) VALUES (?)")) {
            ps.setString(1, name.trim());
            ps.executeUpdate();
        }
        return findByName(name).orElseThrow();
    }

    /**
     * [PURPOSE]       Saves the new best score, but only if it is higher than the one in the DB.
     * [OOP / PATTERN] Conditional UPDATE (the check is in the SQL WHERE clause).
     * [CALLED BY]     btd/ui/screens/GamePanel.java (finishRun(), on game over or quit, only if waves cleared > old best).
     * [CONNECTS TO]   btd/config/DatabaseConnection.java, MySQL 'players' table.
     * [DEFENSE NOTE]  The clause 'AND highest_score < ?' guarantees in SQL that an inferior score
     *                 can never overwrite a personal best, preventing state regression without client locks.
     */
    public void updateHighestScore(int playerId, int wavesCleared) throws SQLException {
        Connection conn = DatabaseConnection.getInstance().getConnection();
        String sql = "UPDATE players SET highest_score = ? WHERE player_id = ? AND highest_score < ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, wavesCleared);
            ps.setInt(2, playerId);
            ps.setInt(3, wavesCleared);
            ps.executeUpdate();
        }
    }
}
