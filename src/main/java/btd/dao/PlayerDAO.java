package btd.dao;

import btd.config.DatabaseConnection;
import btd.model.player.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.Optional;

public class PlayerDAO {

    public static final int MAX_NAME_LENGTH = 50;

    private static final String SELECT_BY_NAME =
            "SELECT player_id, player_name, highest_score FROM players WHERE player_name = ?";
    private static final String INSERT =
            "INSERT INTO players (player_name) VALUES (?)";
    private static final String UPDATE_SCORE =
            "UPDATE players SET highest_score = ? WHERE player_id = ? AND highest_score < ?";
 
    private final DatabaseConnection db = DatabaseConnection.getInstance();

    public Optional<Player> findByName(String name) throws SQLException {
        String cleaned = clean(name);
        Connection conn = db.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(SELECT_BY_NAME)) {
            ps.setString(1, cleaned);
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

    public Player findOrCreate(String name) throws SQLException {
        Optional<Player> existing = findByName(name); 
        if (existing.isPresent()) {
            return existing.get();
        }
        String cleaned = clean(name);
        Connection conn = db.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, cleaned);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return new Player(keys.getInt(1), cleaned, 0);
                }
            }
        } catch (SQLIntegrityConstraintViolationException duplicate) {
            // Another session created the same name in the meantime, so load that profile.
            return findByName(cleaned).orElseThrow(() -> duplicate);
        }
        return findByName(cleaned).orElseThrow(() -> new SQLException("Could not create player " + cleaned));
    }

    /**
     * Saves a run's waves cleared, but only if it beats the stored highest score.
     *
     * @return true if the highest score was updated
     */
    public boolean updateHighestScore(int playerId, int wavesCleared) throws SQLException {
        Connection conn = db.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(UPDATE_SCORE)) {
            ps.setInt(1, wavesCleared);
            ps.setInt(2, playerId);
            ps.setInt(3, wavesCleared);
            return ps.executeUpdate() > 0;
        }
    }

    /** Trims the name and checks it fits the players table. */
    private static String clean(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Player name must not be empty.");
        }
        String cleaned = name.trim();
        if (cleaned.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Player name must be at most " + MAX_NAME_LENGTH + " characters.");
        }
        return cleaned;
    }
}
