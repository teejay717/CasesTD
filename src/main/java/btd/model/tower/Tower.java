package btd.model.tower;

import btd.core.VisualEffect;
import btd.model.enemy.Enemy;

import java.util.List;

/**
 * [PURPOSE]       Abstract base class for all defense towers placed on the grid.
 *                 Handles cooldowns, targeting math, level multipliers, and Lv5 passive hooks.
 * [OOP / PATTERN] Abstract Class / Template Method Pattern / Inheritance & Polymorphism.
 * [CALLED BY]     btd/factory/TowerFactory.java, btd/core/GameEngine.java, btd/core/GameSession.java.
 * [CONNECTS TO]   btd/model/tower/Upgradable.java, btd/model/tower/TowerType.java,
 *                 btd/model/enemy/Enemy.java, btd/core/VisualEffect.java.
 * [DEFENSE NOTE]  Encapsulates generic tower loop logic while leaving attack mechanics,
 *                 auras, and max passives to specialized subclasses.
 */
public abstract class Tower implements Upgradable {

    /** Maximum level reachable via standard gold upgrades. */
    public static final int MAX_NORMAL_LEVEL = 4;
    /** Absolute maximum level unlocking the signature archetype passive. */
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

    /**
     * [PURPOSE]       Constructs a new placed tower instance on the grid.
     * [OOP / PATTERN] Constructor / State Initialization.
     * [CALLED BY]     Subclass constructors (SingleTargetTower, SplashTower, SniperTower, SupportTower).
     * [CONNECTS TO]   btd/model/tower/TowerType.java.
     * [DEFENSE NOTE]  Position coordinates and database metadata record are immutable.
     */
    protected Tower(TowerType type, double x, double y) {
        this.type = type;
        this.x = x;
        this.y = y;
    }

    /**
     * [PURPOSE]       Executes simulation frame tick: decrements cooldown and fires when ready.
     * [OOP / PATTERN] Template Method Pattern (invokes attack() hook).
     * [CALLED BY]     btd/core/GameEngine.java (updateTowers()).
     * [CONNECTS TO]   btd/model/enemy/Enemy.java, btd/core/VisualEffect.java.
     * [DEFENSE NOTE]  If no enemies are in range, cooldown stays at zero to fire immediately once a target appears.
     */
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

    /**
     * [PURPOSE]       Subclass-specific attack behavior (single target, splash AoE, sniper, or support).
     * [OOP / PATTERN] Abstract Method (Template Method step).
     * [CALLED BY]     btd/model/tower/Tower.java (update()).
     * [CONNECTS TO]   Subclasses, btd/model/enemy/Enemy.java, btd/core/VisualEffect.java.
     * [DEFENSE NOTE]  Returns true if shot fired, causing cooldown reset; false if idle.
     */
    protected abstract boolean attack(List<Enemy> enemies, List<VisualEffect> effects);

    /**
     * [PURPOSE]       Applies signature Level 5 passive enhancement when reaching max level.
     * [OOP / PATTERN] Hook Method (Template Method Pattern).
     * [CALLED BY]     btd/model/tower/Tower.java (applyLevel()).
     * [CONNECTS TO]   Subclass passive multipliers.
     * [DEFENSE NOTE]  SingleTarget gets +50% fire rate, Splash gets +50% radius, Sniper gets 2x damage, Support gets +50% aura/turrets.
     */
    protected abstract void applyMaxPassive();

    /**
     * [PURPOSE]       Provides readable description of Level 5 passive for side panel UI.
     * [OOP / PATTERN] Abstract Method / Polymorphism.
     * [CALLED BY]     btd/ui/screens/SidePanel.java.
     * [CONNECTS TO]   UI tooltips and upgrade prompt labels.
     * [DEFENSE NOTE]  Ensures UI can dynamically display perk details without instanceof checks.
     */
    public abstract String getPassiveDescription();

