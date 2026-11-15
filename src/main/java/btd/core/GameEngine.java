package btd.core;

import btd.model.enemy.Enemy;
import btd.model.tower.Tower;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * [PURPOSE]       Core simulation coordinator driving the 60 FPS tick loop: spawns enemies,
 *                 updates aura buffs, fires towers, culls dead units, awards gold, and checks game over.
 * [OOP / PATTERN] Game Loop Pattern / Facade Pattern / Mediator Pattern.
 * [CALLED BY]     btd/ui/screens/GamePanel.java (actionPerformed() timer hook).
 * [CONNECTS TO]   btd/core/WaveSpawner.java, btd/core/GameSession.java,
 *                 btd/model/enemy/Enemy.java, btd/model/tower/Tower.java,
 *                 btd/core/VisualEffect.java.
 * [DEFENSE NOTE]  Orchestrates all active game subsystems in strict sequence each frame.
 */
public class GameEngine {

    /** State machine representing wave progression lifecycle. */
    public enum State { BETWEEN_WAVES, WAVE_ACTIVE, GAME_OVER }

    private final GameConfig config;
    private final GameMap map;
    private final GameSession session;
    private final WaveSpawner spawner;

    private final List<Enemy> enemies = new ArrayList<>();
    /**
     * Master visual animation queue for the entire game.
     * [DEFENSE NOTE] There is ONE shared effects list passed down to all towers rather than each tower
     * owning its own queue. Benefits:
     * 1. Memory efficiency: exactly 1 list instead of 30+ separate lists for 30 placed towers.
     * 2. Single-pass rendering: GamePanel paints all active effects in one clean loop.
     * 3. Persistence: if a tower is sold while its laser is fading, the animation finishes smoothly.
     * 4. Mediator Pattern / MVC: models remain decoupled from Swing graphics.
     */
    private final List<VisualEffect> effects = new ArrayList<>();

    private State state = State.BETWEEN_WAVES;
    private int wave;          // the wave that is running or just finished (0 before the first)
    private int wavesCleared;
    private int kills;
    private int lastWaveReward;

    /**
     * [PURPOSE]       Constructs GameEngine and instantiates WaveSpawner.
     * [OOP / PATTERN] Constructor / Dependency Injection.
     * [CALLED BY]     btd/ui/AppController.java (startNewGame()).
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
     * [CALLED BY]     btd/ui/screens/SidePanel.java (startWaveButton), btd/ui/screens/GamePanel.java (spacebar shortcut).
     * [CONNECTS TO]   btd/core/WaveSpawner.java (begin()).
     * [DEFENSE NOTE]  Returns false if a wave is already running or game is over.
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
     * [PURPOSE]       Executes a single simulation tick: updates effects, spawns units, recalculates auras,
     *                 fires towers, moves enemies along waypoints, awards gold, and triggers game over.
     * [OOP / PATTERN] Main Game Loop Step / Mediator Pattern.
     * [CALLED BY]     btd/ui/screens/GamePanel.java (Swing Timer tick).
     * [CONNECTS TO]   btd/core/WaveSpawner.java, btd/model/tower/Tower.java, btd/model/enemy/Enemy.java.
     * [DEFENSE NOTE]  Aura buffs are reset and recalculated every frame before tower attack phases run.
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
            // [COORDINATE & STATE UPDATE] Recalculates distance += speed * dt, resolves LERP pointAt(),
            // and updates e.x = p[0] and e.y = p[1] screen coordinates for this frame:
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

    /** [PURPOSE] Iterates visual attack effect timers and culls expired animations. */
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
    /** [PURPOSE] Total number of waves successfully cleared this run. */
    public int getWavesCleared() { return wavesCleared; }
    /** [PURPOSE] Total enemies killed this run. */
    public int getKills() { return kills; }
    /** [PURPOSE] Gold coins awarded on the last cleared wave. */
    public int getLastWaveReward() { return lastWaveReward; }
    /** [PURPOSE] List of active living enemies on the battlefield. */
    public List<Enemy> getEnemies() { return enemies; }
    /** [PURPOSE] Active visual tracer line and circle animations. */
    public List<VisualEffect> getEffects() { return effects; }
    /** [PURPOSE] Reference to game map and waypoint geometry. */
    public GameMap getMap() { return map; }
    /** [PURPOSE] Reference to active session state (gold, lives, towers). */
    public GameSession getSession() { return session; }
    /** [PURPOSE] Total count of remaining enemies: alive on map plus queued in spawner. */
    public int getEnemiesLeft() { return enemies.size() + spawner.remaining(); }
}
