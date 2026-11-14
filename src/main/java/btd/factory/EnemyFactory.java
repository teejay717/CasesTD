package btd.factory;

import btd.model.enemy.ArmoredEnemy;
import btd.model.enemy.BasicEnemy;
import btd.model.enemy.BossEnemy;
import btd.model.enemy.Enemy;
import btd.model.enemy.FastEnemy;

/**
 * [PURPOSE]       Instantiates concrete Enemy subclasses with wave-scaled health and speed multipliers.
 * [OOP / PATTERN] Factory Method Pattern / Encapsulated Object Creation.
 * [CALLED BY]     btd/core/WaveSpawner.java (update() method when deploying queued enemies).
 * [CONNECTS TO]   btd/model/enemy/Enemy.java, btd/model/enemy/BasicEnemy.java,
 *                 btd/model/enemy/FastEnemy.java, btd/model/enemy/ArmoredEnemy.java,
 *                 btd/model/enemy/BossEnemy.java.
 * [DEFENSE NOTE]  Hides subclass constructor scaling formulas behind a clean, single-point factory call.
 */
public final class EnemyFactory {

    private EnemyFactory() {
    }

    /**
     * [PURPOSE]       Constructs an enemy subclass scaled by wave multipliers and bounty coins.
     * [OOP / PATTERN] Factory Method / Polymorphic Construction.
     * [CALLED BY]     btd/core/WaveSpawner.java (update()).
     * [CONNECTS TO]   Concrete Enemy subclasses (BasicEnemy, FastEnemy, ArmoredEnemy, BossEnemy).
     * [DEFENSE NOTE]  Scales stats dynamically per wave; BossEnemy automatically spawns on 10th waves.
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
