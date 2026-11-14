package btd.core;

import btd.factory.TowerFactory;
import btd.model.gacha.CaseDef;
import btd.model.tower.Tower;
import btd.model.tower.TowerType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Collections;
import java.util.Set;

/**
 * [PURPOSE]       Central state container managing player currency, base lives, inventory copies, placed towers, and gacha opens.
 * [OOP / PATTERN] Aggregate Root / Session State Pattern / Encapsulation.
 * [CALLED BY]     btd/core/GameEngine.java, btd/ui/screens/SidePanel.java,
 *                 btd/ui/screens/GamePanel.java, btd/ui/screens/ShopOverlay.java.
 * [CONNECTS TO]   btd/core/GameConfig.java, btd/core/CaseRNGManager.java,
 *                 btd/factory/TowerFactory.java, btd/model/tower/Tower.java.
 * [DEFENSE NOTE]  Enforces single-source-of-truth: gold validation, spacing rules, and unique Level 5 limits are guarded here.
 */
public class GameSession {

    private static final double MIN_TOWER_GAP = 34;

    private final GameConfig config;
    private final CaseRNGManager rng;

    private int coins;
    private int lives;
    private int freeCases;
    private int casesOpened;

    private final List<TowerType> inventory = new ArrayList<>();
    private final List<Tower> towers = new ArrayList<>();
    private final Set<Integer> levelFiveTypes = new HashSet<>();

    /**
     * [PURPOSE]       Initializes new gameplay session using starting values from config.
     * [OOP / PATTERN] Constructor / State Initialization.
     * [CALLED BY]     btd/ui/AppController.java (startNewGame()).
     * [CONNECTS TO]   btd/core/GameConfig.Economy.
     */
    public GameSession(GameConfig config, CaseRNGManager rng) {
        this.config = config;
        this.rng = rng;
        this.coins = config.economy.startingCoins;
        this.lives = config.economy.startingLives;
        this.freeCases = config.economy.freeStarterCases;
    }

    // ---- coins and lives ----

    /**
     * [PURPOSE]       Credits gold coins to the player balance from kills or wave clear rewards.
     * [OOP / PATTERN] State Mutation / Encapsulation.
     * [CALLED BY]     btd/core/GameEngine.java (update()).
     */
    public void addCoins(int amount) {
        coins += amount;
    }

    /**
     * [PURPOSE]       Deducts lives when enemies reach the end of the track, clamping at 0.
     * [OOP / PATTERN] State Mutation with Invariant Protection.
     * [CALLED BY]     btd/core/GameEngine.java (update()).
     */
    public void loseLives(int amount) {
        lives = Math.max(0, lives - amount);
    }

    // ---- cases ----

    /**
     * [PURPOSE]       Deducts gold cost and rolls a case, adding result to player inventory.
     * [OOP / PATTERN] Transaction Pattern / Guard Clause.
     * [CALLED BY]     btd/ui/screens/ShopOverlay.java (buyCase()).
     * [CONNECTS TO]   open(), btd/model/gacha/CaseDef.java.
     * [DEFENSE NOTE]  Returns null if insufficient gold, preventing overdrafts.
     */
    public TowerType buyCase(CaseDef def) {
        if (coins < def.cost()) {
            return null;
        }
        coins -= def.cost();
        return open(def);
    }

    /**
     * [PURPOSE]       Consumes one free starter case and rolls a tower without spending gold.
     * [OOP / PATTERN] Transaction Pattern / Guard Clause.
     * [CALLED BY]     btd/ui/screens/ShopOverlay.java.
     * [CONNECTS TO]   open().
     */
    public TowerType openFreeCase(CaseDef def) {
        if (freeCases <= 0) {
            return null;
        }
        freeCases--;
        return open(def);
    }

    /** [PURPOSE] Common internal roll execution appending dropped tower to inventory list. */
    private TowerType open(CaseDef def) {
        TowerType won = rng.roll(def);
        inventory.add(won);
        casesOpened++;
        return won;
    }

    // ---- inventory ----

    /** [PURPOSE] Counts number of unplaced spare copies of a specific tower archetype in inventory. */
    public int copiesOf(TowerType type) {
        int n = 0;
        for (TowerType t : inventory) {
            if (t.id() == type.id()) {
                n++;
            }
        }
        return n;
    }

    /**
     * [PURPOSE]       Sells an unplaced tower card from inventory, refunding gold based on rarity.
     * [OOP / PATTERN] State Mutation / List Removal.
     * [CALLED BY]     btd/ui/screens/SidePanel.java.
     * [CONNECTS TO]   refundFor().
     */
    public boolean sellInventory(int index) {
        if (index < 0 || index >= inventory.size()) {
            return false;
        }
        TowerType t = inventory.remove(index);
        coins += refundFor(t);
        return true;
    }

    /** [PURPOSE] Looks up gold refund value for tower rarity from economy config table. */
    public int refundFor(TowerType type) {
        return config.economy.sellRefund.get(type.rarity().name());
    }

    // ---- placing, upgrading, selling ----

    /**
     * [PURPOSE]       Verifies valid placement: checks map walkability and enforces 34px tower separation.
     * [OOP / PATTERN] Spatial Validation / Guard Predicate.
     * [CALLED BY]     place(), btd/ui/screens/GamePanel.java (paintPlacementPreview()).
     * [CONNECTS TO]   btd/core/GameMap.java (canBuildAt()).
     * [DEFENSE NOTE]  Prevents placing towers on top of one another using Euclidean distance check.
     */
    public boolean canPlaceAt(GameMap map, double x, double y) {
        if (!map.canBuildAt(x, y)) {
            return false;
        }
        for (Tower t : towers) {
            if (Math.hypot(t.getX() - x, t.getY() - y) < MIN_TOWER_GAP) {
                return false;
            }
        }
        return true;
    }

