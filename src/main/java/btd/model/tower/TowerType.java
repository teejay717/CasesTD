package btd.model.tower;

import btd.model.gacha.Rarity;

/**
 * [PURPOSE]       Immutable data record modeling a tower specification row from the database 'towers' table.
 * [OOP / PATTERN] Java Record / Data Transfer Object (DTO) / Immutability.
 * [CALLED BY]     btd/dao/TowerDAO.java (getAllTowers()), btd/factory/TowerFactory.java.
 * [CONNECTS TO]   btd/model/gacha/Rarity.java, btd/model/tower/Special.java,
 *                 btd/model/tower/Tower.java.
 * [DEFENSE NOTE]  Java 21 record automatically generates immutable getters, equals, hashCode, and toString.
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
