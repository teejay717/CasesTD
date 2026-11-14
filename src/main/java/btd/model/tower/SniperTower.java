package btd.model.tower;

import btd.core.VisualEffect;
import btd.model.enemy.Enemy;

import java.awt.Color;
import java.util.List;

/**
 * [PURPOSE]       Extreme range sniper tower targeting highest-health enemies, with piercing beam capability.
 * [OOP / PATTERN] Concrete Subclass / Specialization / Polymorphism.
 * [CALLED BY]     btd/factory/TowerFactory.java (create()).
 * [CONNECTS TO]   btd/model/tower/Tower.java, btd/model/enemy/Enemy.java,
 *                 btd/core/VisualEffect.java.
 * [DEFENSE NOTE]  Targets strongest enemy (highest HP) to crack armored enemies and bosses; Lv5 doubles damage.
 */
public class SniperTower extends Tower {

    private static final double BEAM_WIDTH = 16;

    /**
     * [PURPOSE]       Constructs SniperTower instance at grid coordinates.
     * [OOP / PATTERN] Superclass Constructor Invocation.
     * [CALLED BY]     btd/factory/TowerFactory.java.
     * [CONNECTS TO]   btd/model/tower/Tower.java.
     */
    public SniperTower(TowerType type, double x, double y) {
        super(type, x, y);
    }

    /**
     * [PURPOSE]       Finds highest-health living enemy in range and fires single heavy shot or piercing laser.
     * [OOP / PATTERN] Method Overriding (Template Method attack hook) / Strongest-Targeting Strategy.
     * [CALLED BY]     btd/model/tower/Tower.java (update()).
     * [CONNECTS TO]   btd/model/enemy/Enemy.java (takeDamage()), btd/core/VisualEffect.java (line()).
     * [DEFENSE NOTE]  Dispatches to pierce() if Special is PIERCE; otherwise hits target directly.
     */
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

    /**
     * [PURPOSE]       Calculates collinear laser beam and pierces multiple enemies along the line of fire.
     * [OOP / PATTERN] Vector Geometry / Stream Filtering & Sorting.
     * [CALLED BY]     btd/model/tower/SniperTower.java (attack()).
     * [CONNECTS TO]   btd/model/enemy/Enemy.java, btd/core/VisualEffect.java.
     * [DEFENSE NOTE]  Hits up to effectValue enemies in order of distance along ray using 2D projection math.
     */
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

    /** [PURPOSE] Computes scalar dot product projection along beam unit vector. */
    private double distanceAlong(Enemy e, double ux, double uy) {
        return (e.getX() - getX()) * ux + (e.getY() - getY()) * uy;
    }

    /** [PURPOSE] Calculates perpendicular distance from enemy center to beam line, checking collision within BEAM_WIDTH. */
    private boolean onBeam(Enemy e, double ux, double uy) {
        double along = distanceAlong(e, ux, uy);
        if (along < 0 || along > getRange()) {
            return false;
        }
        double sideways = Math.abs((e.getX() - getX()) * uy - (e.getY() - getY()) * ux);
        return sideways <= BEAM_WIDTH + e.getRadius();
    }

    /**
     * [PURPOSE]       Activates Level 5 Headshot passive: 2.0x damage multiplier and 1.5x pierce limit.
     * [OOP / PATTERN] Hook Method Overriding.
     * [CALLED BY]     btd/model/tower/Tower.java (applyLevel()).
     * [CONNECTS TO]   passiveDamage, passiveEffect fields.
     */
    @Override
    protected void applyMaxPassive() {
        passiveDamage = 2.0;
        passiveEffect = 1.5;
    }

    /**
     * [PURPOSE]       Returns description of Level 5 Headshot bonus.
     * [OOP / PATTERN] Polymorphic Property Accessor.
     * [CALLED BY]     btd/ui/screens/SidePanel.java.
     */
    @Override
    public String getPassiveDescription() {
        if (getType().special() == Special.PIERCE) {
            return "Lv 5 Headshot: double damage and 50% more pierce.";
        }
        return "Lv 5 Headshot: double damage.";
    }
}