    /**
     * [PURPOSE]       Instantiates and places a Level 1 tower onto the map from an inventory card.
     * [OOP / PATTERN] Factory Method Invocation / State Mutation.
     * [CALLED BY]     btd/ui/screens/GamePanel.java (mouseReleased()).
     * [CONNECTS TO]   btd/factory/TowerFactory.java (create()).
     * [DEFENSE NOTE]  Removes copy from inventory and returns concrete placed Tower reference.
     */
    public Tower place(int inventoryIndex, GameMap map, double x, double y) {
        if (inventoryIndex < 0 || inventoryIndex >= inventory.size() || !canPlaceAt(map, x, y)) {
            return null;
        }
        TowerType type = inventory.remove(inventoryIndex);
        Tower tower = TowerFactory.create(type, x, y);
        towers.add(tower);
        return tower;
    }

    /**
     * [PURPOSE]       Evaluates why a tower cannot be upgraded: checks max level, duplicate Lv5 rule, or copy count.
     * [OOP / PATTERN] Validation / Human-Readable Rule Explanation.
     * [CALLED BY]     btd/ui/screens/SidePanel.java.
     * [CONNECTS TO]   upgradeCost(), copiesOf().
     * [DEFENSE NOTE]  Enforces the key rule: a player may only own ONE Level 5 tower of any given archetype at once.
     */
    public String upgradeBlockReason(Tower tower) {
        int level = tower.getLevel();
        if (level >= Tower.MAX_LEVEL) {
            return "Max level";
        }
        int next = level + 1;
        if (next == Tower.MAX_LEVEL && levelFiveTypes.contains(tower.getType().id())) {
            return "Max Lv " + Tower.MAX_NORMAL_LEVEL + " (another is Lv " + Tower.MAX_LEVEL + ")";
        }
        int need = upgradeCost(tower);
        int have = copiesOf(tower.getType());
        if (have < need) {
            return "Need " + need + " spare cop" + (need == 1 ? "y" : "ies") + " (have " + have + ")";
        }
        return null;
    }

    /** [PURPOSE] Returns number of duplicate tower copies needed to reach the next upgrade tier. */
    public int upgradeCost(Tower tower) {
        if (tower.getLevel() >= Tower.MAX_LEVEL) {
            return 0;
        }
        return UpgradeCostTable.copiesNeeded(config, tower.getType().rarity(), tower.getLevel() + 1);
    }

    /**
     * [PURPOSE]       Consumes required duplicate copies from inventory and increments tower tier.
     * [OOP / PATTERN] State Mutation / Invariant Enforcement.
     * [CALLED BY]     btd/ui/screens/SidePanel.java (upgradeButton).
     * [CONNECTS TO]   btd/model/tower/Tower.java (applyLevel()).
     * [DEFENSE NOTE]  Adds tower type ID to levelFiveTypes set when reaching Level 5.
     */
    public boolean upgrade(Tower tower) {
        if (upgradeBlockReason(tower) != null) {
            return false;
        }
        int need = upgradeCost(tower);
        for (int i = 0; i < need; i++) {
            consumeCopy(tower.getType());
        }
        tower.applyLevel(tower.getLevel() + 1);
        if (tower.getLevel() == Tower.MAX_LEVEL) {
            levelFiveTypes.add(tower.getType().id());
        }
        return true;
    }

    /** [PURPOSE] Removes first matching copy of tower archetype from inventory. */
    private void consumeCopy(TowerType type) {
        for (int i = 0; i < inventory.size(); i++) {
            if (inventory.get(i).id() == type.id()) {
                inventory.remove(i);
                return;
            }
        }
    }

    /**
     * [PURPOSE]       Removes placed tower from grid, refunds base gold value, and releases Lv5 uniqueness lock if applicable.
     * [OOP / PATTERN] Deletion / Cleanup.
     * [CALLED BY]     btd/ui/screens/SidePanel.java (sellButton).
     * [CONNECTS TO]   towers list, levelFiveTypes set.
     */
    public void sell(Tower tower) {
        if (towers.remove(tower)) {
            coins += refundFor(tower.getType());
            if (tower.getLevel() == Tower.MAX_LEVEL) {
                levelFiveTypes.remove(tower.getType().id());
            }
        }
    }

    // ---- getters ----

    /** [PURPOSE] Current available gold coin balance. */
    public int getCoins() { return coins; }
    /** [PURPOSE] Current remaining base health lives. */
    public int getLives() { return lives; }
    /** [PURPOSE] Number of unspent free starter cases remaining. */
    public int getFreeCases() { return freeCases; }
    /** [PURPOSE] Total number of cases opened across this entire run. */
    public int getCasesOpened() { return casesOpened; }
    /** [PURPOSE] Unmodifiable view of cards currently held in player inventory. */
    public List<TowerType> getInventory() { return Collections.unmodifiableList(inventory); }
    /**
     * [PURPOSE]       Returns unmodifiable view of all towers currently constructed on the map grid.
     * [OOP / PATTERN] Encapsulation / Defensive Copying / Immutability.
     * [DEFENSE NOTE]  Towers are added and removed only through place() and sell(), preventing external mutation.
     */
    public List<Tower> getTowers() { return Collections.unmodifiableList(towers); }
    /** [PURPOSE] Game balance configuration reference. */
    public GameConfig getConfig() { return config; }
}
