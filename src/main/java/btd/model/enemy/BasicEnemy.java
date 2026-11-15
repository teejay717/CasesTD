package btd.model.enemy;

/**
 * [PURPOSE]       Standard baseline zombie with balanced health, movement speed, and standard reward.
 * [OOP / PATTERN] Concrete Subclass / Specialization / Inheritance.
 * [CALLED BY]     btd/factory/EnemyFactory.java (create()).
 * [CONNECTS TO]   btd/model/enemy/Enemy.java (super class).
 * [DEFENSE NOTE]  Acts as the control/baseline unit against which all tower DPS formulas are tuned.
 */
public class BasicEnemy extends Enemy {

    /**
     * [PURPOSE]       Constructs a BasicEnemy with baseline stats scaled by wave modifiers.
     * [OOP / PATTERN] Constructor Chaining via super().
     * [CALLED BY]     btd/factory/EnemyFactory.java.
     * [CONNECTS TO]   btd/model/enemy/Enemy.java constructor (30 HP, 55 speed, reward, 1 leak).
     * [DEFENSE NOTE]  Health and speed scales grow progressively with wave count in WaveSpawner.
     */
    public BasicEnemy(double healthScale, double speedScale, int reward) {
        super(30 * healthScale, 55 * speedScale, reward, 1);
    }

    /**
     * [PURPOSE]       Returns collision and rendering radius for BasicEnemy (11 pixels).
     * [OOP / PATTERN] Method Overriding (Polymorphism).
     * [CALLED BY]     btd/ui/screens/GamePanel.java (paintEnemy()), btd/model/tower/SniperTower.java (onBeam()).
     * [CONNECTS TO]   btd/model/enemy/Enemy.java (abstract method implementation).
     * [DEFENSE NOTE]  Standard size hitbox representing average zombie profile.
     */
    @Override
    public double getRadius() {
        return 11;
    }
}
