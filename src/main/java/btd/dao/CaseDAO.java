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
 * [PURPOSE]       Reads all cases from MySQL, plus which towers each case can drop.
 * [OOP / PATTERN] Data Access Object (DAO) / many-to-many through the case_towers join table.
 * [CALLED BY]     btd/core/Catalog.java (load(), the first time Play is pressed).
 * [CONNECTS TO]   btd/config/DatabaseConnection.java, MySQL 'cases' and 'case_towers' tables.
 * [DEFENSE NOTE]  No SQL JOIN on the towers table: it reads only tower_ids from case_towers and looks them up in
 *                 the towersById map that Catalog already built. Cost: 1 query for cases + 1 query per case (3 cases = 4 queries).
 */
public class CaseDAO {

    /**
     * [PURPOSE]       Loads all cases and populates their TowerType drop pools.
     * [OOP / PATTERN] ResultSet -> Record mapping / PreparedStatement.
     * [CALLED BY]     btd/core/Catalog.java (load()).
     * [CONNECTS TO]   btd/model/gacha/CaseDef.java, btd/model/tower/TowerType.java.
     * [DEFENSE NOTE]  ORDER BY cost: Standard (100), Premium (250), Legendary (500). The shop shows them in this order,
     *                 and the FIRST one (Standard) is the one the free starter case opens.
     *                 try-with-resources closes the statement and result set, but NOT the shared Connection.
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
     * [PURPOSE]       Gets the towers a case can drop: SELECT tower_id FROM case_towers WHERE case_id = ?
     * [OOP / PATTERN] Many-to-Many lookup / PreparedStatement with a ? parameter (safe from SQL injection).
     * [CALLED BY]     findAll(), once per case.
     * [CONNECTS TO]   MySQL 'case_towers' table, the towersById map.
     * [DEFENSE NOTE]  case_towers has a composite primary key (case_id, tower_id), so a tower cannot be in the same case twice.
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
