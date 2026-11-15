package btd.model.enemy;

import btd.core.GameMap;

/**
 * [PURPOSE]       Abstract parent of every zombie. Handles walking the path, facing direction,
 *                 health, poison, burn, and stun (with immunity afterwards).
 * [OOP / PATTERN] Abstract Class / Inheritance / Encapsulation / Template Method (takeDamage() calls reduceDamage()).
 * [CALLED BY]     Created by btd/factory/EnemyFactory.java; updated by btd/core/GameEngine.java;
 *                 hit by the tower classes; drawn by btd/ui/screens/GamePanel.java (paintEnemy()).
 * [CONNECTS TO]   btd/model/enemy/Damageable.java, btd/core/GameMap.java.
 * [DEFENSE NOTE]  An enemy only stores 'distance' (pixels walked). GameMap.pointAt(distance) turns it into x, y.
 *                 Subclasses just pass their stats to super(...) and override getRadius() (and maybe one more method).
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
     * [DEFENSE NOTE]  maxHealth, speed, reward and leakDamage are final: set once here, never changed.
     *                 protected = only subclasses can call it (through super(...)).
     */
    protected Enemy(double maxHealth, double speed, int reward, int leakDamage) {
        this.maxHealth = maxHealth;
        this.health = maxHealth;
        this.speed = speed;
        this.reward = reward;
        this.leakDamage = leakDamage;
    }

    /**
     * [PURPOSE]       Returns the class name, e.g. "BasicEnemy" or "BossEnemy".
     * [OOP / PATTERN] getClass() returns the REAL subclass at runtime, even though this code is in Enemy.
     * [CALLED BY]     btd/ui/screens/GamePanel.java (paintEnemy(), paintEnemyCircle()).
     * [CONNECTS TO]   btd/util/SpriteLoader.java (through the sprite path GamePanel builds).
     * [DEFENSE NOTE]  GamePanel builds "assets/sprites/enemies/" + getTypeName() + "_leftfoot.png", so a new enemy
     *                 class only needs PNG files with the same name; no drawing code has to change.
     */
    public String getTypeName() {
        return getClass().getSimpleName();
    }

    /**
     * [PURPOSE]       Size of the enemy in pixels (Fast 9, Basic 11, Armored 13, Boss 24).
     * [OOP / PATTERN] Abstract Method (every subclass MUST give its own value).
     * [CALLED BY]     btd/ui/screens/GamePanel.java (paintEnemy(): sprite size, health bar, status rings),
     *                 btd/model/tower/SniperTower.java (onBeam(): bigger enemies are easier for the beam to touch).
     * [CONNECTS TO]   Each subclass's getRadius().
     * [DEFENSE NOTE]  Normal tower range checks use the enemy's CENTER point, so radius only matters for drawing and the beam.
     */
    public abstract double getRadius();

    /**
     * [PURPOSE]       One step for this enemy: 1) poison/burn damage, 2) walk (unless stunned), 3) find x, y and angle.
     * [OOP / PATTERN] Simulation Step.
     * [CALLED BY]     btd/core/GameEngine.java (update()).
     * [CONNECTS TO]   btd/core/GameMap.java (pointAt()).
     * [DEFENSE NOTE]  distance += speed x dt, so speed is in pixels per second (Basic = 55 px/s).
     *                 Poison and burn still hurt while stunned. When a stun ends, 1.5 s of stun immunity starts.
     *                 The angle comes from looking 1 px ahead on the path with Math.atan2, so the sprite turns at corners.
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
     * [PURPOSE]       Lowers health by the damage, after letting the subclass reduce it (armor).
     * [OOP / PATTERN] Template Method (fixed step here, reduceDamage() is the part subclasses can change).
     * [CALLED BY]     All 4 tower classes' attack methods, and update() for poison/burn ticks.
     * [CONNECTS TO]   btd/model/enemy/Enemy.java (reduceDamage()).
     * [DEFENSE NOTE]  Health is private, so the ONLY way to hurt an enemy is this method. Armor can never be skipped.
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
     * [CALLED BY]     btd/core/GameEngine.java (update()), the towers' targeting (so they skip dead enemies), canBeStunned().
     * [CONNECTS TO]   GameEngine removes dead enemies and gives their reward coins.
     * [DEFENSE NOTE]  An enemy can die mid-step (e.g. from a splash) and still be in the list until GameEngine removes it,
     *                 so towers check isDead() to avoid shooting a dead one.
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
     * [DEFENSE NOTE]  Not stunnable if dead, already stunned, or within 1.5 s after a stun ended.
     *                 This stops several Policemen from freezing an enemy forever.
     */
    public boolean canBeStunned() {
        return !isDead() && stunLeft <= 0 && stunImmuneLeft <= 0;
    }

    /**
     * [PURPOSE]       Applies stun duration if the enemy is not currently immune.
     * [OOP / PATTERN] State Modifier with Guard Clause.
     * [CALLED BY]     btd/model/tower/SupportTower.java (stunAll()).
     * [CONNECTS TO]   btd/model/enemy/BossEnemy.java (overrides to halve stun duration).
     * [DEFENSE NOTE]  Returns true if the stun landed, false if the enemy could not be stunned.
     *                 (stunAll() checks canBeStunned() first and does not use this return value.)
     */
    public boolean stun(double seconds) {
        if (!canBeStunned()) {
            return false;
        }
        stunLeft = seconds;
        return true;
    }

    /**
     * [PURPOSE]       Poisons the enemy (damage over time, "DoT"). Poison does NOT stack.
     * [OOP / PATTERN] Status Effect (keeps the strongest).
     * [CALLED BY]     btd/model/tower/SplashTower.java (applyEffect(), Scientist).
     * [CONNECTS TO]   poisonDps, poisonLeft.
     * [DEFENSE NOTE]  Math.max keeps the stronger damage and the longer time, so a weak hit never weakens a strong poison.
     */
    public void poison(double dps, double seconds) {
        poisonDps = Math.max(poisonDps, dps);
        poisonLeft = Math.max(poisonLeft, seconds);
    }

    /**
     * [PURPOSE]       Burns the enemy. Unlike poison, burn STACKS: each hit adds 1 stack, up to 5.
     * [OOP / PATTERN] Stacking Debuff.
     * [CALLED BY]     btd/model/tower/SplashTower.java (applyEffect(), Flamethrower).
     * [CONNECTS TO]   burnStacks, burnDpsPerStack, burnLeft.
     * [DEFENSE NOTE]  Burn damage per second = dpsPerStack x stacks (5 stacks = 5x). When the timer runs out,
     *                 all stacks are cleared at once (see update()).
     */
    public void burn(double dpsPerStack, double seconds) {
        burnDpsPerStack = dpsPerStack;
        burnStacks = Math.min(MAX_BURN_STACKS, burnStacks + 1);
        burnLeft = Math.max(burnLeft, seconds);
    }

    /**
     * [PURPOSE]       True when the enemy has walked the whole path (reached the BASE).
     * [OOP / PATTERN] Boundary Check / Predicate.
     * [CALLED BY]     btd/core/GameEngine.java (update()).
     * [CONNECTS TO]   btd/core/GameMap.java (getTotalLength()).
     * [DEFENSE NOTE]  When true, GameEngine removes the enemy and calls GameSession.loseLives(getLeakDamage()).
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
    /** [PURPOSE] Lives lost if this enemy reaches the base (Basic/Fast 1, Armored 2, Boss 10). */
    public int getLeakDamage() { return leakDamage; }
    /** [PURPOSE] Query whether enemy is currently stunned. */
    public boolean isStunned() { return stunLeft > 0; }
    /** [PURPOSE] Query whether enemy is suffering from poison. */
    public boolean isPoisoned() { return poisonLeft > 0; }
    /** [PURPOSE] Query whether enemy is suffering from burn stacks. */
    public boolean isBurning() { return burnLeft > 0; }
}
