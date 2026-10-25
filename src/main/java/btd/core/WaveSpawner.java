package btd.core;

import btd.factory.EnemyFactory;
import btd.model.enemy.Enemy;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns a wave number into a timed list of enemies. Waves 1 to 3 come from the JSON file;
 * later waves are generated so the game can keep going. Every 10th wave adds a boss.
 */
public class WaveSpawner {

    private record Spawn(double time, String type) {
    }

    private final GameConfig config;
    private final List<Spawn> queue = new ArrayList<>();
    private double elapsed;
    private int next;
    private int wave;

    public WaveSpawner(GameConfig config) {
        this.config = config;
    }

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

    /** Waves after the hand-written ones: more of everything, with Fast and Armored zombies mixed in. */
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

    private GameConfig.SpawnDef group(String type, int count, int intervalMs) {
        GameConfig.SpawnDef g = new GameConfig.SpawnDef();
        g.type = type;
        g.count = count;
        g.intervalMs = intervalMs;
        return g;
    }

    /** Advances the clock and returns the enemies that appear this frame. */
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

    public boolean isFinished() {
        return next >= queue.size();
    }

    public int remaining() {
        return queue.size() - next;
    }
}
