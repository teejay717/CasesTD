package btd.model.enemy;

/**
 * [PURPOSE]       Core contract defining any game entity capable of receiving damage and dying.
 * [OOP / PATTERN] Interface Abstraction / Dependency Inversion Principle.
 * [CALLED BY]     btd/model/tower/SingleTargetTower.java, btd/model/tower/SplashTower.java,
 *                 btd/model/tower/SniperTower.java, btd/model/tower/SupportTower.java.
 * [CONNECTS TO]   btd/model/enemy/Enemy.java (implements this contract).
 * [DEFENSE NOTE]  Decouples tower targeting and projectile logic from specific enemy classes,
 *                 allowing polymorphic damage application across all enemy variants.
 */
public interface Damageable {

    /**
     * [PURPOSE]       Reduces entity health by the calculated incoming damage value.
     * [OOP / PATTERN] Abstract Method / Polymorphic invocation.
     * [CALLED BY]     btd/model/tower/SingleTargetTower.java (attack()),
     *                 btd/model/tower/SplashTower.java (attack()),
     *                 btd/model/tower/SniperTower.java (attack(), pierce()),
     *                 btd/model/tower/SupportTower.java (stunAll(), fireTurrets()).
     * [CONNECTS TO]   Concrete entity health pool.
     * [DEFENSE NOTE]  Towers call this uniformly without needing to know if the target is basic,
     *                 fast, armored, or a boss.
     */
    void takeDamage(double amount);

    /**
     * [PURPOSE]       Queries whether the entity's health has reached zero or below.
     * [OOP / PATTERN] State Query / Predicate Method.
     * [CALLED BY]     btd/core/GameEngine.java (update()),
     *                 btd/model/tower/Tower.java (furthestInRange()).
     * [CONNECTS TO]   btd/core/GameSession.java (addCoins(), called by GameEngine).
     * [DEFENSE NOTE]  Used by the engine loop to remove defeated units and trigger kill rewards.
     */
    boolean isDead();
}
