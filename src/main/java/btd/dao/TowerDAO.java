package btd.dao;

import btd.config.DatabaseConnection;
import btd.model.gacha.Rarity;
import btd.model.tower.Special;
import btd.model.tower.TowerType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * [PURPOSE]       Reads all 10 tower definitions from the MySQL 'towers' table.
 * [OOP / PATTERN] Data Access Object (DAO) Pattern / Object-Relational Mapping (ORM).
 * [CALLED BY]     btd/core/Catalog.java (load() at startup).
 * [CONNECTS TO]   btd/config/DatabaseConnection.java, btd/model/tower/TowerType.java (immutable record).
 * [DEFENSE NOTE]  Tower catalog is read-only at runtime; maps relational columns directly to immutable records.
 */
public class TowerDAO {

    /**
     * [PURPOSE]       Queries all tower rows ordered by tower_id and constructs TowerType records.
     * [OOP / PATTERN] ResultSet Mapping / Record Instantiation.
     * [CALLED BY]     btd/core/Catalog.java (load()).
     * [CONNECTS TO]   btd/model/tower/TowerType.java, btd/model/gacha/Rarity.java, btd/model/tower/Special.java.
     * [DEFENSE NOTE]  Loads once at application start to avoid repeated SQL queries during the 60 FPS loop.
     */
    public List<TowerType> findAll() throws SQLException {
        Connection conn = DatabaseConnection.getInstance().getConnection();
        String sql = "SELECT tower_id, tower_name, tower_class, rarity, base_damage, attack_range, fire_rate, "
                + "splash_radius, special, effect_value, effect_duration_ms, description, sprite_path "
                + "FROM towers ORDER BY tower_id";
        List<TowerType> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(new TowerType(
                        rs.getInt("tower_id"),
                        rs.getString("tower_name"),
                        rs.getString("tower_class"),
                        Rarity.valueOf(rs.getString("rarity")),
                        rs.getInt("base_damage"),
                        rs.getInt("attack_range"),
                        rs.getDouble("fire_rate"),
                        rs.getInt("splash_radius"),
                        Special.valueOf(rs.getString("special")),
                        rs.getDouble("effect_value"),
                        rs.getInt("effect_duration_ms"),
                        rs.getString("description"),
                        rs.getString("sprite_path")));
            }
        }
        return result;
    }
}
