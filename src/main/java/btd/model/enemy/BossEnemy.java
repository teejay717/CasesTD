package btd.model.enemy;

/**
 * [PURPOSE]       Boss zombie, one per 10th wave: 1200 HP, 32 px/s, stuns last half as long, 500 coins.
 * [OOP / PATTERN] Concrete Subclass / Method Overriding (stun(), getRadius()).
 * [CALLED BY]     btd/factory/EnemyFactory.java (create(), when WaveSpawner.begin() queued a "BossEnemy").
 * [CONNECTS TO]   btd/model/enemy/Enemy.java (super class).
 * [DEFENSE NOTE]  Leaking it costs 10 lives, half of the 20 starting lives.
 */
public class BossEnemy extends Enemy {

    /**
     * [PURPOSE]       Constructs a BossEnemy with 1200 base health, 32 speed, and 10 leak penalty.
     * [OOP / PATTERN] Constructor Chaining via super().
     * [CALLED BY]     btd/factory/EnemyFactory.java.
     * [CONNECTS TO]   btd/model/enemy/Enemy.java constructor.
     * [DEFENSE NOTE]  1200 is the base; the wave's healthScale is applied too, so the wave 20 boss is much tougher than wave 10's.
     */
    public BossEnemy(double healthScale, double speedScale, int reward) {
        super(1200 * healthScale, 32 * speedScale, reward, 10);
    }

    /**
     * [PURPOSE]       Applies stun with 50% duration reduction (tenacity resistance).
     * [OOP / PATTERN] Method Overriding with super call (Polymorphic behavioral modification).
     * [CALLED BY]     btd/model/tower/SupportTower.java (stunAll()).
     * [CONNECTS TO]   btd/model/enemy/Enemy.java (stun()).
     * [DEFENSE NOTE]  Override + super call: it halves the time, then lets Enemy.stun() do the normal work.
     *                 A 2.0 s stun becomes 1.0 s on the boss.
     */
    @Override
    public boolean stun(double seconds) {
        return super.stun(seconds / 2);
    }

    /**
     * [PURPOSE]       Returns collision radius for BossEnemy (24 pixels).
     * [OOP / PATTERN] Method Overriding (Polymorphism).
     * [CALLED BY]     btd/ui/screens/GamePanel.java (paintEnemy()), btd/model/tower/SniperTower.java (onBeam()).
     * [CONNECTS TO]   btd/model/enemy/Enemy.java.
     * [DEFENSE NOTE]  The biggest enemy, so the Railgunner beam touches it most easily.
     *                 GamePanel draws the boss sprite at 4x radius (others 6x) because its art fills more of the image.
     */
    @Override
    public double getRadius() {
        return 24;
    }
}
