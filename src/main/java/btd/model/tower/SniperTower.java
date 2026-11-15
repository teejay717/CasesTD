package btd.model.tower;

import btd.core.VisualEffect;
import btd.model.enemy.Enemy;

import java.awt.Color;
import java.util.List;

/**
 * [PURPOSE]       Long-range tower that shoots the enemy with the MOST health. (Sniper; Railgunner = PIERCE)
 * [OOP / PATTERN] Concrete Subclass (extends Tower) / Method Overriding / Polymorphism.
 * [CALLED BY]     btd/factory/TowerFactory.java (create(), when tower_class = "SniperTower").
 * [CONNECTS TO]   btd/model/tower/Tower.java, btd/model/enemy/Enemy.java,
 *                 btd/core/VisualEffect.java.
 * [DEFENSE NOTE]  Different target rule from the others: highest health, not closest to the base,
 *                 so it goes for Armored zombies and bosses. Lv 5 "Headshot": 2x damage.
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
     * [PURPOSE]       Finds the living enemy in range with the most health, then either shoots it or fires a piercing beam.
     * [OOP / PATTERN] Method Overriding (Template Method attack hook) / "Strongest" targeting.
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
     * [PURPOSE]       Railgunner beam: a straight line from the tower through the target, out to full range.
     *                 Damages up to effect_value enemies on that line (5; 7.5 -> 8 at Lv 5), nearest first.
     * [OOP / PATTERN] Vector Math (unit vector ux, uy) / Java Streams (filter -> sorted -> limit -> toList).
     * [CALLED BY]     btd/model/tower/SniperTower.java (attack()).
     * [CONNECTS TO]   btd/model/enemy/Enemy.java, btd/core/VisualEffect.java.
     * [DEFENSE NOTE]  The main target is always hit, even if it was not in the first maxHits on the beam.
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

    /** [PURPOSE] How far forward along the beam an enemy is (dot product with the beam direction). Negative = behind the tower. */
    private double distanceAlong(Enemy e, double ux, double uy) {
        return (e.getX() - getX()) * ux + (e.getY() - getY()) * uy;
    }

    /**
     * [PURPOSE] Is the enemy on the beam? It must be in front of the tower, within range, and at most
     *           BEAM_WIDTH (16 px) + its own radius away from the line sideways (cross product).
     */
    private boolean onBeam(Enemy e, double ux, double uy) {
        double along = distanceAlong(e, ux, uy);
        if (along < 0 || along > getRange()) {
            return false;
        }
        double sideways = Math.abs((e.getX() - getX()) * uy - (e.getY() - getY()) * ux);
        return sideways <= BEAM_WIDTH + e.getRadius();
    }

    /**
     * [PURPOSE]       Lv 5 "Headshot": 2x damage; pierce count x 1.5 (only matters for the Railgunner).
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
     * [CALLED BY]     btd/ui/screens/SidePanel.java (paintTowerInfo()).
     */
    @Override
    public String getPassiveDescription() {
        if (getType().special() == Special.PIERCE) {
            return "Lv 5 Headshot: double damage and 50% more pierce.";
        }
        return "Lv 5 Headshot: double damage.";
    }
}
