package btd.model.tower;

import btd.core.VisualEffect;
import btd.model.enemy.Enemy;

import java.awt.Color;
import java.util.List;

/**
 * [PURPOSE]       Area damage tower: hits every enemy near the target. Some also poison or burn.
 *                 (Grenadier, Mortar = plain blast; Scientist = POISON; Flamethrower = BURN)
 * [OOP / PATTERN] Concrete Subclass (extends Tower) / Method Overriding / Polymorphism.
 * [CALLED BY]     btd/factory/TowerFactory.java (create(), when tower_class = "SplashTower").
 * [CONNECTS TO]   btd/model/tower/Tower.java, btd/model/enemy/Enemy.java,
 *                 btd/core/VisualEffect.java.
 * [DEFENSE NOTE]  The blast is centered on the lead enemy, with radius = splash_radius from the DB.
 *                 Lv 5 "Wide Blast": radius x 1.5.
 */
public class SplashTower extends Tower {

    /**
     * [PURPOSE]       Constructs SplashTower instance at grid coordinates.
     * [OOP / PATTERN] Superclass Constructor Invocation.
     * [CALLED BY]     btd/factory/TowerFactory.java.
     * [CONNECTS TO]   btd/model/tower/Tower.java.
     */
    public SplashTower(TowerType type, double x, double y) {
        super(type, x, y);
    }

    /**
     * [PURPOSE]       Picks the lead enemy, then damages EVERY living enemy within the radius of it, and adds a circle effect.
     * [OOP / PATTERN] Method Overriding (Template Method attack hook).
     * [CALLED BY]     btd/model/tower/Tower.java (update()).
     * [CONNECTS TO]   btd/model/enemy/Enemy.java (takeDamage(), poison(), burn()),
     *                 btd/core/VisualEffect.java (circle()).
     * [DEFENSE NOTE]  Splash radius is dynamically scaled by passiveSplash multiplier.
     */
    @Override
    protected boolean attack(List<Enemy> enemies, List<VisualEffect> effects) {
        Enemy target = furthestInRange(enemies);
        if (target == null) {
            return false;
        }
        double radius = getType().splashRadius() * passiveSplash;
        for (Enemy e : enemies) {
            if (!e.isDead() && Math.hypot(e.getX() - target.getX(), e.getY() - target.getY()) <= radius) {
                e.takeDamage(getDamage());
                applyEffect(e);
            }
        }
        effects.add(VisualEffect.circle(target.getX(), target.getY(), radius, blastColor(), 0.25));
        return true;
    }

    /**
     * [PURPOSE]       Applies POISON or BURN status effects to caught enemies based on Special enum.
     * [OOP / PATTERN] Switch on the Special enum.
     * [CALLED BY]     btd/model/tower/SplashTower.java (attack()).
     * [CONNECTS TO]   btd/model/enemy/Enemy.java (poison(), burn()).
     * [DEFENSE NOTE]  effect_value = damage per second, effect_duration_ms = how long. Both come from the DB.
     *                 The damage per second is multiplied by the level multiplier. NONE does nothing.
     */
    private void applyEffect(Enemy e) {
        double seconds = getType().effectDurationMs() / 1000.0;
        switch (getType().special()) {
            case POISON -> e.poison(getType().effectValue() * levelMultiplier(), seconds);
            case BURN -> e.burn(getType().effectValue() * levelMultiplier(), seconds);
            default -> { }
        }
    }

    /**
     * [PURPOSE]       Selects visual explosion color corresponding to active special effect.
     * [OOP / PATTERN] Visual Theme Mapping.
     * [CALLED BY]     btd/model/tower/SplashTower.java (attack()).
     * [CONNECTS TO]   VisualEffect rendering.
     * [DEFENSE NOTE]  Poison renders purple, Burn renders orange, standard blast renders gold.
     */
    private Color blastColor() {
        return switch (getType().special()) {
            case POISON -> new Color(160, 80, 220);
            case BURN -> new Color(255, 120, 30);
            default -> new Color(255, 190, 60);
        };
    }

    /**
     * [PURPOSE]       Activates Level 5 Wide Blast passive: expands splash radius by 1.5x.
     * [OOP / PATTERN] Hook Method Overriding.
     * [CALLED BY]     btd/model/tower/Tower.java (applyLevel()).
     * [CONNECTS TO]   passiveSplash field.
     */
    @Override
    protected void applyMaxPassive() {
        passiveSplash = 1.5;
    }

    /**
     * [PURPOSE]       Returns description of Level 5 Wide Blast bonus.
     * [OOP / PATTERN] Polymorphic Property Accessor.
     * [CALLED BY]     btd/ui/screens/SidePanel.java (paintTowerInfo()).
     */
    @Override
    public String getPassiveDescription() {
        return "Lv 5 Wide Blast: 50% bigger splash.";
    }
}
