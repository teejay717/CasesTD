package btd.dao;

import btd.config.DatabaseConnection;
import btd.model.gacha.CaseDef;
import btd.model.tower.TowerType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * [PURPOSE]       Reads all case definitions and resolves their drop pools from MySQL.
 * [OOP / PATTERN] Data Access Object (DAO) Pattern / Junction Table Resolution.
 * [CALLED BY]     btd/core/Catalog.java (load() at startup).
 * [CONNECTS TO]   btd/config/DatabaseConnection.java, MySQL 'cases' and 'case_towers' tables.
 * [DEFENSE NOTE]  Reuses pre-loaded TowerType records from towersById map, avoiding redundant SQL queries.
 */
public class CaseDAO {

    /**
     * [PURPOSE]       Loads all cases and populates their TowerType drop pools.
     * [OOP / PATTERN] Data Transfer Object Mapping / Record Creation.
     * [CALLED BY]     btd/core/Catalog.java (load()).
     * [CONNECTS TO]   btd/model/gacha/CaseDef.java, btd/model/tower/TowerType.java.
     * [DEFENSE NOTE]  Orders cases by cost ascending (Standard: 100, Premium: 250, Legendary: 500).
     */
    public List<CaseDef> findAll(Map<Integer, TowerType> towersById) throws SQLException {
        Connection conn = DatabaseConnection.getInstance().getConnection();
        List<CaseDef> result = new ArrayList<>();
        String sql = "SELECT case_id, case_name, cost, common_weight, rare_weight, epic_weight, legendary_weight "
                + "FROM cases ORDER BY cost, case_id";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("case_id");
                result.add(new CaseDef(
                        id,
                        rs.getString("case_name"),
                        rs.getInt("cost"),
                        rs.getDouble("common_weight"),
                        rs.getDouble("rare_weight"),
                        rs.getDouble("epic_weight"),
                        rs.getDouble("legendary_weight"),
                        findPool(conn, id, towersById)));
            }
        }
        return result;
    }

    /**
     * [PURPOSE]       Queries case_towers junction table to find all towers droppable from a case.
     * [OOP / PATTERN] Many-to-Many Relational Mapping.
     * [CALLED BY]     findAll() internally.
     * [CONNECTS TO]   MySQL 'case_towers' table.
     * [DEFENSE NOTE]  Standard normalized junction table resolution via foreign key case_id.
     */
    private List<TowerType> findPool(Connection conn, int caseId, Map<Integer, TowerType> towersById)
            throws SQLException {
        List<TowerType> pool = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT tower_id FROM case_towers WHERE case_id = ?")) {
            ps.setInt(1, caseId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TowerType t = towersById.get(rs.getInt("tower_id"));
                    if (t != null) {
                        pool.add(t);
                    }
                }
            }
        }
        return pool;
    }
}
