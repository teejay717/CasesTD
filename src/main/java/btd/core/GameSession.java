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
 * Everything the player owns during one run: coins, lives, free cases, tower copies in the
 * inventory and towers placed on the map. All changes go through methods here so the rules
 * (costs, refunds, one Level 5 per type) are enforced in one place.
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

    public GameSession(GameConfig config, CaseRNGManager rng) {
        this.config = config;
        this.rng = rng;
        this.coins = config.economy.startingCoins;
        this.lives = config.economy.startingLives;
        this.freeCases = config.economy.freeStarterCases;
    }

    // ---- coins and lives ----

    public void addCoins(int amount) {
        coins += amount;
    }

    public void loseLives(int amount) {
        lives = Math.max(0, lives - amount);
    }

    // ---- cases ----

    /** Pays for a case and returns the tower that dropped (already added to the inventory), or null if too poor. */
    public TowerType buyCase(CaseDef def) {
        if (coins < def.cost()) {
            return null;
        }
        coins -= def.cost();
        return open(def);
    }

    /** Uses one free case. Returns null if none are left. */
    public TowerType openFreeCase(CaseDef def) {
        if (freeCases <= 0) {
            return null;
        }
        freeCases--;
        return open(def);
    }

    private TowerType open(CaseDef def) {
        TowerType won = rng.roll(def);
        inventory.add(won);
        casesOpened++;
        return won;
    }

    // ---- inventory ----

    public int copiesOf(TowerType type) {
        int n = 0;
        for (TowerType t : inventory) {
            if (t.id() == type.id()) {
                n++;
            }
        }
        return n;
    }

    /** Sells an unused copy for the rarity's refund. */
    public boolean sellInventory(int index) {
        if (index < 0 || index >= inventory.size()) {
            return false;
        }
        TowerType t = inventory.remove(index);
        coins += refundFor(t);
        return true;
    }

    public int refundFor(TowerType type) {
        return config.economy.sellRefund.get(type.rarity().name());
    }

    // ---- placing, upgrading, selling ----

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

    /** Places the inventory copy at index on the map at Level 1. Returns the new tower or null. */
    public Tower place(int inventoryIndex, GameMap map, double x, double y) {
        if (inventoryIndex < 0 || inventoryIndex >= inventory.size() || !canPlaceAt(map, x, y)) {
            return null;
        }
        TowerType type = inventory.remove(inventoryIndex);
        Tower tower = TowerFactory.create(type, x, y);
        towers.add(tower);
        return tower;
    }

    /** Why this tower cannot be upgraded right now, or null if it can. */
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

    /** Spare copies needed for the next level (0 if already max). */
    public int upgradeCost(Tower tower) {
        if (tower.getLevel() >= Tower.MAX_LEVEL) {
            return 0;
        }
        return UpgradeCostTable.copiesNeeded(config, tower.getType().rarity(), tower.getLevel() + 1);
    }

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

    private void consumeCopy(TowerType type) {
        for (int i = 0; i < inventory.size(); i++) {
            if (inventory.get(i).id() == type.id()) {
                inventory.remove(i);
                return;
            }
        }
    }

    /** Removes a placed tower and gives back the rarity's refund. */
    public void sell(Tower tower) {
        if (towers.remove(tower)) {
            coins += refundFor(tower.getType());
            if (tower.getLevel() == Tower.MAX_LEVEL) {
                levelFiveTypes.remove(tower.getType().id());
            }
        }
    }

    // ---- getters ----

    public int getCoins() { return coins; }
    public int getLives() { return lives; }
    public int getFreeCases() { return freeCases; }
    public int getCasesOpened() { return casesOpened; }
    public List<TowerType> getInventory() { return Collections.unmodifiableList(inventory); }
    /** Read-only view; towers are added and removed only through place() and sell(). */
    public List<Tower> getTowers() { return Collections.unmodifiableList(towers); }
    public GameConfig getConfig() { return config; }
}
