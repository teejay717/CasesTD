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

    public SupportTower(TowerType type, double x, double y) {
        super(type, x, y);
    }

    @Override
    protected boolean attack(List<Enemy> enemies, List<VisualEffect> effects) {
        return switch (getType().special()) {
            case STUN -> stunAll(enemies, effects);
            case TURRETS -> fireTurrets(enemies, effects);
            default -> false; // BUFF works through applyAura
        };
    }

    private boolean stunAll(List<Enemy> enemies, List<VisualEffect> effects) {
        boolean any = false;
        for (Enemy e : enemies) {
            if (!e.isDead() && inRange(e)) {
                e.stun(getType().effectDurationMs() / 1000.0 * passiveEffect);
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
        double multiplier = 1.0 + getType().effectValue() / 100.0 * levelMultiplier() * passiveEffect;
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
        return "Lv 5 Upgraded Gear: special effect 50% stronger.";
    }
}
