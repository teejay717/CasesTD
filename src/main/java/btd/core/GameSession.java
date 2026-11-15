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
 * [PURPOSE]       Holds the player's state for ONE run: coins, lives, free cases, inventory cards, and placed towers.
 * [OOP / PATTERN] Encapsulation (all fields private; changed only through methods that check the rules).
 * [CALLED BY]     Created in btd/ui/screens/GamePanel.java (startRun()). Used by GamePanel, btd/core/GameEngine.java,
 *                 btd/ui/screens/SidePanel.java (reads it to draw), btd/ui/screens/ShopOverlay.java (buyCase(), openFreeCase()).
 * [CONNECTS TO]   btd/core/GameConfig.java, btd/core/CaseRNGManager.java,
 *                 btd/factory/TowerFactory.java, btd/model/tower/Tower.java.
 * [DEFENSE NOTE]  Single source of truth: "enough coins?", "can build here?", "enough copies?",
 *                 and "only one Lv 5 per tower type" are all checked here, so the UI cannot break the rules.
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
     * [PURPOSE]       Starts a new run with the starting values from config (75 coins, 20 lives, 1 free case).
     * [OOP / PATTERN] Constructor / Dependency Injection (config and rng are passed in).
     * [CALLED BY]     btd/ui/screens/GamePanel.java (startRun()).
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
     * [PURPOSE]       Adds coins (kill reward or wave-clear reward).
     * [OOP / PATTERN] State Mutation / Encapsulation.
     * [CALLED BY]     btd/core/GameEngine.java (update(): when an enemy dies, and when a wave is cleared).
     */
    public void addCoins(int amount) {
        coins += amount;
    }

    /**
     * [PURPOSE]       Deducts lives when enemies reach the end of the track, clamping at 0.
     * [OOP / PATTERN] State Mutation with Invariant Protection.
     * [CALLED BY]     btd/core/GameEngine.java (update(), with the enemy's getLeakDamage()).
     */
    public void loseLives(int amount) {
        lives = Math.max(0, lives - amount);
    }

    // ---- cases ----

    /**
     * [PURPOSE]       Deducts gold cost and rolls a case, adding result to player inventory.
     * [OOP / PATTERN] Transaction Pattern / Guard Clause.
     * [CALLED BY]     btd/ui/screens/ShopOverlay.java (openCase()).
     * [CONNECTS TO]   open(), btd/model/gacha/CaseDef.java.
     * [DEFENSE NOTE]  Returns null if there are not enough coins, so coins can never go negative.
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
     * [CALLED BY]     btd/ui/screens/ShopOverlay.java (openCase(), only for the first case while a free case is left).
     * [CONNECTS TO]   open().
     */
    public TowerType openFreeCase(CaseDef def) {
        if (freeCases <= 0) {
            return null;
        }
        freeCases--;
        return open(def);
    }

    /** [PURPOSE] Shared by buyCase() and openFreeCase(): asks CaseRNGManager.roll() for a tower and adds it to the inventory. */
    private TowerType open(CaseDef def) {
        TowerType won = rng.roll(def);
        inventory.add(won);
        casesOpened++;
        return won;
    }

    // ---- inventory ----

    /** [PURPOSE] How many unplaced copies of this tower type are in the inventory (compared by id). */
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
     * [PURPOSE]       Sells an unplaced card from the inventory for coins (30 / 50 / 75 / 150 by rarity).
     * [OOP / PATTERN] State Mutation / Guard Clause (bad index -> false).
     * [CALLED BY]     btd/ui/screens/GamePanel.java (sellInventory(), from the right-click "Sell" menu).
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

    /** [PURPOSE] Sell price for a tower type, based only on rarity (economy.sellRefund). The level does not matter. */
    public int refundFor(TowerType type) {
        return config.economy.sellRefund.get(type.rarity().name());
    }

    // ---- placing, upgrading, selling ----

    /**
     * [PURPOSE]       Can a tower go at (x, y)? Map rules first, then "at least 34 px from every other tower".
     * [OOP / PATTERN] Validation / Guard Predicate.
     * [CALLED BY]     place(), btd/ui/screens/GamePanel.java (paintPlacementGrid() for the red tiles, paintGhost() for the red/white circle).
     * [CONNECTS TO]   btd/core/GameMap.java (canBuildAt()).
     * [DEFENSE NOTE]  Math.hypot(dx, dy) is the straight-line distance between two towers.
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
     * [CALLED BY]     btd/ui/screens/GamePanel.java (placeAt(), when you click the map while placing).
     * [CONNECTS TO]   btd/factory/TowerFactory.java (create()).
     * [DEFENSE NOTE]  The card (TowerType = blueprint) leaves the inventory and TowerFactory builds a real Tower
     *                 object (SingleTargetTower, SplashTower...). Returns null if the spot is not allowed.
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
     * [CALLED BY]     upgrade(), btd/ui/screens/SidePanel.java (paintTowerInfo(), to grey out UPGRADE and show the reason).
     * [CONNECTS TO]   upgradeCost(), copiesOf().
     * [DEFENSE NOTE]  Returns null when the upgrade IS allowed, otherwise a message for the player.
     *                 Key rule: only ONE Level 5 per tower type at a time (tracked in levelFiveTypes),
     *                 so a second tower of that type stops at Level 4 (MAX_NORMAL_LEVEL).
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

    /** [PURPOSE] Copies needed for the next level (0 if already max). Called by upgrade(), upgradeBlockReason(), SidePanel. */
    public int upgradeCost(Tower tower) {
        if (tower.getLevel() >= Tower.MAX_LEVEL) {
            return 0;
        }
        return UpgradeCostTable.copiesNeeded(config, tower.getType().rarity(), tower.getLevel() + 1);
    }

    /**
     * [PURPOSE]       Consumes required duplicate copies from inventory and increments tower tier.
     * [OOP / PATTERN] State Mutation / Invariant Enforcement.
     * [CALLED BY]     btd/ui/screens/GamePanel.java (upgradeSelected(), after the SidePanel UPGRADE button is clicked).
     * [CONNECTS TO]   btd/model/tower/Tower.java (applyLevel()).
     * [DEFENSE NOTE]  Checks the rules first (upgradeBlockReason), removes the copies, then levels up the SAME
     *                 Tower object. Reaching Level 5 adds the type id to levelFiveTypes.
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
     * [PURPOSE]       Removes a placed tower, refunds coins by rarity, and frees the "one Lv 5" slot if it was Lv 5.
     * [OOP / PATTERN] Deletion / Cleanup.
     * [CALLED BY]     btd/ui/screens/GamePanel.java (sellSelected(), after the SidePanel SELL button is clicked).
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
    /** [PURPOSE] Read-only view of the inventory cards (calling add/remove on it throws an exception). */
    public List<TowerType> getInventory() { return Collections.unmodifiableList(inventory); }
    /**
     * [PURPOSE]       Read-only view of all towers placed on the map.
     * [OOP / PATTERN] Encapsulation / Read-Only View (not a copy: it wraps the same list, but blocks changes).
     * [DEFENSE NOTE]  Towers can only be added by place() and removed by sell(), so no other class can sneak one in.
     */
    public List<Tower> getTowers() { return Collections.unmodifiableList(towers); }
    /** [PURPOSE] Game balance configuration reference. */
    public GameConfig getConfig() { return config; }
}
