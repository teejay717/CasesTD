package btd.model.tower;

/**
 * [PURPOSE]       Enumeration of distinct special abilities or status effects attached to towers.
 * [OOP / PATTERN] Enum Pattern / Type Safety.
 * [CALLED BY]     btd/dao/TowerDAO.java, btd/model/tower/TowerType.java,
 *                 btd/model/tower/SplashTower.java, btd/model/tower/SniperTower.java,
 *                 btd/model/tower/SupportTower.java.
 * [CONNECTS TO]   btd/model/tower/TowerType.java.
 * [DEFENSE NOTE]  Replaces magic strings with compile-time type safety for special behaviors.
 */
public enum Special {
    NONE, STUN, POISON, BURN, BUFF, TURRETS, PIERCE
}
