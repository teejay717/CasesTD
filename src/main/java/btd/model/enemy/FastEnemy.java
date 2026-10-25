package btd.model.enemy;

/** Higher speed, lower health. */
public class FastEnemy extends Enemy {

    public FastEnemy(double healthScale, double speedScale, int reward) {
        super(18 * healthScale, 105 * speedScale, reward, 1);
    }

    @Override
    public double getRadius() {
        return 9;
    }
}
