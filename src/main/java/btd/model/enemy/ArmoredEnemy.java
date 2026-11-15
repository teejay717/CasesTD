package btd.model.enemy;

/**
 * [PURPOSE]       Heavy tank zombie with thick armor that softens all incoming hits by 30%.
 * [OOP / PATTERN] Concrete Subclass / Hook Method Overriding / Template Method Pattern.
 * [CALLED BY]     btd/factory/EnemyFactory.java (create()).
 * [CONNECTS TO]   btd/model/enemy/Enemy.java (super class).
 * [DEFENSE NOTE]  Deals 2 base damage upon leak and requires high-damage single hits (e.g., Snipers) to counter.
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
     * [CONNECTS TO]   btd/model/enemy/Damageable.java.
     * [DEFENSE NOTE]  Classic OOP Hook implementation: alters algorithmic step without modifying takeDamage() structure.
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
     * [DEFENSE NOTE]  Larger hitbox (13px) makes it easier for piercing beams to strike (splash uses the enemy's center point, not radius).
     */
    @Override
    public double getRadius() {
        return 13;
    }
}
