package btd.model.tower;

import btd.core.VisualEffect;
import btd.model.enemy.Enemy;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * [PURPOSE]       One class for the 3 helper towers. What it does depends on its Special:
 *                 STUN = Policeman (stuns everyone in range), BUFF = Radio Tower (other towers shoot faster),
 *                 TURRETS = Engineer (mini-turrets that each shoot a different enemy).
 * [OOP / PATTERN] Concrete Subclass (extends Tower) / Method Overriding (also overrides getDamage, getFireRate, applyAura).
 * [CALLED BY]     btd/factory/TowerFactory.java (create(), when tower_class = "SupportTower").
 * [CONNECTS TO]   btd/model/tower/Tower.java, btd/model/enemy/Enemy.java,
 *                 btd/core/VisualEffect.java.
 * [DEFENSE NOTE]  The behavior comes from DB data (special column), so a new support tower can be added in SQL
 *                 without a new Java class, as long as it uses one of these 3 specials.
 */
public class SupportTower extends Tower {

    /** Radio Tower buff strength grows 10% per level (Lv 1 = 1.0x, Lv 5 = 1.4x of the base 30%). */
    private static final double AURA_LEVEL_BONUS = 0.10;

    /**
     * Policeman stun time per level, as a multiple of the base stun (effect_duration_ms).
     * With a 1000 ms base: Lv1 1.0 s, Lv2 1.17 s, Lv3 1.33 s, Lv4 1.5 s, Lv5 2.0 s (the cap).
     */
    private static final double[] STUN_LEVEL_SCALE = {1.00, 1.17, 1.33, 1.50, 2.00};

    /**
     * [PURPOSE]       Constructs SupportTower instance at grid coordinates.
     * [OOP / PATTERN] Superclass Constructor Invocation.
     * [CALLED BY]     btd/factory/TowerFactory.java.
     * [CONNECTS TO]   btd/model/tower/Tower.java.
     */
    public SupportTower(TowerType type, double x, double y) {
        super(type, x, y);
    }

    /**
     * [PURPOSE]       Damage per hit. The Policeman's damage does NOT grow with level (its stun time grows instead).
     * [OOP / PATTERN] Method Overriding with super call (other specials use Tower's normal formula).
     * [CALLED BY]     stunAll(), fireTurrets(), btd/ui/screens/SidePanel.java (paintTowerInfo()).
     * [CONNECTS TO]   getType().baseDamage().
     */
    @Override
    public double getDamage() {
        if (getType().special() == Special.STUN) {
            return getType().baseDamage() * passiveDamage;
        }
        return super.getDamage();
    }

    /**
     * [PURPOSE]       Shots per second. The Policeman's rate does NOT grow with level, so stuns cannot be spammed.
     *                 (A Radio Tower buff still applies to it.)
     * [OOP / PATTERN] Method Overriding with super call.
     * [CALLED BY]     btd/model/tower/Tower.java (update(), for the cooldown), btd/ui/screens/SidePanel.java.
     * [CONNECTS TO]   getType().fireRate().
     */
    @Override
    public double getFireRate() {
        if (getType().special() == Special.STUN) {
            return getType().fireRate() * passiveRate * getFireRateBuff();
        }
        return super.getFireRate();
    }

    /**
     * [PURPOSE]       STUN -> stunAll(), TURRETS -> fireTurrets(), anything else (BUFF) -> does not attack.
     * [OOP / PATTERN] Method Overriding (Template Method attack hook) / switch expression.
     * [CALLED BY]     btd/model/tower/Tower.java (update()).
     * [CONNECTS TO]   stunAll(), fireTurrets().
     * [DEFENSE NOTE]  The Radio Tower returns false here. Its job is done in applyAura(), which GameEngine calls
     *                 for every tower before they attack.
     */
    @Override
    protected boolean attack(List<Enemy> enemies, List<VisualEffect> effects) {
        return switch (getType().special()) {
            case STUN -> stunAll(enemies, effects);
            case TURRETS -> fireTurrets(enemies, effects);
            default -> false; // BUFF works through applyAura
        };
    }

    /**
     * [PURPOSE]       Stuns (and lightly damages) every enemy in range that can be stunned right now.
     * [OOP / PATTERN] Crowd Control / Guard Check (canBeStunned()).
     * [CALLED BY]     btd/model/tower/SupportTower.java (attack()).
     * [CONNECTS TO]   btd/model/enemy/Enemy.java (canBeStunned(), stun()), btd/core/VisualEffect.java.
     * [DEFENSE NOTE]  Stun time = effect_duration_ms x STUN_LEVEL_SCALE (1.0 s at Lv 1 up to 2.0 s at Lv 5).
     *                 Returns true only if someone was stunned. If everyone is immune, no cooldown is used,
     *                 so the Policeman fires as soon as an enemy becomes stunnable.
     */
    private boolean stunAll(List<Enemy> enemies, List<VisualEffect> effects) {
        double seconds = getType().effectDurationMs() / 1000.0 * STUN_LEVEL_SCALE[getLevel() - 1];
        boolean any = false;
        for (Enemy e : enemies) {
            if (inRange(e) && e.canBeStunned()) {
                e.stun(seconds);
                e.takeDamage(getDamage());
                any = true;
            }
        }
        if (any) {
            effects.add(VisualEffect.circle(getX(), getY(), getRange(), new Color(90, 160, 255), 0.3));
        }
        return any;
    }

