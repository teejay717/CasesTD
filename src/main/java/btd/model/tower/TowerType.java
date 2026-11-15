package btd.model.tower;

import btd.model.gacha.Rarity;

/**
 * [PURPOSE]       The BLUEPRINT of a tower: one row of the 'towers' table (name, class, rarity, damage, range...).
 *                 Inventory cards are TowerTypes; a placed Tower object holds a reference to its TowerType.
 * [OOP / PATTERN] Java Record / Immutable Data / Type Object (blueprint vs. placed instance).
 * [CALLED BY]     Created in btd/dao/TowerDAO.java (findAll()). Used almost everywhere: TowerFactory, Tower,
 *                 GameSession (inventory), CaseDef (pool), TowerArt and the UI screens.
 * [CONNECTS TO]   btd/model/gacha/Rarity.java, btd/model/tower/Special.java, btd/model/tower/Tower.java.
 * [DEFENSE NOTE]  A record automatically gets a constructor, getters named like the fields (t.name(), t.range()),
 *                 equals, hashCode and toString. Fields are final, so the blueprint can never be changed in game.
 */
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
