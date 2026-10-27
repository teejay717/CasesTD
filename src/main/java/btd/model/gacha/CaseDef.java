package btd.model.gacha;

import btd.model.tower.TowerType;

import java.util.List;

/** One row of the cases table plus the towers that can drop from it. Weights are percentages. */
public record CaseDef(
        int id,
        String name,
        int cost,
        double commonWeight,
        double rareWeight,
        double epicWeight,
        double legendaryWeight,
        List<TowerType> pool) {

    public double weightOf(Rarity rarity) {
        return switch (rarity) {
            case COMMON -> commonWeight;
            case RARE -> rareWeight;
            case EPIC -> epicWeight;
            case LEGENDARY -> legendaryWeight;
        };
    }
}
