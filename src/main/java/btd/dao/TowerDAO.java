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
 * [PURPOSE]       Reads all towers (10 in seed_data.sql) from the MySQL 'towers' table.
 * [OOP / PATTERN] Data Access Object (DAO): all tower SQL is in this one class.
 * [CALLED BY]     btd/core/Catalog.java (load(), the first time Play is pressed).
 * [CONNECTS TO]   btd/config/DatabaseConnection.java, btd/model/tower/TowerType.java (immutable record).
 * [DEFENSE NOTE]  Read-only: the game never INSERTs or UPDATEs towers. Each row becomes one TowerType record.
 */
public class TowerDAO {

    /**
     * [PURPOSE]       Queries all tower rows ordered by tower_id and constructs TowerType records.
     * [OOP / PATTERN] ResultSet Mapping / Record Instantiation.
     * [CALLED BY]     btd/core/Catalog.java (load()).
     * [CONNECTS TO]   btd/model/tower/TowerType.java, btd/model/gacha/Rarity.java, btd/model/tower/Special.java.
     * [DEFENSE NOTE]  Runs once per app session (Catalog is cached), never during the 60 FPS loop.
     *                 Rarity.valueOf("RARE") / Special.valueOf("STUN") turn the text columns into enums;
     *                 a typo in the DB would throw IllegalArgumentException here.
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
