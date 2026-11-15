package btd.core;

import btd.dao.CaseDAO;
import btd.dao.TowerDAO;
import btd.model.gacha.CaseDef;
import btd.model.tower.TowerType;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * [PURPOSE]       In-memory copy of all tower types and cases read from MySQL.
 * [OOP / PATTERN] In-Memory Cache / Registry (lookup by id).
 * [CALLED BY]     btd/ui/AppController.java (loadData(), the first time Play is pressed),
 *                 then handed to btd/ui/screens/GamePanel.java and btd/ui/screens/ShopOverlay.java.
 * [CONNECTS TO]   btd/dao/TowerDAO.java, btd/dao/CaseDAO.java,
 *                 btd/model/tower/TowerType.java, btd/model/gacha/CaseDef.java.
 * [DEFENSE NOTE]  Loaded ONCE (on the first Play press, not at app startup). AppController keeps it in a field,
 *                 so later runs reuse it and the 60 FPS game loop never touches the database.
 */
public class Catalog {

    private final List<TowerType> towers;
    private final List<CaseDef> cases;
    private final Map<Integer, TowerType> towersById = new LinkedHashMap<>();

    /**
     * [PURPOSE]       Initializes memory registry and populates O(1) ID lookup map.
     * [OOP / PATTERN] Constructor / Map Indexing.
     * [CALLED BY]     btd/core/Catalog.java (load()).
     * [CONNECTS TO]   towersById map.
     */
    public Catalog(List<TowerType> towers, List<CaseDef> cases) {
        this.towers = towers;
        this.cases = cases;
        for (TowerType t : towers) {
            towersById.put(t.id(), t);
        }
    }

    /**
     * [PURPOSE]       Queries database DAOs for all towers and case definitions, validating seed data presence.
     * [OOP / PATTERN] Static Factory Method / Data Hydration.
     * [CALLED BY]     btd/ui/AppController.java (loadData()).
     * [CONNECTS TO]   btd/dao/TowerDAO.java (findAll()), btd/dao/CaseDAO.java (findAll()).
     * [DEFENSE NOTE]  Order matters: towers are loaded first and put in a map by id, because CaseDAO needs
     *                 that map to turn the tower_ids in case_towers into TowerType objects.
     *                 Throws SQLException if either table is empty, telling the user to run seed_data.sql.
     */
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

    /** [PURPOSE] Returns the list of all tower types (the records inside cannot change). */
    public List<TowerType> getTowers() { return towers; }
    /** [PURPOSE] Returns the list of all cases, cheapest first (used by ShopOverlay). */
    public List<CaseDef> getCases() { return cases; }
    /** [PURPOSE] Fast O(1) hash lookup of tower specification by its primary key ID. */
    public TowerType getTower(int id) { return towersById.get(id); }
}
