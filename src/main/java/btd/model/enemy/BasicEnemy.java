package btd.model.enemy;

/** Baseline speed and health. */
public class BasicEnemy extends Enemy {

    public BasicEnemy(double healthScale, double speedScale, int reward) {
        super(30 * healthScale, 55 * speedScale, reward, 1);
    }

    @Override
    public double getRadius() {
        return 11;
    }
}
