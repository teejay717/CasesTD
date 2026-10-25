package btd.core;

import btd.model.gacha.Rarity;

/** How many spare copies of a tower it costs to upgrade it, depending on rarity and target level. */
public final class UpgradeCostTable {

    private UpgradeCostTable() {
    }

    /** Copies needed to go from (targetLevel - 1) to targetLevel. Target level is 2 to 5. */
    public static int copiesNeeded(GameConfig config, Rarity rarity, int targetLevel) {
        int[] costs = config.economy.upgradeCopyCosts.get(rarity.name());
        return costs[targetLevel - 2];
    }
}
