package btd.model.tower;

import btd.core.VisualEffect;
import btd.model.enemy.Enemy;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Helps the defense instead of just dealing damage. What it does depends on its special:
 * STUN freezes enemies, BUFF speeds up nearby towers, TURRETS shoots several enemies at once.
 */
public class SupportTower extends Tower {

    /** Radio Tower buff grows +10% per level (kept as before; the big level table is for damage towers). */
    private static final double AURA_LEVEL_BONUS = 0.10;

    /**
     * Policeman stun time per level, as a multiple of the base stun (effect_duration_ms).
     * With a 1000 ms base: Lv1 1.0 s, Lv2 1.17 s, Lv3 1.33 s, Lv4 1.5 s, Lv5 2.0 s (the cap).
     */
    private static final double[] STUN_LEVEL_SCALE = {1.00, 1.17, 1.33, 1.50, 2.00};

    public SupportTower(TowerType type, double x, double y) {
        super(type, x, y);
    }

    /** The Policeman does not hit harder with levels; only its Lv 5 passive (longer stun) changes it. */
    @Override
    public double getDamage() {
        if (getType().special() == Special.STUN) {
            return getType().baseDamage() * passiveDamage;
        }
        return super.getDamage();
    }

    /** The Policeman does not fire faster with levels, so stuns stay readable and can't be spammed. */
    @Override
    public double getFireRate() {
        if (getType().special() == Special.STUN) {
            return getType().fireRate() * passiveRate * getFireRateBuff();
        }
        return super.getFireRate();
    }

    @Override
    protected boolean attack(List<Enemy> enemies, List<VisualEffect> effects) {
        return switch (getType().special()) {
            case STUN -> stunAll(enemies, effects);
            case TURRETS -> fireTurrets(enemies, effects);
            default -> false; // BUFF works through applyAura
        };
    }

    /**
     * Stuns and hits every enemy in range that can be stunned right now. Enemies that are
     * already stunned or still immune (see Enemy.STUN_IMMUNITY_SECONDS) are skipped, and the
     * tower only fires when at least one stun would land, so it never wastes its cooldown.
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

    /** Each mini-turret shoots a different enemy, closest to the exit first. */
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

    /** How many mini-turrets this tower has built. */
    public int getTurretCount() {
        if (getType().special() != Special.TURRETS) {
            return 0;
        }
        return (int) Math.round(getType().effectValue() * passiveEffect);
    }

    /** Where mini-turret number i stands, around the tower. */
    public double[] turretPosition(int i, int total) {
        double angle = 2 * Math.PI * i / Math.max(1, total) - Math.PI / 2;
        return new double[]{getX() + Math.cos(angle) * 22, getY() + Math.sin(angle) * 22};
    }

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

    @Override
    protected void applyMaxPassive() {
        passiveEffect = 1.5;
    }

    @Override
    public String getPassiveDescription() {
        if (getType().special() == Special.STUN) {
            double lv5 = getType().effectDurationMs() / 1000.0 * STUN_LEVEL_SCALE[MAX_LEVEL - 1];
            return String.format("Lv 5 Upgraded Gear: stun lasts %.1f s.", lv5);
        }
        return "Lv 5 Upgraded Gear: special effect 50% stronger.";
    }
}
