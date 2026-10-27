package btd.model.tower;

import btd.model.gacha.Rarity;

/** One row of the towers table: the fixed stats of a kind of tower. */
public record TowerType(
        int id,
        String name,
        String towerClass,
        Rarity rarity,
        int baseDamage,
        int range,
        double fireRate,
        int splashRadius,
        Special special,
        double effectValue,
        int effectDurationMs,
        String description,
        String spritePath) {
}
