package btd.model.tower;

import btd.core.VisualEffect;
import btd.model.enemy.Enemy;

import java.awt.Color;
import java.util.List;

/**
 * [PURPOSE]       Concrete tower that fires focused single-target shots at the lead enemy.
 * [OOP / PATTERN] Concrete Subclass / Specialization / Polymorphism.
 * [CALLED BY]     btd/factory/TowerFactory.java (create()).
 * [CONNECTS TO]   btd/model/tower/Tower.java, btd/model/enemy/Enemy.java,
 *                 btd/core/VisualEffect.java.
 * [DEFENSE NOTE]  Lv5 Overdrive passive grants +50% fire rate for extreme single-target DPS.
 */
public class SingleTargetTower extends Tower {

    /**
     * [PURPOSE]       Constructs SingleTargetTower instance at grid coordinates.
     * [OOP / PATTERN] Superclass Constructor Invocation.
     * [CALLED BY]     btd/factory/TowerFactory.java.
     * [CONNECTS TO]   btd/model/tower/Tower.java.
     */
    public SingleTargetTower(TowerType type, double x, double y) {
        super(type, x, y);
    }

    /**
     * [PURPOSE]       Locates furthest living enemy in range, applies damage, and creates tracer line effect.
     * [OOP / PATTERN] Method Overriding (Template Method attack hook).
     * [CALLED BY]     btd/model/tower/Tower.java (update()).
     * [CONNECTS TO]   btd/model/enemy/Enemy.java (takeDamage()), btd/core/VisualEffect.java (line()).
     * [DEFENSE NOTE]  Returns true if fired, prompting cooldown timer reset in base class.
     */
    @Override
    protected boolean attack(List<Enemy> enemies, List<VisualEffect> effects) {
        Enemy target = furthestInRange(enemies);
        if (target == null) {
            return false;
        }
        target.takeDamage(getDamage());
        effects.add(VisualEffect.line(getX(), getY(), target.getX(), target.getY(), new Color(255, 230, 120), 0.12));
        return true;
    }

    /**
     * [PURPOSE]       Activates Level 5 Overdrive passive: multiplies fire rate by 1.5x.
     * [OOP / PATTERN] Hook Method Overriding.
     * [CALLED BY]     btd/model/tower/Tower.java (applyLevel()).
     * [CONNECTS TO]   passiveRate field.
     * [DEFENSE NOTE]  Stacking 1.5x on top of Lv5 2.3x multiplier gives 3.45x baseline attack speed.
     */
    @Override
    protected void applyMaxPassive() {
        passiveRate = 1.5;
    }

    /**
     * [PURPOSE]       Returns description of Level 5 Overdrive bonus for side panel.
     * [OOP / PATTERN] Polymorphic Property Accessor.
     * [CALLED BY]     btd/ui/screens/SidePanel.java.
     * [CONNECTS TO]   UI display.
     */
    @Override
    public String getPassiveDescription() {
        return "Lv 5 Overdrive: shoots 50% faster.";
    }
}
