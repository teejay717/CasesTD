package btd.core;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/** Game numbers loaded from data/maps/waves_config.json, so they can be tuned without recompiling. */
public class GameConfig {

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

    public static class SpawnDef {
        public String type;
        public int count;
        public int intervalMs;
    }

    public static class WaveDef {
        public int wave;
        public List<SpawnDef> spawns;
    }

    public Economy economy;
    public Scaling scaling;
    public List<WaveDef> waves;

    public static GameConfig load(String jsonFile) {
        Path file = Paths.get(jsonFile);
        try (Reader reader = Files.newBufferedReader(file)) {
            return new Gson().fromJson(reader, GameConfig.class);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + file.toAbsolutePath()
                    + ". Run the game from the project root.", e);
        }
    }

    public WaveDef explicitWave(int wave) {
        for (WaveDef w : waves) {
            if (w.wave == wave) {
                return w;
            }
        }
        return null;
    }

    public int killReward(String enemyType) {
        return economy.coinsPerEnemy.getOrDefault(enemyType, 5);
    }

    public int waveClearReward(int wave) {
        return economy.waveClearBaseReward + economy.waveClearRewardPerWave * wave;
    }

    /**
     * Enemy health multiplier for a wave. Health grows a little every wave (healthPerWavePercent
     * of the base), and each time a new block of waves starts (wave 11, 21, 31...) it is multiplied
     * again by healthJumpPercent, so 10 to 11 is a much bigger step than 11 to 12.
     * Everything is scaled by healthStartPercent (80 = wave 1 enemies have 80% of base health).
     * With 80%, 30% and 45%: wave 10 = 2.96x, wave 11 = 4.64x, wave 31 = 24.4x, wave 41 = 46x.
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

    /** Enemy speed multiplier for a wave: +2% per wave, capped at +50%. */
    public double speedScale(int wave) {
        double bonus = Math.min(scaling.speedCapPercent, scaling.speedPerWavePercent * (wave - 1));
        return 1.0 + bonus / 100.0;
    }
}
