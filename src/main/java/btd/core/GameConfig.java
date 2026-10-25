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
        public int healthPerWavePercent;
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
    public int levelBonusPercent;
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

    /** Enemy health multiplier for a wave: +50% of the base health per wave after the first. */
    public double healthScale(int wave) {
        return 1.0 + scaling.healthPerWavePercent / 100.0 * (wave - 1);
    }

    /** Enemy speed multiplier for a wave: +2% per wave, capped at +50%. */
    public double speedScale(int wave) {
        double bonus = Math.min(scaling.speedCapPercent, scaling.speedPerWavePercent * (wave - 1));
        return 1.0 + bonus / 100.0;
    }
}
