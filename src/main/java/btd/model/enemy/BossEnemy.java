package btd.model.enemy;

/**
 * [PURPOSE]       Colossal boss enemy spawning every 10th wave with massive HP (1200) and 50% stun resistance.
 * [OOP / PATTERN] Concrete Subclass / Specialization / Method Overriding.
 * [CALLED BY]     btd/factory/EnemyFactory.java (create()).
 * [CONNECTS TO]   btd/model/enemy/Enemy.java (super class).
 * [DEFENSE NOTE]  Leaking this boss costs 10 base health points, posing an immediate run-ending threat.
 */
public class BossEnemy extends Enemy {

    /**
     * [PURPOSE]       Constructs a BossEnemy with 1200 base health, 32 speed, and 10 leak penalty.
     * [OOP / PATTERN] Constructor Chaining via super().
     * [CALLED BY]     btd/factory/EnemyFactory.java.
     * [CONNECTS TO]   btd/model/enemy/Enemy.java constructor.
     * [DEFENSE NOTE]  Features high health scaling to force players to build and upgrade multiple synergy towers.
     */
    public BossEnemy(double healthScale, double speedScale, int reward) {
        super(1200 * healthScale, 32 * speedScale, reward, 10);
    }

    /**
     * [PURPOSE]       Applies stun with 50% duration reduction (tenacity resistance).
     * [OOP / PATTERN] Method Overriding with super call (Polymorphic behavioral modification).
     * [CALLED BY]     btd/model/tower/SupportTower.java (stunAll()).
     * [CONNECTS TO]   btd/model/enemy/Enemy.java (stun()).
     * [DEFENSE NOTE]  Demonstrates behavioral override: divides duration by 2 before passing to super.stun().
     */
    @Override
    public boolean stun(double seconds) {
        return super.stun(seconds / 2);
    }

    /**
     * [PURPOSE]       Returns collision radius for BossEnemy (24 pixels).
     * [OOP / PATTERN] Method Overriding (Polymorphism).
     * [CALLED BY]     btd/ui/screens/GamePanel.java (paintEnemyCircle()).
     * [CONNECTS TO]   btd/model/enemy/Enemy.java.
     * [DEFENSE NOTE]  Massive 24px radius visually dominates the track and guarantees beam hits.
     */
    @Override
    public double getRadius() {
        return 24;
    }
}
