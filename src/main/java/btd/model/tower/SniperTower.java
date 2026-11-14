package btd.model.tower;

import btd.core.VisualEffect;
import btd.model.enemy.Enemy;

import java.awt.Color;
import java.util.List;

/** Long range. Targets the enemy with the most health. Some snipers pierce through a line of enemies. */
public class SniperTower extends Tower {

    private static final double BEAM_WIDTH = 16;

    public SniperTower(TowerType type, double x, double y) {
        super(type, x, y);
    }

    @Override
    protected boolean attack(List<Enemy> enemies, List<VisualEffect> effects) {
        Enemy target = null;
        for (Enemy e : enemies) {
            if (!e.isDead() && inRange(e) && (target == null || e.getHealth() > target.getHealth())) {
                target = e;
            }
        }
        if (target == null) {
            return false;
        }
        if (getType().special() == Special.PIERCE) {
            pierce(target, enemies, effects);
        } else {
            target.takeDamage(getDamage());
            effects.add(VisualEffect.line(getX(), getY(), target.getX(), target.getY(), new Color(120, 220, 255), 0.18));
        }
        return true;
    }

    /** Fires a beam through the target and hits up to effectValue enemies on that line. */
    private void pierce(Enemy target, List<Enemy> enemies, List<VisualEffect> effects) {
        double dx = target.getX() - getX();
        double dy = target.getY() - getY();
        double length = Math.hypot(dx, dy);
        double ux = dx / length;
        double uy = dy / length;
        double endX = getX() + ux * getRange();
        double endY = getY() + uy * getRange();

        int maxHits = (int) Math.round(getType().effectValue() * passiveEffect);
        List<Enemy> hit = enemies.stream()
                .filter(e -> !e.isDead() && onBeam(e, ux, uy))
                .sorted((a, b) -> Double.compare(distanceAlong(a, ux, uy), distanceAlong(b, ux, uy)))
                .limit(maxHits)
                .toList();
        for (Enemy e : hit) {
            e.takeDamage(getDamage());
        }
        if (!hit.contains(target)) {
            target.takeDamage(getDamage());
        }
        effects.add(VisualEffect.line(getX(), getY(), endX, endY, new Color(255, 120, 60), 0.25));
    }

    private double distanceAlong(Enemy e, double ux, double uy) {
        return (e.getX() - getX()) * ux + (e.getY() - getY()) * uy;
    }

    private boolean onBeam(Enemy e, double ux, double uy) {
        double along = distanceAlong(e, ux, uy);
        if (along < 0 || along > getRange()) {
            return false;
        }
        double sideways = Math.abs((e.getX() - getX()) * uy - (e.getY() - getY()) * ux);
        return sideways <= BEAM_WIDTH + e.getRadius();
    }

    @Override
    protected void applyMaxPassive() {
        passiveDamage = 2.0;
        passiveEffect = 1.5;
    }

    @Override
    public String getPassiveDescription() {
        if (getType().special() == Special.PIERCE) {
            return "Lv 5 Headshot: double damage and 50% more pierce.";
        }
        return "Lv 5 Headshot: double damage.";
    }
}