    /**
     * [PURPOSE]       Engineer: each mini-turret shoots a DIFFERENT enemy, starting from the one closest to the base.
     * [OOP / PATTERN] Multi-target targeting / Comparator (sort by distance walked, biggest first).
     * [CALLED BY]     btd/model/tower/SupportTower.java (attack()).
     * [CONNECTS TO]   btd/model/enemy/Enemy.java, btd/core/VisualEffect.java.
     * [DEFENSE NOTE]  3 turrets + 2 enemies in range -> only 2 shots. The shot line starts at the turret, not the tower.
     */
    private boolean fireTurrets(List<Enemy> enemies, List<VisualEffect> effects) {
        List<Enemy> targets = new ArrayList<>();
        for (Enemy e : enemies) {
            if (!e.isDead() && inRange(e)) {
                targets.add(e);
            }
        }
        targets.sort(Comparator.comparingDouble(Enemy::getDistance).reversed());
        int turrets = getTurretCount();
        for (int i = 0; i < turrets && i < targets.size(); i++) {
            Enemy e = targets.get(i);
            e.takeDamage(getDamage());
            double[] pos = turretPosition(i, turrets);
            effects.add(VisualEffect.line(pos[0], pos[1], e.getX(), e.getY(), new Color(255, 210, 90), 0.1));
        }
        return !targets.isEmpty();
    }

    /**
     * [PURPOSE]       Number of mini-turrets: effect_value (3), or 3 x 1.5 = 4.5 -> 5 at Lv 5. 0 if not an Engineer.
     * [OOP / PATTERN] Derived (calculated) Property.
     * [CALLED BY]     fireTurrets(), btd/ui/screens/GamePanel.java (paintTowers(), draws the turret dots).
     * [CONNECTS TO]   effectValue and passiveEffect.
     */
    public int getTurretCount() {
        if (getType().special() != Special.TURRETS) {
            return 0;
        }
        return (int) Math.round(getType().effectValue() * passiveEffect);
    }

    /**
     * [PURPOSE]       Position of turret i: spread evenly on a circle of radius 22 px around the tower, first one on top.
     * [OOP / PATTERN] Trigonometry (cos for x, sin for y).
     * [CALLED BY]     fireTurrets() (where the shot starts), btd/ui/screens/GamePanel.java (paintTowers()).
     * [CONNECTS TO]   Drawing and shot-line start points.
     */
    public double[] turretPosition(int i, int total) {
        double angle = 2 * Math.PI * i / Math.max(1, total) - Math.PI / 2;
        return new double[]{getX() + Math.cos(angle) * 22, getY() + Math.sin(angle) * 22};
    }

    /**
     * [PURPOSE]       Radio Tower only: makes every OTHER tower within range shoot faster.
     * [OOP / PATTERN] Method Overriding (overrides Tower's empty hook).
     * [CALLED BY]     btd/core/GameEngine.java (update(), for every tower, before towers attack).
     * [CONNECTS TO]   btd/model/tower/Tower.java (addBuff()).
     * [DEFENSE NOTE]  multiplier = 1 + 30% x levelBonus x passive.
     *                 Lv 1 = 1.30x, Lv 5 = 1 + 0.30 x 1.4 x 1.5 = 1.63x fire rate for nearby towers.
     */
    @Override
    public void applyAura(List<Tower> towers) {
        if (getType().special() != Special.BUFF) {
            return;
        }
        double auraLevel = 1.0 + AURA_LEVEL_BONUS * (getLevel() - 1);
        double multiplier = 1.0 + getType().effectValue() / 100.0 * auraLevel * passiveEffect;
        for (Tower t : towers) {
            if (t != this && Math.hypot(t.getX() - getX(), t.getY() - getY()) <= getRange()) {
                t.addBuff(multiplier);
            }
        }
    }

    /**
     * [PURPOSE]       Lv 5 "Upgraded Gear": passiveEffect = 1.5 (stronger buff, more turrets).
     *                 The Policeman does not use passiveEffect; its Lv 5 bonus is the 2.0 s stun in STUN_LEVEL_SCALE.
     * [OOP / PATTERN] Hook Method Overriding.
     * [CALLED BY]     btd/model/tower/Tower.java (applyLevel()).
     * [CONNECTS TO]   passiveEffect field.
     */
    @Override
    protected void applyMaxPassive() {
        passiveEffect = 1.5;
    }

    /**
     * [PURPOSE]       Returns description of Level 5 Upgraded Gear bonus for side panel.
     * [OOP / PATTERN] Polymorphic Property Accessor.
     * [CALLED BY]     btd/ui/screens/SidePanel.java (paintTowerInfo()).
     */
    @Override
    public String getPassiveDescription() {
        if (getType().special() == Special.STUN) {
            double lv5 = getType().effectDurationMs() / 1000.0 * STUN_LEVEL_SCALE[MAX_LEVEL - 1];
            return String.format("Lv 5 Upgraded Gear: stun lasts %.1f s.", lv5);
        }
        return "Lv 5 Upgraded Gear: special effect 50% stronger.";
    }
}
