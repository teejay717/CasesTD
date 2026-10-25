package btd.model.tower;

import btd.core.VisualEffect;
import btd.model.enemy.Enemy;

import java.awt.Color;
import java.util.List;

/** Hits the one enemy furthest along the track. */
public class SingleTargetTower extends Tower {

    public SingleTargetTower(TowerType type, double x, double y) {
        super(type, x, y);
    }

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

    @Override
    protected void applyMaxPassive() {
        passiveRate = 1.5;
    }

    @Override
    public String getPassiveDescription() {
        return "Lv 5 Overdrive: shoots 50% faster.";
    }
}
