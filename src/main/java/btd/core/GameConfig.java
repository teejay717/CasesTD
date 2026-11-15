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
 * [CALLED BY]     Loaded by btd/ui/AppController.java (loadData()); read by btd/core/GameSession.java,
 *                 btd/core/GameEngine.java, btd/core/WaveSpawner.java, btd/core/UpgradeCostTable.java, GamePanel.
 * [CONNECTS TO]   data/maps/waves_config.json (via the Gson library).
 * [DEFENSE NOTE]  Balance numbers live in JSON, not Java. Change the JSON and restart the game to retune it;
 *                 no recompiling needed. Gson fills these public fields by matching the JSON key names.
 */
public class GameConfig {

    /** [PURPOSE] The "economy" block of the JSON: starting coins/lives, kill rewards, sell refunds, upgrade costs. */
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

    /** [PURPOSE] The "scaling" block of the JSON: how fast enemy health/speed grow and how often a boss comes. */
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

    /** [PURPOSE] One hand-made wave from the JSON "waves" list (currently waves 1, 2 and 3). */
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
     * [DEFENSE NOTE]  Throws IllegalStateException with the full path if the file cannot be read
     *                 (usually because the game was not started from the project root folder).
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

    /** [PURPOSE] Coins for killing this enemy type (from coinsPerEnemy; 5 if the type is missing). Called by WaveSpawner.update(). */
    public int killReward(String enemyType) {
        return economy.coinsPerEnemy.getOrDefault(enemyType, 5);
    }

    /** [PURPOSE] Coins for clearing a wave: 100 + 10 x wave (e.g. wave 5 = 150). Called by GameEngine.update(). */
    public int waveClearReward(int wave) {
        return economy.waveClearBaseReward + economy.waveClearRewardPerWave * wave;
    }

    /**
     * [PURPOSE]       Returns the enemy health multiplier for a wave (1.0 = base health).
     * [OOP / PATTERN] Difficulty Scaling Formula.
     * [CALLED BY]     btd/core/WaveSpawner.java (update()), result is passed into btd/factory/EnemyFactory.java.
     * [DEFENSE NOTE]  Two parts, with the current JSON numbers:
     *                 1) linear: 0.8 x (1 + 0.30 x (wave - 1))  -> wave 1 = 0.8, wave 2 = 1.04, wave 10 = 2.96
     *                 2) jump:   x 1.45 for every 10 waves passed -> the jump happens at wave 11, 21, 31...
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
     * [PURPOSE]       Returns the enemy speed multiplier for a wave (+2% per wave, capped at +50%, so max 1.5x from wave 26).
     * [OOP / PATTERN] Linear Clamped Formula.
     * [CALLED BY]     btd/core/WaveSpawner.java (update()).
     */
    public double speedScale(int wave) {
        double bonus = Math.min(scaling.speedCapPercent, scaling.speedPerWavePercent * (wave - 1));
        return 1.0 + bonus / 100.0;
    }
}
