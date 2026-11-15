package btd.core;

import btd.model.gacha.Rarity;

/**
 * [PURPOSE]       Static lookup utility determining duplicate tower copy costs required for tier promotions (Levels 2-5).
 * [OOP / PATTERN] Utility Class / Pure Fabrication / Single Responsibility Principle.
 * [CALLED BY]     btd/core/GameSession.java (upgradeCost()).
 * [CONNECTS TO]   btd/core/GameConfig.java, btd/model/gacha/Rarity.java.
 * [DEFENSE NOTE]  Decouples economy balancing arrays from Tower domain classes; loaded from JSON config.
 */
public final class UpgradeCostTable {

    /** Private constructor prevents instantiation of static utility class. */
    private UpgradeCostTable() {
    }

    /**
     * [PURPOSE]       Calculates duplicate tower copies needed to reach targetLevel from targetLevel - 1.
     * [OOP / PATTERN] Static Lookup Method / Array Indexing Mapping.
     * [CALLED BY]     btd/core/GameSession.java (upgradeCost()).
     * [CONNECTS TO]   btd/core/GameConfig.Economy.upgradeCopyCosts.
     * [DEFENSE NOTE]  Target levels range from 2 to 5; maps to index (targetLevel - 2) in JSON cost array.
     */
    public static int copiesNeeded(GameConfig config, Rarity rarity, int targetLevel) {
        int[] costs = config.economy.upgradeCopyCosts.get(rarity.name());
        return costs[targetLevel - 2];
    }
}
