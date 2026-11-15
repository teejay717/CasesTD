package btd.model.enemy;

/**
 * [PURPOSE]       The normal zombie: 30 HP, 55 px/s, 1 life if it leaks, 10 coins (from waves_config.json).
 * [OOP / PATTERN] Concrete Subclass / Inheritance.
 * [CALLED BY]     btd/factory/EnemyFactory.java (create()).
 * [CONNECTS TO]   btd/model/enemy/Enemy.java (super class).
 * [DEFENSE NOTE]  The simplest subclass: it only passes numbers to super() and gives a radius. All behavior is inherited.
 */
public class BasicEnemy extends Enemy {

    /**
     * [PURPOSE]       Constructs a BasicEnemy with baseline stats scaled by wave modifiers.
     * [OOP / PATTERN] Constructor Chaining via super().
     * [CALLED BY]     btd/factory/EnemyFactory.java.
     * [CONNECTS TO]   btd/model/enemy/Enemy.java constructor (30 HP, 55 speed, reward, 1 leak).
     * [DEFENSE NOTE]  30 and 55 are the BASE stats; healthScale and speedScale (from GameConfig) make later waves stronger.
     */
    public BasicEnemy(double healthScale, double speedScale, int reward) {
        super(30 * healthScale, 55 * speedScale, reward, 1);
    }

    /**
     * [PURPOSE]       Radius 11 px (used for drawing size and the Railgunner beam check).
     * [OOP / PATTERN] Method Overriding (implements Enemy's abstract method).
     * [CALLED BY]     btd/ui/screens/GamePanel.java (paintEnemy()), btd/model/tower/SniperTower.java (onBeam()).
     * [CONNECTS TO]   btd/model/enemy/Enemy.java (abstract method implementation).
     */
    @Override
    public double getRadius() {
        return 11;
    }
}
