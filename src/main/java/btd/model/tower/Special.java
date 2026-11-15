package btd.model.tower;

/**
 * [PURPOSE]       The special ability of a tower (the 'special' column in the towers table).
 *                 STUN = Policeman, POISON = Scientist, BURN = Flamethrower, BUFF = Radio Tower,
 *                 TURRETS = Engineer, PIERCE = Railgunner, NONE = everyone else.
 * [OOP / PATTERN] Enum / Type Safety.
 * [CALLED BY]     btd/dao/TowerDAO.java (Special.valueOf(...) turns the DB text into the enum),
 *                 btd/model/tower/SplashTower.java, SniperTower.java, SupportTower.java (switch on it).
 * [CONNECTS TO]   btd/model/tower/TowerType.java.
 * [DEFENSE NOTE]  A typo like Special.STUNN does not compile, but a typo in a String "stunn" would only fail at runtime.
 */
public enum Special {
    NONE, STUN, POISON, BURN, BUFF, TURRETS, PIERCE
}
