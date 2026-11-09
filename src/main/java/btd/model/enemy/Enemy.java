package btd.model.enemy;

import btd.core.GameMap;

/**
 * A zombie walking along the track. Subclasses set the base stats and can change how damage
 * or status effects are handled.
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

    protected Enemy(double maxHealth, double speed, int reward, int leakDamage) {
        this.maxHealth = maxHealth;
        this.health = maxHealth;
        this.speed = speed;
        this.reward = reward;
        this.leakDamage = leakDamage;
    }

    /** Name used for the display and for the reward lookup, such as "BasicEnemy". */
    public String getTypeName() {
        return getClass().getSimpleName();
    }

    /** Radius in pixels, used for drawing and for hit checks. */
    public abstract double getRadius();

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

    @Override
    public void takeDamage(double amount) {
        health -= reduceDamage(amount);
    }

    /** Lets a subclass (such as ArmoredEnemy) soften each hit. */
    protected double reduceDamage(double amount) {
        return amount;
    }

    @Override
    public boolean isDead() {
        return health <= 0;
    }

    /** True if a stun would land now: not already stunned and not in the immunity window after one. */
    public boolean canBeStunned() {
        return !isDead() && stunLeft <= 0 && stunImmuneLeft <= 0;
    }

    /** Stuns the enemy unless it is already stunned or still immune. Returns true if the stun landed. */
    public boolean stun(double seconds) {
        if (!canBeStunned()) {
            return false;
        }
        stunLeft = seconds;
        return true;
    }

    public void poison(double dps, double seconds) {
        poisonDps = Math.max(poisonDps, dps);
        poisonLeft = Math.max(poisonLeft, seconds);
    }

    /** Burn stacks up to 5 times; each hit also refreshes how long it lasts. */
    public void burn(double dpsPerStack, double seconds) {
        burnDpsPerStack = dpsPerStack;
        burnStacks = Math.min(MAX_BURN_STACKS, burnStacks + 1);
        burnLeft = Math.max(burnLeft, seconds);
    }

    public boolean hasReachedEnd(GameMap map) {
        return distance >= map.getTotalLength();
    }

    public double getHealth() { return health; }
    public double getMaxHealth() { return maxHealth; }
    public double getDistance() { return distance; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getAngle() { return angle; }
    public int getReward() { return reward; }
    public int getLeakDamage() { return leakDamage; }
    public boolean isStunned() { return stunLeft > 0; }
    public boolean isPoisoned() { return poisonLeft > 0; }
    public boolean isBurning() { return burnLeft > 0; }
}
