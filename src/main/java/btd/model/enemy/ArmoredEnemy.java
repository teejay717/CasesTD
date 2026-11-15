package btd.model.enemy;

/**
 * [PURPOSE]       Tank zombie: 90 HP, 40 px/s, takes only 70% of every hit, 2 lives if it leaks, 20 coins.
 *                 Appears from wave 5.
 * [OOP / PATTERN] Concrete Subclass / Hook Method Overriding (reduceDamage()) / Template Method.
 * [CALLED BY]     btd/factory/EnemyFactory.java (create()).
 * [CONNECTS TO]   btd/model/enemy/Enemy.java (super class).
 * [DEFENSE NOTE]  Armor also reduces poison and burn, because those go through takeDamage() too.
 */
public class ArmoredEnemy extends Enemy {

    private static final double DAMAGE_TAKEN = 0.7;

    /**
     * [PURPOSE]       Constructs an ArmoredEnemy with elevated health (90) and low speed (40).
     * [OOP / PATTERN] Constructor Chaining via super().
     * [CALLED BY]     btd/factory/EnemyFactory.java.
     * [CONNECTS TO]   btd/model/enemy/Enemy.java constructor (leak damage 2).
     * [DEFENSE NOTE]  Possesses 3x the base health of basic enemies with double leak penalty.
     */
    public ArmoredEnemy(double healthScale, double speedScale, int reward) {
        super(90 * healthScale, 40 * speedScale, reward, 2);
    }

    /**
     * [PURPOSE]       Reduces all incoming damage to 70% (30% flat damage resistance).
     * [OOP / PATTERN] Hook Method Overriding (Polymorphic damage mitigation).
     * [CALLED BY]     btd/model/enemy/Enemy.java (takeDamage()).
     * [CONNECTS TO]   Overrides Enemy.reduceDamage() (which normally returns the damage unchanged).
     * [DEFENSE NOTE]  Hook method: ArmoredEnemy changes one step without copying or changing takeDamage().
     *                 Example: a 10 damage hit only removes 7 health.
     */
    @Override
    protected double reduceDamage(double amount) {
        return amount * DAMAGE_TAKEN;
    }

    /**
     * [PURPOSE]       Returns collision radius for ArmoredEnemy (13 pixels).
     * [OOP / PATTERN] Method Overriding (Polymorphism).
     * [CALLED BY]     btd/ui/screens/GamePanel.java (paintEnemy()), btd/model/tower/SniperTower.java (onBeam()).
     * [CONNECTS TO]   btd/model/enemy/Enemy.java.
     * [DEFENSE NOTE]  A bigger radius makes it look bigger and easier for the Railgunner beam to touch.
     *                 Splash damage does not use radius (it checks the enemy's center point).
     */
    @Override
    public double getRadius() {
        return 13;
    }
}
