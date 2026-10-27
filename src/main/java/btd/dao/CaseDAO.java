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

/** Reads the cases table and the case_towers join table. */
public class CaseDAO {

    /** Loads every case with its tower pool. The tower list comes from TowerDAO so rows are not read twice. */
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
