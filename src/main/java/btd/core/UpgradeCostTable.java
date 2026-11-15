package btd.core;

import btd.model.gacha.Rarity;

/**
 * [PURPOSE]       Looks up how many spare copies of a tower you must spend to upgrade it (to Level 2, 3, 4 or 5).
 * [OOP / PATTERN] Utility Class (final + private constructor, only static methods) / Single Responsibility.
 * [CALLED BY]     btd/core/GameSession.java (upgradeCost()).
 * [CONNECTS TO]   btd/core/GameConfig.java (economy.upgradeCopyCosts), btd/model/gacha/Rarity.java.
 * [DEFENSE NOTE]  The numbers live in waves_config.json, e.g. "COMMON": [1, 1, 2, 3], not inside the Tower classes.
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
     * [DEFENSE NOTE]  Target levels range from 2 to 5, so index = targetLevel - 2.
     *                 Example: COMMON to Level 4 -> costs[2] = 2 copies.
     */
    public static int copiesNeeded(GameConfig config, Rarity rarity, int targetLevel) {
        int[] costs = config.economy.upgradeCopyCosts.get(rarity.name());
        return costs[targetLevel - 2];
    }
}
