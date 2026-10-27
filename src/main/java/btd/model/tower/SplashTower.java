package btd.model.tower;

import btd.core.VisualEffect;
import btd.model.enemy.Enemy;

import java.awt.Color;
import java.util.List;

/** Hits a target and damages every enemy within the splash radius. Can also poison or burn them. */
public class SplashTower extends Tower {

    public SplashTower(TowerType type, double x, double y) {
        super(type, x, y);
    }

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

    private void applyEffect(Enemy e) {
        double seconds = getType().effectDurationMs() / 1000.0;
        switch (getType().special()) {
            case POISON -> e.poison(getType().effectValue() * levelMultiplier(), seconds);
            case BURN -> e.burn(getType().effectValue() * levelMultiplier(), seconds);
            default -> { }
        }
    }

    private Color blastColor() {
        return switch (getType().special()) {
            case POISON -> new Color(160, 80, 220);
            case BURN -> new Color(255, 120, 30);
            default -> new Color(255, 190, 60);
        };
    }

    @Override
    protected void applyMaxPassive() {
        passiveSplash = 1.5;
    }

    @Override
    public String getPassiveDescription() {
        return "Lv 5 Wide Blast: 50% bigger splash.";
    }
}