    /**
     * [PURPOSE]       Aura buff application hook for support towers.
     * [OOP / PATTERN] Hook Method (Default empty implementation).
     * [CALLED BY]     btd/core/GameEngine.java (updateTowers()).
     * [CONNECTS TO]   btd/model/tower/SupportTower.java (overrides for Radio Tower).
     * [DEFENSE NOTE]  Iterates all towers on the field before attack phase to apply aura multipliers.
     */
    public void applyAura(List<Tower> towers) {
    }

    /** [PURPOSE] Returns current upgrade level (1..5). */
    @Override
    public int getLevel() {
        return level;
    }

    /**
     * [PURPOSE]       Updates level and triggers applyMaxPassive() if level reaches 5.
     * [OOP / PATTERN] Contract Implementation / State Transition.
     * [CALLED BY]     btd/core/GameSession.java (upgradeTower()).
     * [CONNECTS TO]   btd/model/tower/Tower.java (applyMaxPassive()).
     * [DEFENSE NOTE]  Seamlessly promotes tower to final tier without rebuilding or replacing the instance.
     */
    @Override
    public void applyLevel(int newLevel) {
        level = newLevel;
        if (level == MAX_LEVEL) {
            applyMaxPassive();
        }
    }

    // ---- stats ----

    /** [PURPOSE] Resolves damage/fire rate multiplier for current level from lookup array. */
    protected double levelMultiplier() {
        return LEVEL_MULTIPLIERS[level - 1];
    }

    /** [PURPOSE] Calculates total damage including base, level multiplier, and Lv5 passive damage. */
    public double getDamage() {
        return type.baseDamage() * levelMultiplier() * passiveDamage;
    }

    /** [PURPOSE] Calculates fire rate (shots/sec) factoring level, passive, and external aura buffs. */
    public double getFireRate() {
        return type.fireRate() * levelMultiplier() * passiveRate * fireRateBuff;
    }

    /** [PURPOSE] Accesses tower attack range radius in pixels from metadata record. */
    public double getRange() {
        return type.range();
    }

    /** [PURPOSE] Accesses active aura buff multiplier applied by nearby SupportTowers. */
    protected double getFireRateBuff() {
        return fireRateBuff;
    }

    /** [PURPOSE] Resets external aura buff back to 1.0 prior to fresh aura recalculation frame. */
    public void resetBuff() {
        fireRateBuff = 1.0;
    }

    /** [PURPOSE] Sets aura buff to maximum of existing buff or newly offered multiplier. */
    public void addBuff(double multiplier) {
        fireRateBuff = Math.max(fireRateBuff, multiplier);
    }

    // ---- targeting helpers ----

    /** [PURPOSE] Euclidean distance check verifying if enemy center falls within range radius. */
    protected boolean inRange(Enemy e) {
        return Math.hypot(e.getX() - x, e.getY() - y) <= getRange();
    }

    /**
     * [PURPOSE]       Finds living enemy within range that has progressed furthest along track.
     * [OOP / PATTERN] First/Progress Targeting Strategy.
     * [CALLED BY]     btd/model/tower/SingleTargetTower.java, btd/model/tower/SplashTower.java.
     * [CONNECTS TO]   btd/model/enemy/Enemy.java (getDistance()).
     * [DEFENSE NOTE]  Standard tower defense priority rule: shoot the enemy closest to leaking.
     */
    protected Enemy furthestInRange(List<Enemy> enemies) {
        Enemy best = null;
        for (Enemy e : enemies) {
            if (!e.isDead() && inRange(e) && (best == null || e.getDistance() > best.getDistance())) {
                best = e;
            }
        }
        return best;
    }

    /** [PURPOSE] Metadata record containing static base stats. */
    public TowerType getType() { return type; }
    /** [PURPOSE] X coordinate on the grid. */
    public double getX() { return x; }
    /** [PURPOSE] Y coordinate on the grid. */
    public double getY() { return y; }
    /** [PURPOSE] Passive fire rate multiplier. */
    public double getPassiveFireRate() { return passiveRate; }
}
