package btd.core;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * [PURPOSE]       Parses and holds game balance numbers from 'data/maps/waves_config.json' (economy, scaling, waves).
 * [OOP / PATTERN] Configuration Object Pattern / JSON Deserialization (Gson) / Data Binding.
 * [CALLED BY]     btd/ui/AppController.java (loadData()), btd/core/GameSession.java, btd/core/GameEngine.java, btd/core/WaveSpawner.java.
 * [CONNECTS TO]   data/maps/waves_config.json.
 * [DEFENSE NOTE]  Decouples balancing formulas and spawn curves from Java code, allowing tuning without recompiling (edit the JSON, restart the game).
 */
public class GameConfig {

    /** [PURPOSE] Nested POJO binding economy configurations (starting gold, lives, kill rewards, sell refunds). */
    public static class Economy {
        public int startingCoins;
        public int freeStarterCases;
        public int startingLives;
        public int waveClearBaseReward;
        public int waveClearRewardPerWave;
        public Map<String, Integer> coinsPerEnemy;
        public Map<String, Integer> sellRefund;
        public Map<String, int[]> upgradeCopyCosts;
    }

    /** [PURPOSE] Nested POJO binding wave difficulty scaling parameters (health growth, speed caps, boss intervals). */
    public static class Scaling {
        /** Enemy health on wave 1, as a % of the base stats (80 = start 20% weaker). */
        public int healthStartPercent;
        public int healthPerWavePercent;
        /** Every this many waves, enemy health gets an extra jump (0 = no jumps). */
        public int healthJumpEveryNWaves;
        /** Size of each jump, as a % multiplier on top of the per-wave growth. */
        public int healthJumpPercent;
        public int speedPerWavePercent;
        public int speedCapPercent;
        public int bossEveryNthWave;
    }

    /** [PURPOSE] Specification of enemy grouping within a scripted wave (type, count, spawn cadence). */
    public static class SpawnDef {
        public String type;
        public int count;
        public int intervalMs;
    }

    /** [PURPOSE] Specification of an explicit hand-crafted wave. */
    public static class WaveDef {
        public int wave;
        public List<SpawnDef> spawns;
    }

    public Economy economy;
    public Scaling scaling;
    public List<WaveDef> waves;

    /**
     * [PURPOSE]       Reads JSON file from disk and parses it into strongly-typed GameConfig instance via Google Gson.
     * [OOP / PATTERN] Static Factory Method / Serialization Pattern.
     * [CALLED BY]     btd/ui/AppController.java (loadData()).
     * [CONNECTS TO]   com.google.gson.Gson.
     * [DEFENSE NOTE]  Throws IllegalStateException with helpful path instruction if file cannot be found.
     */
    public static GameConfig load(String jsonFile) {
        Path file = Paths.get(jsonFile);
        try (Reader reader = Files.newBufferedReader(file)) {
            return new Gson().fromJson(reader, GameConfig.class);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + file.toAbsolutePath()
                    + ". Run the game from the project root.", e);
        }
    }

    /**
     * [PURPOSE]       Finds scripted wave definition matching wave number, or returns null if wave must be procedurally generated.
     * [OOP / PATTERN] Query Method / Optional Return Pattern.
     * [CALLED BY]     btd/core/WaveSpawner.java (begin()).
     */
    public WaveDef explicitWave(int wave) {
        for (WaveDef w : waves) {
            if (w.wave == wave) {
                return w;
            }
        }
        return null;
    }

    /** [PURPOSE] Looks up gold bounty awarded when defeating given enemy archetype. */
    public int killReward(String enemyType) {
        return economy.coinsPerEnemy.getOrDefault(enemyType, 5);
    }

    /** [PURPOSE] Calculates gold reward distributed upon completing wave number (base + per-wave bonus). */
    public int waveClearReward(int wave) {
        return economy.waveClearBaseReward + economy.waveClearRewardPerWave * wave;
    }

    /**
     * [PURPOSE]       Calculates the enemy health scale multiplier for wave number (linear growth + jumps).
     * [OOP / PATTERN] Mathematical Progression Formula / Compound Difficulty Scaling.
     * [CALLED BY]     btd/core/WaveSpawner.java (update()).
     * [CONNECTS TO]   btd/factory/EnemyFactory.java.
     * [DEFENSE NOTE]  Combines linear per-wave growth with a compound jump multiplier after every 10th wave (waves 10->11, 20->21, 30->31...).
     */
    public double healthScale(int wave) {
        double start = scaling.healthStartPercent > 0 ? scaling.healthStartPercent / 100.0 : 1.0;
        double perWave = start * (1.0 + scaling.healthPerWavePercent / 100.0 * (wave - 1));
        if (scaling.healthJumpEveryNWaves <= 0) {
            return perWave;
        }
        int jumps = (wave - 1) / scaling.healthJumpEveryNWaves;
        return perWave * Math.pow(1.0 + scaling.healthJumpPercent / 100.0, jumps);
    }

    /**
     * [PURPOSE]       Calculates enemy speed multiplier for wave number (+2% per wave, capped at +50%).
     * [OOP / PATTERN] Linear Clamped Formula.
     * [CALLED BY]     btd/core/WaveSpawner.java (update()).
     */
    public double speedScale(int wave) {
        double bonus = Math.min(scaling.speedCapPercent, scaling.speedPerWavePercent * (wave - 1));
        return 1.0 + bonus / 100.0;
    }
}
