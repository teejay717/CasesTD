package btd.model.enemy;

/**
 * [PURPOSE]       High-speed, low-health scout zombie designed to breach defenses before heavy towers can fire.
 * [OOP / PATTERN] Concrete Subclass / Specialization / Inheritance.
 * [CALLED BY]     btd/factory/EnemyFactory.java (create()).
 * [CONNECTS TO]   btd/model/enemy/Enemy.java (super class).
 * [DEFENSE NOTE]  Tests tower fire rate and projectile velocity; fast movement counters slow-reloading snipers.
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
     * [PURPOSE]       Returns collision and rendering radius for FastEnemy (9 pixels).
     * [OOP / PATTERN] Method Overriding (Polymorphism).
     * [CALLED BY]     btd/ui/screens/GamePanel.java (paintEnemyCircle()).
     * [CONNECTS TO]   btd/model/enemy/Enemy.java.
     * [DEFENSE NOTE]  Smaller hitbox (9px) visually and mechanically reflects agile build.
     */
    @Override
    public double getRadius() {
        return 9;
    }
}
