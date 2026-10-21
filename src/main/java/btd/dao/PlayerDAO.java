package btd.dao;

import btd.config.DatabaseConnection;
import btd.model.player.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/** All SQL for the players table lives here, so the UI never touches the database. */
public class PlayerDAO {

    /** Finds a profile by name. MySQL ignores case by default, so "sam" finds "Sam". */
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

    /** Loads the profile for this name, or creates it with a highest score of 0. */
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

    /** Saves a run's waves cleared, but only if it beats the stored highest score. */
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
