package btd.model.enemy;

/** Very high health, spawns on every 10th wave. Stuns only work for half as long. */
public class BossEnemy extends Enemy {

    public BossEnemy(double healthScale, double speedScale, int reward) {
        super(1200 * healthScale, 32 * speedScale, reward, 10);
    }

    @Override
    public boolean stun(double seconds) {
        return super.stun(seconds / 2);
    }

    @Override
    public double getRadius() {
        return 24;
    }
}
