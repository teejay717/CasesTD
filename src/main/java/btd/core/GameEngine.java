package btd.core;

import btd.model.enemy.Enemy;
import btd.model.tower.Tower;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** The game loop logic: runs waves, moves enemies, fires towers, pays rewards and ends the run. */
public class GameEngine {

    public enum State { BETWEEN_WAVES, WAVE_ACTIVE, GAME_OVER }

    private final GameConfig config;
    private final GameMap map;
    private final GameSession session;
    private final WaveSpawner spawner;

    private final List<Enemy> enemies = new ArrayList<>();
    private final List<VisualEffect> effects = new ArrayList<>();

    private State state = State.BETWEEN_WAVES;
    private int wave;          // the wave that is running or just finished (0 before the first)
    private int wavesCleared;
    private int kills;
    private int lastWaveReward;

    public GameEngine(GameConfig config, GameMap map, GameSession session) {
        this.config = config;
        this.map = map;
        this.session = session;
        this.spawner = new WaveSpawner(config);
    }

    /** Starts the next wave. Does nothing while a wave is running or after game over. */
    public boolean startNextWave() {
        if (state != State.BETWEEN_WAVES) {
            return false;
        }
        wave++;
        spawner.begin(wave);
        state = State.WAVE_ACTIVE;
        return true;
    }

    public void update(double dt) {
        updateEffects(dt);
        if (state != State.WAVE_ACTIVE) {
            return;
        }

        enemies.addAll(spawner.update(dt));

        List<Tower> towers = session.getTowers();
        for (Tower t : towers) {
            t.resetBuff();
        }
        for (Tower t : towers) {
            t.applyAura(towers);
        }
        for (Tower t : towers) {
            t.update(dt, enemies, effects);
        }

        Iterator<Enemy> it = enemies.iterator();
        while (it.hasNext()) {
            Enemy e = it.next();
            e.update(dt, map);
            if (e.isDead()) {
                session.addCoins(e.getReward());
                kills++;
                it.remove();
            } else if (e.hasReachedEnd(map)) {
                session.loseLives(e.getLeakDamage());
                it.remove();
            }
        }

        if (session.getLives() <= 0) {
            state = State.GAME_OVER;
            return;
        }
        if (spawner.isFinished() && enemies.isEmpty()) {
            wavesCleared = wave;
            lastWaveReward = config.waveClearReward(wave);
            session.addCoins(lastWaveReward);
            state = State.BETWEEN_WAVES;
        }
    }

    private void updateEffects(double dt) {
        Iterator<VisualEffect> it = effects.iterator();
        while (it.hasNext()) {
            VisualEffect fx = it.next();
            fx.update(dt);
            if (fx.isDone()) {
                it.remove();
            }
        }
    }

    public State getState() { return state; }
    public int getWave() { return wave; }
    public int getWavesCleared() { return wavesCleared; }
    public int getKills() { return kills; }
    public int getLastWaveReward() { return lastWaveReward; }
    public List<Enemy> getEnemies() { return enemies; }
    public List<VisualEffect> getEffects() { return effects; }
    public GameMap getMap() { return map; }
    public GameSession getSession() { return session; }
    public int getEnemiesLeft() { return enemies.size() + spawner.remaining(); }
}
