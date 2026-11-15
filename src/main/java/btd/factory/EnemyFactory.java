package btd.factory;

import btd.model.enemy.ArmoredEnemy;
import btd.model.enemy.BasicEnemy;
import btd.model.enemy.BossEnemy;
import btd.model.enemy.Enemy;
import btd.model.enemy.FastEnemy;

/**
 * [PURPOSE]       Turns an enemy type name ("BasicEnemy", "BossEnemy"...) into the right Enemy subclass object.
 * [OOP / PATTERN] Factory Pattern (Simple / Static Factory) / Utility Class (final + private constructor).
 * [CALLED BY]     btd/core/WaveSpawner.java (update(), when a queued enemy's spawn time arrives).
 * [CONNECTS TO]   btd/model/enemy/Enemy.java, btd/model/enemy/BasicEnemy.java,
 *                 btd/model/enemy/FastEnemy.java, btd/model/enemy/ArmoredEnemy.java,
 *                 btd/model/enemy/BossEnemy.java.
 * [DEFENSE NOTE]  The type names come from waves_config.json and WaveSpawner, so they must match these cases exactly.
 */
public final class EnemyFactory {

    private EnemyFactory() {
    }

    /**
     * [PURPOSE]       Creates one enemy. healthScale / speedScale come from GameConfig for the current wave,
     *                 reward is its kill coins (e.g. BasicEnemy = 10).
     * [OOP / PATTERN] Static Factory Method / returns the parent type Enemy (polymorphism).
     * [CALLED BY]     btd/core/WaveSpawner.java (update()).
     * [CONNECTS TO]   Concrete Enemy subclasses (BasicEnemy, FastEnemy, ArmoredEnemy, BossEnemy).
     * [DEFENSE NOTE]  This method does not decide WHEN a boss comes; WaveSpawner.begin() adds "BossEnemy" every 10th wave.
     *                 An unknown name throws IllegalArgumentException.
     */
    public static Enemy create(String typeName, double healthScale, double speedScale, int reward) {
        return switch (typeName) {
            case "BasicEnemy" -> new BasicEnemy(healthScale, speedScale, reward);
            case "FastEnemy" -> new FastEnemy(healthScale, speedScale, reward);
            case "ArmoredEnemy" -> new ArmoredEnemy(healthScale, speedScale, reward);
            case "BossEnemy" -> new BossEnemy(healthScale, speedScale, reward);
            default -> throw new IllegalArgumentException("Unknown enemy type: " + typeName);
        };
    }
}
