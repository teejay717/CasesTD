package btd.core;

import btd.factory.EnemyFactory;
import btd.model.enemy.Enemy;

import java.util.ArrayList;
import java.util.List;

/**
 * [PURPOSE]       Manages enemy spawn queue for each wave, converting wave numbers into timed spawn events.
 * [OOP / PATTERN] Spawner Pattern / Queue Scheduling / Factory Orchestration.
 * [CALLED BY]     btd/core/GameEngine.java (startNextWave(), update()).
 * [CONNECTS TO]   btd/core/GameConfig.java, btd/factory/EnemyFactory.java, btd/model/enemy/Enemy.java.
 * [DEFENSE NOTE]  Waves 1-3 use handcrafted JSON definitions; wave 4+ are procedurally generated with boss every 10th wave.
 */
public class WaveSpawner {

    /** [PURPOSE] Immutable record defining scheduled spawn timestamp and enemy archetype string. */
    private record Spawn(double time, String type) {
    }

    private final GameConfig config;
    private final List<Spawn> queue = new ArrayList<>();
    private double elapsed;
    private int next;
    private int wave;

    /**
     * [PURPOSE]       Constructs WaveSpawner tied to game configuration.
     * [OOP / PATTERN] Constructor / Dependency Injection.
     * [CALLED BY]     btd/core/GameEngine.java constructor.
     */
    public WaveSpawner(GameConfig config) {
        this.config = config;
    }

    /**
     * [PURPOSE]       Builds timed spawn queue for requested wave number, appending boss on every 10th wave.
     * [OOP / PATTERN] Queue Initialization / Procedural Generation Dispatch.
     * [CALLED BY]     btd/core/GameEngine.java (startNextWave()).
     * [CONNECTS TO]   config.explicitWave(), generate().
     * [DEFENSE NOTE]  Resets elapsed time and queue index to zero for clean wave execution.
     */
    public void begin(int waveNumber) {
        wave = waveNumber;
        queue.clear();
        elapsed = 0;
        next = 0;

        GameConfig.WaveDef def = config.explicitWave(waveNumber);
        List<GameConfig.SpawnDef> groups = def != null ? def.spawns : generate(waveNumber);

        double time = 0;
        for (GameConfig.SpawnDef group : groups) {
            for (int i = 0; i < group.count; i++) {
                queue.add(new Spawn(time, group.type));
                time += group.intervalMs / 1000.0;
            }
            time += 1.0; // short pause between groups
        }
        if (waveNumber % config.scaling.bossEveryNthWave == 0) {
            queue.add(new Spawn(time + 1.0, "BossEnemy"));
        }
    }

    /**
     * [PURPOSE]       Procedural generator for infinite waves beyond wave 3, increasing counts and adding enemy variants.
     * [OOP / PATTERN] Procedural Content Generation (PCG).
     * [CALLED BY]     btd/core/WaveSpawner.java (begin()).
     * [DEFENSE NOTE]  BasicEnemies scale linearly; FastEnemies unlock at wave 3; ArmoredEnemies unlock at wave 5.
     */
    private List<GameConfig.SpawnDef> generate(int n) {
        List<GameConfig.SpawnDef> groups = new ArrayList<>();
        groups.add(group("BasicEnemy", 6 + 2 * n, Math.max(350, 800 - 10 * n)));
        if (n >= 3) {
            groups.add(group("FastEnemy", n + 1, Math.max(300, 600 - 10 * n)));
        }
        if (n >= 5) {
            groups.add(group("ArmoredEnemy", n - 3, Math.max(500, 1200 - 20 * n)));
        }
        return groups;
    }

    /** [PURPOSE] Helper factory instantiating a SpawnDef configuration object. */
    private GameConfig.SpawnDef group(String type, int count, int intervalMs) {
        GameConfig.SpawnDef g = new GameConfig.SpawnDef();
        g.type = type;
        g.count = count;
        g.intervalMs = intervalMs;
        return g;
    }

    /**
     * [PURPOSE]       Advances wave clock, instantiating and releasing all enemies whose scheduled spawn times have elapsed.
     * [OOP / PATTERN] Queue Polling / Factory Method Invocation.
     * [CALLED BY]     btd/core/GameEngine.java (update()).
     * [CONNECTS TO]   btd/factory/EnemyFactory.java (create()).
     * [DEFENSE NOTE]  Scales enemy HP and speed via config formulas matching active wave number.
     */
    public List<Enemy> update(double dt) {
        elapsed += dt;
        List<Enemy> spawned = new ArrayList<>();
        double healthScale = config.healthScale(wave);
        double speedScale = config.speedScale(wave);
        while (next < queue.size() && queue.get(next).time() <= elapsed) {
            String type = queue.get(next).type();
            spawned.add(EnemyFactory.create(type, healthScale, speedScale, config.killReward(type)));
            next++;
        }
        return spawned;
    }

    /** [PURPOSE] Returns true when all scheduled enemies in queue have emerged onto map. */
    public boolean isFinished() {
        return next >= queue.size();
    }

    /** [PURPOSE] Number of unspawned enemies remaining in current wave queue. */
    public int remaining() {
        return queue.size() - next;
    }
}
