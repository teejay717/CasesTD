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
 * [CALLED BY]     btd/ui/screens/LandingPanel.java (login), btd/ui/screens/GamePanel.java (game over save).
 * [CONNECTS TO]   btd/config/DatabaseConnection.java, MySQL 'players' table (player_id, player_name, highest_score).
 * [DEFENSE NOTE]  Completely isolates SQL from GUI screens; prevents SQL injection using PreparedStatement.
 */
public class PlayerDAO {

    /**
     * [PURPOSE]       Finds an existing player profile by case-insensitive username.
     * [OOP / PATTERN] Parameterized Query / Null-Safe Optional Return.
     * [CALLED BY]     findOrCreate(name), btd/ui/screens/LandingPanel.java.
     * [CONNECTS TO]   btd/config/DatabaseConnection.java.
     * [DEFENSE NOTE]  Returns Optional<Player> to prevent NullPointerException if player does not exist yet.
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
     * [PURPOSE]       Retrieves an existing player profile or automatically inserts a new row if absent.
     * [OOP / PATTERN] Idempotent Registration / User Onboarding.
     * [CALLED BY]     btd/ui/screens/LandingPanel.java (when typing handle or clicking crate/play).
     * [CONNECTS TO]   btd/model/player/Player.java, MySQL 'players' table.
     * [DEFENSE NOTE]  Eliminates tedious manual registration screens; players are auto-created seamlessly.
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
     * [PURPOSE]       Updates player highest score only if current run beats the historical record.
     * [OOP / PATTERN] Atomic Conditional SQL Query / Race Condition Prevention.
     * [CALLED BY]     btd/ui/screens/GamePanel.java (endRun() on game over).
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
