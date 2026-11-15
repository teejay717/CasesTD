package btd.model.gacha;

import btd.model.tower.TowerType;

import java.util.List;

/**
 * [PURPOSE]       One case from the 'cases' table: name, cost, the 4 rarity weights, and its pool of towers.
 * [OOP / PATTERN] Java Record / Immutable Data.
 * [CALLED BY]     Created in btd/dao/CaseDAO.java (findAll()); used by btd/core/CaseRNGManager.java (roll()),
 *                 btd/core/GameSession.java (buyCase()) and btd/ui/screens/ShopOverlay.java.
 * [CONNECTS TO]   btd/model/gacha/Rarity.java, btd/model/tower/TowerType.java.
 * [DEFENSE NOTE]  The weights are percentages; the DB CHECK constraint (chk_weights_sum) forces them to add up to 100.
 *                 'pool' is filled from the case_towers join table (many-to-many: a tower can be in many cases).
 */
public record CaseDef(
        int id,
        String name,
        int cost,
        double commonWeight,
        double rareWeight,
        double epicWeight,
        double legendaryWeight,
        List<TowerType> pool) {

    /**
     * [PURPOSE]       Returns numerical percentage drop weight configured for given rarity tier.
     * [OOP / PATTERN] Switch expression on an enum.
     * [CALLED BY]     btd/core/CaseRNGManager.java (rollRarity()), btd/ui/screens/ShopOverlay.java (paintCases(), "Drop chances").
     * [CONNECTS TO]   btd/model/gacha/Rarity.java.
     * [DEFENSE NOTE]  Lets other code loop over Rarity.values() instead of writing 4 if-else checks.
     *                 The switch covers all 4 enum values, so the compiler needs no default branch.
     */
    public double weightOf(Rarity rarity) {
        return switch (rarity) {
            case COMMON -> commonWeight;
            case RARE -> rareWeight;
            case EPIC -> epicWeight;
            case LEGENDARY -> legendaryWeight;
        };
    }
}
