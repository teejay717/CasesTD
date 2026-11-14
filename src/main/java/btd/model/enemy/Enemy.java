package btd.model.enemy;

import btd.core.GameMap;

/**
 * [PURPOSE]       Abstract base class representing an enemy walking the waypoint path.
 *                 Manages waypoints, rotation, DoT status effects (poison, burn), and stuns.
 * [OOP / PATTERN] Abstract Class / Template Method Pattern / Inheritance & Encapsulation.
 * [CALLED BY]     btd/core/WaveSpawner.java, btd/factory/EnemyFactory.java, btd/core/GameEngine.java.
 * [CONNECTS TO]   btd/model/enemy/Damageable.java, btd/core/GameMap.java,
 *                 btd/ui/screens/GamePanel.java.
 * [DEFENSE NOTE]  Encapsulates physics, status effect timers, and rotation interpolation
 *                 so subclasses only specify baseline stats and unique damage mitigations.
 */
public abstract class Enemy implements Damageable {

    private static final int MAX_BURN_STACKS = 5;

    /** After a stun wears off, the enemy can't be stunned again for this long, so stuns can never chain forever. */
    public static final double STUN_IMMUNITY_SECONDS = 1.5;

    private final double maxHealth;
    private final double speed;
    private final int reward;
    private final int leakDamage;

    private double health;
    private double distance;      // how far along the track, in pixels
    private double x;
    private double y;
    private double angle;         // walking direction in radians

    private double stunLeft;
    private double stunImmuneLeft;
    private double poisonDps;
    private double poisonLeft;
    private int burnStacks;
    private double burnDpsPerStack;
    private double burnLeft;

    /**
     * [PURPOSE]       Initializes base attributes for an enemy instance.
     * [OOP / PATTERN] Superclass Constructor / State Initialization.
     * [CALLED BY]     Subclass constructors (BasicEnemy, FastEnemy, ArmoredEnemy, BossEnemy).
     * [CONNECTS TO]   Internal attribute state.
     * [DEFENSE NOTE]  Guarantees all enemy instances have immutable speed, maxHealth, and leak penalties.
     */
    protected Enemy(double maxHealth, double speed, int reward, int leakDamage) {
        this.maxHealth = maxHealth;
        this.health = maxHealth;
        this.speed = speed;
        this.reward = reward;
        this.leakDamage = leakDamage;
    }

    /**
     * [PURPOSE]       Returns simple class name for asset matching and telemetry.
     * [OOP / PATTERN] Reflection / Polymorphic Identifier.
     * [CALLED BY]     btd/ui/screens/GamePanel.java (paintEnemy()).
     * [CONNECTS TO]   btd/util/SpriteLoader.java (dynamic sprite lookup).
     * [DEFENSE NOTE]  Dynamically resolves sprite paths like 'BasicEnemy_leftfoot.png' without hardcoding.
     */
    public String getTypeName() {
        return getClass().getSimpleName();
    }

    /**
     * [PURPOSE]       Abstract accessor for the enemy collision and rendering radius.
     * [OOP / PATTERN] Abstract Method (Subclass Responsibility).
     * [CALLED BY]     btd/ui/screens/GamePanel.java (paintEnemyCircle()),
     *                 btd/model/tower/SniperTower.java (onBeam()).
     * [CONNECTS TO]   Subclass hitbox configurations.
     * [DEFENSE NOTE]  Allows larger enemies (Boss) to have larger hitboxes for piercing beams and visuals.
     */
    public abstract double getRadius();

    /**
     * [PURPOSE]       Updates status effect timers, deals DoT ticks, moves along the path, and computes heading angle.
     * [OOP / PATTERN] Simulation Tick / Vector Heading Calculation.
     * [CALLED BY]     btd/core/GameEngine.java (updateEnemies()).
     * [CONNECTS TO]   btd/core/GameMap.java (pointAt()).
     * [DEFENSE NOTE]  Heading angle is computed by sampling 1 pixel ahead on the track via Math.atan2,
     *                 enabling smooth sprite orientation around corners.
     */
    public void update(double dt, GameMap map) {
        // damage over time
        if (poisonLeft > 0) {
            takeDamage(poisonDps * dt);
            poisonLeft -= dt;
        }
        if (burnLeft > 0) {
            takeDamage(burnDpsPerStack * burnStacks * dt);
            burnLeft -= dt;
            if (burnLeft <= 0) {
                burnStacks = 0;
            }
        }
        // movement
        if (stunLeft > 0) {
            stunLeft -= dt;
            if (stunLeft <= 0) {
                stunImmuneLeft = STUN_IMMUNITY_SECONDS;
            }
        } else {
            if (stunImmuneLeft > 0) {
                stunImmuneLeft -= dt;
            }
            distance += speed * dt;
        }
        double[] p = map.pointAt(distance);
        // face along the track by looking 1 pixel ahead (0 = right, PI/2 = down)
        double[] ahead = map.pointAt(distance + 1);
        angle = Math.atan2(ahead[1] - p[1], ahead[0] - p[0]);
        x = p[0];
        y = p[1];
    }

    /**
     * [PURPOSE]       Reduces health by amount after applying class-specific mitigation.
     * [OOP / PATTERN] Template Method Pattern (invokes reduceDamage hook).
     * [CALLED BY]     btd/model/tower/*.java, internal DoT tick.
     * [CONNECTS TO]   btd/model/enemy/Enemy.java (reduceDamage()).
     * [DEFENSE NOTE]  Encapsulates health reduction while delegating armor mitigation to subclasses.
     */
    @Override
    public void takeDamage(double amount) {
        health -= reduceDamage(amount);
    }

