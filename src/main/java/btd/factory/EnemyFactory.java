package btd.factory;

import btd.model.enemy.ArmoredEnemy;
import btd.model.enemy.BasicEnemy;
import btd.model.enemy.BossEnemy;
import btd.model.enemy.Enemy;
import btd.model.enemy.FastEnemy;

/** Builds an enemy from its type name. The scales make later waves tougher and faster. */
public final class EnemyFactory {

    private EnemyFactory() {
    }

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
