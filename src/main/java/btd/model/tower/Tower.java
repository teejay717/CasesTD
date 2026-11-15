package btd.model.tower;

import btd.core.VisualEffect;
import btd.model.enemy.Enemy;

import java.util.List;

/**
 * [PURPOSE]       Abstract parent of every PLACED tower. Holds what all towers share: position, level,
 *                 cooldown, level multipliers, aura buff, and "find the enemy closest to the base".
 * [OOP / PATTERN] Abstract Class / Template Method (update() is final and calls the abstract attack()) /
 *                 Inheritance & Polymorphism (4 subclasses: SingleTarget, Splash, Sniper, Support).
 * [CALLED BY]     Created by btd/factory/TowerFactory.java; stored in btd/core/GameSession.java;
 *                 updated by btd/core/GameEngine.java; drawn by btd/ui/screens/GamePanel.java.
 * [CONNECTS TO]   btd/model/tower/Upgradable.java, btd/model/tower/TowerType.java,
 *                 btd/model/enemy/Enemy.java, btd/core/VisualEffect.java.
 * [DEFENSE NOTE]  Cannot write "new Tower(...)" because it is abstract. GameEngine only knows "Tower",
 *                 so calling t.update() runs the right subclass's attack() (polymorphism).
 */
public abstract class Tower implements Upgradable {

    /**
     * Highest level for a tower when ANOTHER tower of the same type is already Level 5
     * (only one Lv 5 per type is allowed; see GameSession.upgradeBlockReason()).
     */
    public static final int MAX_NORMAL_LEVEL = 4;
    /** Absolute maximum level. Reaching it unlocks the subclass's Lv 5 passive (applyMaxPassive()). */
    public static final int MAX_LEVEL = 5;

    /**
     * Multiplier at each level (index 0 = Lv 1). It is applied to BOTH damage and fire rate,
     * so damage per second grows by the multiplier squared: Lv 2 = 1.45 x 1.45 = about 2.1x a Lv 1 tower.
     * Upgrades cost spare copies, so this makes an upgrade roughly as strong as placing those copies.
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
     * [CALLED BY]     Subclass constructors through super(type, x, y).
     * [CONNECTS TO]   btd/model/tower/TowerType.java.
     * [DEFENSE NOTE]  protected = only subclasses can call it. type, x and y are final: a tower never moves.
     */
    protected Tower(TowerType type, double x, double y) {
        this.type = type;
        this.x = x;
        this.y = y;
    }

    /**
     * [PURPOSE]       Called every step: count the cooldown down; when it hits 0, try to attack.
     * [OOP / PATTERN] Template Method (the fixed steps are here, the attack() step is filled in by each subclass).
     * [CALLED BY]     btd/core/GameEngine.java (update()).
     * [CONNECTS TO]   btd/model/enemy/Enemy.java, btd/core/VisualEffect.java.
     * [DEFENSE NOTE]  'final' = subclasses cannot change this loop. If attack() hit something, wait 1 / fireRate seconds
     *                 (fire rate 2.0 -> 0.5 s). If nothing was in range, cooldown = 0 so it fires as soon as an enemy comes.
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
     * [DEFENSE NOTE]  SingleTarget: +50% fire rate. Splash: +50% blast radius. Sniper: 2x damage (+50% pierce for Railgunner).
     *                 Support: +50% effect (Radio Tower buff, Engineer turrets); Policeman instead gets a 2x stun time.
     */
    protected abstract void applyMaxPassive();

    /**
     * [PURPOSE]       Provides readable description of Level 5 passive for side panel UI.
     * [OOP / PATTERN] Abstract Method / Polymorphism.
     * [CALLED BY]     btd/ui/screens/SidePanel.java (paintTowerInfo()).
     * [CONNECTS TO]   The gold/brown passive text in the tower info card.
     * [DEFENSE NOTE]  Ensures UI can dynamically display perk details without instanceof checks.
     */
    public abstract String getPassiveDescription();

    /**
     * [PURPOSE]       Lets a tower buff other towers. Does nothing by default.
     * [OOP / PATTERN] Hook Method (empty default; a subclass overrides it only if it needs it).
     * [CALLED BY]     btd/core/GameEngine.java (update(), for every tower, before any tower shoots).
     * [CONNECTS TO]   btd/model/tower/SupportTower.java (overrides it; only the Radio Tower / BUFF actually buffs).
     * [DEFENSE NOTE]  Because it is not abstract, SingleTarget/Splash/Sniper do not have to write an empty method.
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
     * [CALLED BY]     btd/core/GameSession.java (upgrade()).
     * [CONNECTS TO]   btd/model/tower/Tower.java (applyMaxPassive()).
     * [DEFENSE NOTE]  The same Tower object is leveled up; it is not removed and rebuilt.
     *                 Damage and fire rate change automatically because getDamage() reads 'level' every time.
     */
    @Override
    public void applyLevel(int newLevel) {
        level = newLevel;
        if (level == MAX_LEVEL) {
            applyMaxPassive();
        }
    }

    // ---- stats ----

    /** [PURPOSE] Multiplier for the current level: LEVEL_MULTIPLIERS[level - 1] (arrays start at 0). */
    protected double levelMultiplier() {
        return LEVEL_MULTIPLIERS[level - 1];
    }

    /** [PURPOSE] Damage per hit = base damage x level multiplier x Lv 5 passive (1.0 unless Sniper Lv 5). */
    public double getDamage() {
        return type.baseDamage() * levelMultiplier() * passiveDamage;
    }

    /** [PURPOSE] Shots per second = base rate x level multiplier x Lv 5 passive x Radio Tower buff. */
    public double getFireRate() {
        return type.fireRate() * levelMultiplier() * passiveRate * fireRateBuff;
    }

    /** [PURPOSE] Accesses tower attack range radius in pixels from metadata record. */
    public double getRange() {
        return type.range();
    }

    /** [PURPOSE] Current Radio Tower buff (1.0 = none). protected: used by SupportTower.getFireRate(). */
    protected double getFireRateBuff() {
        return fireRateBuff;
    }

    /** [PURPOSE] Sets the buff back to 1.0 (no buff). GameEngine.update() calls this on every tower each step, before auras. */
    public void resetBuff() {
        fireRateBuff = 1.0;
    }

    /** [PURPOSE] Called by SupportTower.applyAura(). Keeps the BIGGEST buff, so two Radio Towers do not stack. */
    public void addBuff(double multiplier) {
        fireRateBuff = Math.max(fireRateBuff, multiplier);
    }

    // ---- targeting helpers ----

    /** [PURPOSE] Euclidean distance check verifying if enemy center falls within range radius. */
    protected boolean inRange(Enemy e) {
        return Math.hypot(e.getX() - x, e.getY() - y) <= getRange();
    }

    /**
     * [PURPOSE]       Finds the living enemy in range that has walked the furthest (closest to the base).
     * [OOP / PATTERN] "First" Targeting (shared helper in the parent class, so subclasses reuse it).
     * [CALLED BY]     btd/model/tower/SingleTargetTower.java (attack()), btd/model/tower/SplashTower.java (attack()).
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
    /** [PURPOSE] Lv 5 fire rate passive (1.5 for SingleTargetTower at Lv 5, otherwise 1.0). Not used by other classes yet. */
    public double getPassiveFireRate() { return passiveRate; }
}
