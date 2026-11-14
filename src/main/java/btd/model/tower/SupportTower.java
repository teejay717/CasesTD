package btd.model.tower;

import btd.core.VisualEffect;
import btd.model.enemy.Enemy;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * [PURPOSE]       Utility defense tower providing crowd-control stuns, aura buffs, or autonomous mini-turrets.
 * [OOP / PATTERN] Concrete Subclass / Specialization / Polymorphism.
 * [CALLED BY]     btd/factory/TowerFactory.java (create()).
 * [CONNECTS TO]   btd/model/tower/Tower.java, btd/model/enemy/Enemy.java,
 *                 btd/core/VisualEffect.java.
 * [DEFENSE NOTE]  Policeman stuns all in range; Radio Tower buffs nearby tower fire rate; Turret Builder spawns mini-turrets.
 */
public class SupportTower extends Tower {

    /** Radio Tower buff grows +10% per level (kept as before; the big level table is for damage towers). */
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
     * [PURPOSE]       Returns damage; Policeman stun tower does not scale base hit damage with levels.
     * [OOP / PATTERN] Method Overriding (Conditional stat scaling).
     * [CALLED BY]     btd/model/tower/Tower.java (update()).
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
     * [PURPOSE]       Returns fire rate; Policeman stun tower fire rate is fixed to prevent stun-lock spam.
     * [OOP / PATTERN] Method Overriding (Conditional stat scaling).
     * [CALLED BY]     btd/model/tower/Tower.java (update()).
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
     * [PURPOSE]       Executes active attack phase: triggers stun pulse or mini-turret volley based on Special.
     * [OOP / PATTERN] Method Overriding (Template Method attack hook) / Switch Dispatch.
     * [CALLED BY]     btd/model/tower/Tower.java (update()).
     * [CONNECTS TO]   stunAll(), fireTurrets().
     * [DEFENSE NOTE]  Aura towers (BUFF) return false here because they operate passively during applyAura().
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
     * [PURPOSE]       Stuns all eligible enemies in range; skips immune/stunned targets to preserve cooldown.
     * [OOP / PATTERN] Crowd Control Logic / State Guard Checking.
     * [CALLED BY]     btd/model/tower/SupportTower.java (attack()).
     * [CONNECTS TO]   btd/model/enemy/Enemy.java (canBeStunned(), stun()), btd/core/VisualEffect.java.
     * [DEFENSE NOTE]  Only resets cooldown if at least one enemy was successfully stunned (any == true).
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
     * [PURPOSE]       Fires autonomous mini-turrets at distinct targets closest to exit.
     * [OOP / PATTERN] Multi-target targeting strategy.
     * [CALLED BY]     btd/model/tower/SupportTower.java (attack()).
     * [CONNECTS TO]   btd/model/enemy/Enemy.java, btd/core/VisualEffect.java.
     * [DEFENSE NOTE]  Sorts targets by distance along track so each turret hits a separate threat.
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
     * [PURPOSE]       Calculates active number of mini-turrets for TURRETS special.
     * [OOP / PATTERN] Derived Property Calculation.
     * [CALLED BY]     btd/model/tower/SupportTower.java (fireTurrets()), btd/ui/components/TowerArt.java.
     * [CONNECTS TO]   effectValue and passiveEffect.
     */
    public int getTurretCount() {
        if (getType().special() != Special.TURRETS) {
            return 0;
        }
        return (int) Math.round(getType().effectValue() * passiveEffect);
    }

    /**
     * [PURPOSE]       Calculates orbital position coordinates for mini-turret i around main tower base.
     * [OOP / PATTERN] Trigonometric Orbital Math.
     * [CALLED BY]     btd/model/tower/SupportTower.java (fireTurrets()), btd/ui/components/TowerArt.java.
     * [CONNECTS TO]   Rendering and visual beam origins.
     */
    public double[] turretPosition(int i, int total) {
        double angle = 2 * Math.PI * i / Math.max(1, total) - Math.PI / 2;
        return new double[]{getX() + Math.cos(angle) * 22, getY() + Math.sin(angle) * 22};
    }

    /**
     * [PURPOSE]       Applies fire rate boost aura to all neighboring towers within radius.
     * [OOP / PATTERN] Method Overriding (Aura Buff Pattern).
     * [CALLED BY]     btd/core/GameEngine.java (updateTowers()).
     * [CONNECTS TO]   btd/model/tower/Tower.java (addBuff()).
     * [DEFENSE NOTE]  Aura scales +10% per level; Lv5 passive boosts buff strength by an additional 50%.
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
     * [PURPOSE]       Activates Level 5 Upgraded Gear passive: amplifies special effect strength by 50%.
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
     * [CALLED BY]     btd/ui/screens/SidePanel.java.
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