    /**
     * [PURPOSE]       Hook method allowing subclasses to modify or reduce raw incoming damage.
     * [OOP / PATTERN] Hook Method (Template Method Pattern).
     * [CALLED BY]     btd/model/enemy/Enemy.java (takeDamage()).
     * [CONNECTS TO]   btd/model/enemy/ArmoredEnemy.java (overrides for 30% armor reduction).
     * [DEFENSE NOTE]  Default implementation is an identity function (100% damage passed through).
     */
    protected double reduceDamage(double amount) {
        return amount;
    }

    /**
     * [PURPOSE]       Returns true when enemy health reaches 0 or below.
     * [OOP / PATTERN] State Query / Polymorphic implementation.
     * [CALLED BY]     btd/core/GameEngine.java, btd/model/tower/Tower.java.
     * [CONNECTS TO]   Game engine death processing.
     * [DEFENSE NOTE]  Simple boolean condition to safeguard against ghost entity processing.
     */
    @Override
    public boolean isDead() {
        return health <= 0;
    }

    /**
     * [PURPOSE]       Evaluates whether the enemy can currently receive a stun effect.
     * [OOP / PATTERN] State Guard Predicate.
     * [CALLED BY]     btd/model/tower/SupportTower.java (stunAll()), btd/model/enemy/Enemy.java (stun()).
     * [CONNECTS TO]   Stun cooldown and immunity state variables.
     * [DEFENSE NOTE]  Prevents infinite perma-stun locks by enforcing an immunity cooldown window.
     */
    public boolean canBeStunned() {
        return !isDead() && stunLeft <= 0 && stunImmuneLeft <= 0;
    }

    /**
     * [PURPOSE]       Applies stun duration if the enemy is not currently immune.
     * [OOP / PATTERN] State Modifier with Guard Clause.
     * [CALLED BY]     btd/model/tower/SupportTower.java (stunAll()).
     * [CONNECTS TO]   btd/model/enemy/BossEnemy.java (overrides to halve stun duration).
     * [DEFENSE NOTE]  Returns boolean indicating whether stun was successfully landed or resisted.
     */
    public boolean stun(double seconds) {
        if (!canBeStunned()) {
            return false;
        }
        stunLeft = seconds;
        return true;
    }

    /**
     * [PURPOSE]       Applies or refreshes poison DoT damage on this enemy.
     * [OOP / PATTERN] Status Effect Modifier (takes maximum DPS).
     * [CALLED BY]     btd/model/tower/SplashTower.java (applyEffect()).
     * [CONNECTS TO]   Internal poison timers.
     * [DEFENSE NOTE]  Uses Math.max to prevent weaker poisons from overwriting stronger active ones.
     */
    public void poison(double dps, double seconds) {
        poisonDps = Math.max(poisonDps, dps);
        poisonLeft = Math.max(poisonLeft, seconds);
    }

    /**
     * [PURPOSE]       Applies burn DoT, stacking up to 5 times and refreshing duration.
     * [OOP / PATTERN] Stacking Buff/Debuff Mechanic.
     * [CALLED BY]     btd/model/tower/SplashTower.java (applyEffect()).
     * [CONNECTS TO]   Internal burn stack counters.
     * [DEFENSE NOTE]  Stacks up to MAX_BURN_STACKS (5), multiplying burn damage per tick.
     */
    public void burn(double dpsPerStack, double seconds) {
        burnDpsPerStack = dpsPerStack;
        burnStacks = Math.min(MAX_BURN_STACKS, burnStacks + 1);
        burnLeft = Math.max(burnLeft, seconds);
    }

    /**
     * [PURPOSE]       Checks if the enemy traveled the full length of the waypoint track.
     * [OOP / PATTERN] Boundary Check / Predicate.
     * [CALLED BY]     btd/core/GameEngine.java (updateEnemies()).
     * [CONNECTS TO]   btd/core/GameMap.java (getTotalLength()), btd/core/GameSession.java (leakHealth()).
     * [DEFENSE NOTE]  When true, the engine removes the enemy and subtracts leakDamage from base health.
     */
    public boolean hasReachedEnd(GameMap map) {
        return distance >= map.getTotalLength();
    }

    /** [PURPOSE] Accessor for current remaining health points. */
    public double getHealth() { return health; }
    /** [PURPOSE] Accessor for maximum possible health points. */
    public double getMaxHealth() { return maxHealth; }
    /** [PURPOSE] Accessor for distance traveled along the track in pixels. */
    public double getDistance() { return distance; }
    /** [PURPOSE] Accessor for current X coordinate on map. */
    public double getX() { return x; }
    /** [PURPOSE] Accessor for current Y coordinate on map. */
    public double getY() { return y; }
    /** [PURPOSE] Accessor for heading direction angle in radians. */
    public double getAngle() { return angle; }
    /** [PURPOSE] Accessor for gold awarded to player upon defeat. */
    public int getReward() { return reward; }
    /** [PURPOSE] Accessor for base HP subtracted if this enemy leaks. */
    public int getLeakDamage() { return leakDamage; }
    /** [PURPOSE] Query whether enemy is currently stunned. */
    public boolean isStunned() { return stunLeft > 0; }
    /** [PURPOSE] Query whether enemy is suffering from poison. */
    public boolean isPoisoned() { return poisonLeft > 0; }
    /** [PURPOSE] Query whether enemy is suffering from burn stacks. */
    public boolean isBurning() { return burnLeft > 0; }
}
