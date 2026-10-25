package btd.core;

import btd.dao.CaseDAO;
import btd.dao.TowerDAO;
import btd.model.gacha.CaseDef;
import btd.model.tower.TowerType;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Every tower and case in the game, loaded once from the database. */
public class Catalog {

    private final List<TowerType> towers;
    private final List<CaseDef> cases;
    private final Map<Integer, TowerType> towersById = new LinkedHashMap<>();

    public Catalog(List<TowerType> towers, List<CaseDef> cases) {
        this.towers = towers;
        this.cases = cases;
        for (TowerType t : towers) {
            towersById.put(t.id(), t);
        }
    }

    /** Reads the towers and cases tables. Run database/schema.sql and seed_data.sql first. */
    public static Catalog load() throws SQLException {
        List<TowerType> towers = new TowerDAO().findAll();
        Map<Integer, TowerType> byId = new LinkedHashMap<>();
        for (TowerType t : towers) {
            byId.put(t.id(), t);
        }
        List<CaseDef> cases = new CaseDAO().findAll(byId);
        if (towers.isEmpty() || cases.isEmpty()) {
            throw new SQLException("The towers or cases table is empty. Run database/seed_data.sql.");
        }
        return new Catalog(towers, cases);
    }

    public List<TowerType> getTowers() { return towers; }
    public List<CaseDef> getCases() { return cases; }
    public TowerType getTower(int id) { return towersById.get(id); }
}
