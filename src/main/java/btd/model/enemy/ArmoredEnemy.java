package btd.model.enemy;

/** Slow and tough: every hit does 30% less damage. */
public class ArmoredEnemy extends Enemy {

    private static final double DAMAGE_TAKEN = 0.7;

    public ArmoredEnemy(double healthScale, double speedScale, int reward) {
        super(90 * healthScale, 40 * speedScale, reward, 2);
    }

    @Override
    protected double reduceDamage(double amount) {
        return amount * DAMAGE_TAKEN;
    }

    @Override
    public double getRadius() {
        return 13;
    }
}
