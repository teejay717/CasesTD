package btd.model.gacha;

import btd.model.tower.TowerType;

import java.util.List;

/**
 * [PURPOSE]       Immutable data model representing a gacha case definition, drop weights, and available tower pool.
 * [OOP / PATTERN] Java Record / Data Transfer Object (DTO) / Immutability.
 * [CALLED BY]     btd/dao/CaseDAO.java (getAllCases()), btd/core/CaseRNGManager.java (rollCase()).
 * [CONNECTS TO]   btd/model/gacha/Rarity.java, btd/model/tower/TowerType.java.
 * [DEFENSE NOTE]  Encapsulates drop rate weights (percentages summing to 100%) and pool membership.
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
     * [OOP / PATTERN] Pattern Matching Switch / Encapsulated Lookup.
     * [CALLED BY]     btd/core/CaseRNGManager.java (rollCase()).
     * [CONNECTS TO]   btd/model/gacha/Rarity.java.
     * [DEFENSE NOTE]  Eliminates repetitive if-else chains when calculating cumulative probability distributions.
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
