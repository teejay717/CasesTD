package btd.model.tower;

import btd.core.VisualEffect;
import btd.model.enemy.Enemy;

import java.util.List;

/**
 * A tower placed on the map. Each subclass decides how it attacks. Level bonuses and the
 * Level 5 passive are handled here so every tower gets them.
 */
public abstract class Tower implements Upgradable {

    public static final int MAX_NORMAL_LEVEL = 4;
    public static final int MAX_LEVEL = 5;

    /**
     * Damage and fire rate multiplier at each level (index 0 = Lv 1). Each level is worth a bit
     * more than placing the copies it costs, so upgrading is never worse than placing duplicates.
     */
    private static final double[] LEVEL_MULTIPLIERS = {1.00, 1.45, 1.80, 2.03, 2.30};

    private final TowerType type;
    private final double x;
    private final double y;

    private int level = 1;
    private double cooldown;
    private double fireRateBuff = 1.0;

    // Set by applyMaxPassive() at Level 5
    protected double passiveDamage = 1.0;
    protected double passiveRate = 1.0;
    protected double passiveSplash = 1.0;
    protected double passiveEffect = 1.0;

    protected Tower(TowerType type, double x, double y) {
        this.type = type;
        this.x = x;
        this.y = y;
    }

    /** Called every frame: counts down the cooldown and attacks when ready. */
    public final void update(double dt, List<Enemy> enemies, List<VisualEffect> effects) {
        cooldown -= dt;
        if (cooldown > 0) {
            return;
        }
        if (attack(enemies, effects)) {
            cooldown = 1.0 / getFireRate();
        } else {
            cooldown = 0; // nothing to shoot, try again next frame
        }
    }

    /** Attacks if there is a target. Returns true if it fired. */
    protected abstract boolean attack(List<Enemy> enemies, List<VisualEffect> effects);

    /** Level 5 bonus, different for each kind of tower. */
    protected abstract void applyMaxPassive();

    /** A one-line description of the Level 5 bonus, for the info panel. */
    public abstract String getPassiveDescription();

    /** Towers that boost others (like the Radio Tower) override this. */
    public void applyAura(List<Tower> towers) {
    }

    @Override
    public int getLevel() {
        return level;
    }

    @Override
    public void applyLevel(int newLevel) {
        level = newLevel;
        if (level == MAX_LEVEL) {
            applyMaxPassive();
        }
    }

    // ---- stats ----

    protected double levelMultiplier() {
        return LEVEL_MULTIPLIERS[level - 1];
    }

    public double getDamage() {
        return type.baseDamage() * levelMultiplier() * passiveDamage;
    }

    public double getFireRate() {
        return type.fireRate() * levelMultiplier() * passiveRate * fireRateBuff;
    }

    public double getRange() {
        return type.range();
    }

    /** The fire rate boost from a nearby Radio Tower (1.0 = none). */
    protected double getFireRateBuff() {
        return fireRateBuff;
    }

    public void resetBuff() {
        fireRateBuff = 1.0;
    }

    public void addBuff(double multiplier) {
        fireRateBuff = Math.max(fireRateBuff, multiplier);
    }

    // ---- targeting helpers ----

    protected boolean inRange(Enemy e) {
        return Math.hypot(e.getX() - x, e.getY() - y) <= getRange();
    }

    /** The enemy in range that is furthest along the track. */
    protected Enemy furthestInRange(List<Enemy> enemies) {
        Enemy best = null;
        for (Enemy e : enemies) {
            if (!e.isDead() && inRange(e) && (best == null || e.getDistance() > best.getDistance())) {
                best = e;
            }
        }
        return best;
    }

    public TowerType getType() { return type; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getPassiveFireRate() { return passiveRate; }
}
