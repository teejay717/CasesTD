package btd.model.enemy;

/**
 * [PURPOSE]       Fast, weak zombie: 18 HP, 105 px/s, 1 life if it leaks, 7 coins. First appears in wave 3.
 * [OOP / PATTERN] Concrete Subclass / Inheritance.
 * [CALLED BY]     btd/factory/EnemyFactory.java (create()).
 * [CONNECTS TO]   btd/model/enemy/Enemy.java (super class).
 * [DEFENSE NOTE]  Towers hit instantly (no flying bullets), so speed matters because it spends less time inside
 *                 each tower's range. Slow-firing towers like the Sniper get fewer shots at it.
 */
public class FastEnemy extends Enemy {

    /**
     * [PURPOSE]       Constructs a FastEnemy with high speed (105) and low health (18).
     * [OOP / PATTERN] Constructor Chaining via super().
     * [CALLED BY]     btd/factory/EnemyFactory.java.
     * [CONNECTS TO]   btd/model/enemy/Enemy.java constructor.
     * [DEFENSE NOTE]  Moves almost twice as fast as BasicEnemy (105 vs 55 pixels/second).
     */
    public FastEnemy(double healthScale, double speedScale, int reward) {
        super(18 * healthScale, 105 * speedScale, reward, 1);
    }

    /**
     * [PURPOSE]       Radius 9 px, the smallest (used for drawing size and the Railgunner beam check).
     * [OOP / PATTERN] Method Overriding (Polymorphism).
     * [CALLED BY]     btd/ui/screens/GamePanel.java (paintEnemy()), btd/model/tower/SniperTower.java (onBeam()).
     * [CONNECTS TO]   btd/model/enemy/Enemy.java.
     */
    @Override
    public double getRadius() {
        return 9;
    }
}
