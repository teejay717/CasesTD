package btd.core;

import btd.model.enemy.Enemy;
import btd.model.tower.Tower;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * [PURPOSE]       Runs the game simulation one step at a time: spawns enemies, applies auras, lets towers shoot,
 *                 moves enemies, gives coins, takes lives, and decides wave-cleared / game-over.
 * [OOP / PATTERN] Game Loop step / Mediator (it coordinates towers, enemies, spawner and session).
 *                 State is an enum: BETWEEN_WAVES -> WAVE_ACTIVE -> BETWEEN_WAVES ... -> GAME_OVER.
 * [CALLED BY]     btd/ui/screens/GamePanel.java (startRun() creates it; advance() calls update(STEP) 60 times a second).
 * [CONNECTS TO]   btd/core/WaveSpawner.java, btd/core/GameSession.java,
 *                 btd/model/enemy/Enemy.java, btd/model/tower/Tower.java,
 *                 btd/core/VisualEffect.java.
 * [DEFENSE NOTE]  The engine has no Swing code at all. GamePanel draws; GameEngine only does the rules.
 */
public class GameEngine {

    /** State machine representing wave progression lifecycle. */
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

    /**
     * [PURPOSE]       Creates the engine for one run and its own WaveSpawner.
     * [OOP / PATTERN] Constructor / Dependency Injection (config, map, session are passed in).
     * [CALLED BY]     btd/ui/screens/GamePanel.java (startRun()).
     * [CONNECTS TO]   btd/core/WaveSpawner.java.
     */
    public GameEngine(GameConfig config, GameMap map, GameSession session) {
        this.config = config;
        this.map = map;
        this.session = session;
        this.spawner = new WaveSpawner(config);
    }

    /**
     * [PURPOSE]       Transitions engine state to WAVE_ACTIVE and starts spawning wave queue.
     * [OOP / PATTERN] State Transition Method / Guard Clause.
     * [CALLED BY]     btd/ui/screens/GamePanel.java (startWave()), which runs when you click START WAVE
     *                 in the SidePanel or press Space.
     * [CONNECTS TO]   btd/core/WaveSpawner.java (begin()).
     * [DEFENSE NOTE]  Returns false if a wave is already running or the game is over, so double-clicking does nothing.
     */
    public boolean startNextWave() {
        if (state != State.BETWEEN_WAVES) {
            return false;
        }
        wave++;
        spawner.begin(wave);
        state = State.WAVE_ACTIVE;
        return true;
    }

    /**
     * [PURPOSE]       One simulation step (dt is always 1/60 s). Order:
     *                 1) age the visual effects   2) stop here if no wave is running
     *                 3) spawn enemies that are due   4) reset buffs, then apply Radio Tower auras
     *                 5) every tower shoots (Tower.update)   6) move every enemy; dead -> coins, reached end -> lose lives
     *                 7) lives = 0 -> GAME_OVER; spawner empty and no enemies left -> wave cleared, reward coins.
     * [OOP / PATTERN] Game Loop Step / Mediator.
     * [CALLED BY]     btd/ui/screens/GamePanel.java (advance(), inside the fixed-timestep while loop).
     * [CONNECTS TO]   btd/core/WaveSpawner.java, btd/model/tower/Tower.java, btd/model/enemy/Enemy.java.
     * [DEFENSE NOTE]  Buffs are reset to 1.0 and re-applied every step, so if a Radio Tower is sold the buff disappears
     *                 at once. An Iterator is used so enemies can be removed safely while looping over the list.
     */
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

    /** [PURPOSE] Counts down every visual effect and removes the finished ones. Runs even between waves so shots fade out. */
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

    /** [PURPOSE] Current engine state (BETWEEN_WAVES, WAVE_ACTIVE, GAME_OVER). */
    public State getState() { return state; }
    /** [PURPOSE] Currently active or most recently completed wave number. */
    public int getWave() { return wave; }
    /** [PURPOSE] Waves cleared this run. This is the "score" saved as highest_score in the database. */
    public int getWavesCleared() { return wavesCleared; }
    /** [PURPOSE] Total enemies killed this run. */
    public int getKills() { return kills; }
    /** [PURPOSE] Gold coins awarded on the last cleared wave. */
    public int getLastWaveReward() { return lastWaveReward; }
    /** [PURPOSE] Enemies currently on the map (GamePanel draws them; dead ones are already removed in update()). */
    public List<Enemy> getEnemies() { return enemies; }
    /** [PURPOSE] Active visual tracer line and circle animations. */
    public List<VisualEffect> getEffects() { return effects; }
    /** [PURPOSE] Reference to game map and waypoint geometry. */
    public GameMap getMap() { return map; }
    /** [PURPOSE] Reference to active session state (gold, lives, towers). */
    public GameSession getSession() { return session; }
    /** [PURPOSE] Enemies left this wave: on the map + not spawned yet. Shown in the HUD as "WAVE n (x left)". */
    public int getEnemiesLeft() { return enemies.size() + spawner.remaining(); }
}
