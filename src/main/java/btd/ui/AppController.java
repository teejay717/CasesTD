package btd.ui;

import btd.core.Catalog;
import btd.core.GameConfig;
import btd.core.GameMap;
import btd.core.RunResult;
import btd.model.player.Player;
import btd.ui.screens.GamePanel;
import btd.ui.screens.LandingPanel;
import btd.ui.screens.ResultsPanel;

import javax.swing.JOptionPane;
import java.sql.SQLException;

/**
 * [PURPOSE]       Master application coordinator managing card-layout screen transitions and lazy catalog/config data loading.
 * [OOP / PATTERN] Controller Pattern (MVC) / Mediator Pattern / Coordinator Pattern.
 * [CALLED BY]     btd/Main.java (main()).
 * [CONNECTS TO]   btd/ui/MainFrame.java, btd/ui/screens/LandingPanel.java,
 *                 btd/ui/screens/GamePanel.java, btd/ui/screens/ResultsPanel.java,
 *                 btd/core/Catalog.java, btd/core/GameConfig.java, btd/core/GameMap.java.
 * [DEFENSE NOTE]  Decouples individual UI view panels from each other; screen switching flows exclusively through AppController.
 */
public class AppController {

    private static final String CONFIG_FILE = "data/maps/waves_config.json";
    private static final String MAP_FILE = "data/maps/map1_path.csv";

    private final MainFrame frame;
    private final LandingPanel landing;
    private final GamePanel game;
    private final ResultsPanel results;

    private Catalog catalog;
    private GameConfig config;
    private GameMap map;

    /**
     * [PURPOSE]       Constructs application controller, registers UI screen panels with MainFrame, and displays Landing screen.
     * [OOP / PATTERN] Constructor / Dependency Wiring / Observer Callbacks.
     * [CALLED BY]     btd/Main.java (main()).
     * [CONNECTS TO]   btd/ui/MainFrame.java (addScreen(), showScreen()).
     */
    public AppController(MainFrame frame) {
        this.frame = frame;
        this.landing = new LandingPanel(this::startRun);
        this.game = new GamePanel(this::showResults);
        this.results = new ResultsPanel(this::startRun, this::showMenu);

        frame.addScreen("landing", landing);
        frame.addScreen("game", game);
        frame.addScreen("results", results);
        frame.showScreen("landing");
    }

    /**
     * [PURPOSE]       Initializes gameplay run: ensures game data is hydrated, binds session, and activates GamePanel.
     * [OOP / PATTERN] Action Callback / State Machine Transition.
     * [CALLED BY]     btd/ui/screens/LandingPanel.java (play button), btd/ui/screens/ResultsPanel.java (play again button).
     * [CONNECTS TO]   btd/ui/screens/GamePanel.java (startRun()).
     * [DEFENSE NOTE]  Validates that a player profile has been chosen before launching into active gameplay.
     */
    private void startRun() {
        Player player = landing.getPlayer();
        if (player == null) {
            return;
        }
        if (!loadData()) {
            return;
        }
        game.startRun(player, catalog, config, map);
        frame.showScreen("game");
        game.requestFocusInWindow();
    }

    /**
     * [PURPOSE]       Lazy loads database catalog, wave config JSON, and map waypoint CSV on first run.
     * [OOP / PATTERN] Lazy Initialization / Singleton Cache.
     * [CALLED BY]     startRun().
     * [CONNECTS TO]   btd/core/Catalog.java (load()), btd/core/GameConfig.java (load()), btd/core/GameMap.java (load()).
     * [DEFENSE NOTE]  Catches SQLException/IllegalStateException and presents user-friendly modal dialog if MySQL is unreachable.
     */
    private boolean loadData() {
        try {
            if (catalog == null) {
                catalog = Catalog.load();
            }
            if (config == null) {
                config = GameConfig.load(CONFIG_FILE);
            }
            if (map == null) {
                map = GameMap.load(MAP_FILE);
            }
            return true;
        } catch (SQLException | IllegalStateException ex) {
            JOptionPane.showMessageDialog(frame, "Could not load the game data:\n" + ex.getMessage()
                    + "\n\nCheck that MySQL is running and that schema.sql and seed_data.sql were run.");
            return false;
        }
    }

    /**
     * [PURPOSE]       Displays game-over results screen with cleared wave statistics and record evaluation.
     * [OOP / PATTERN] Screen Transition / Callback Handler.
     * [CALLED BY]     btd/ui/screens/GamePanel.java (finishRun()).
     * [CONNECTS TO]   btd/ui/screens/ResultsPanel.java (show()).
     */
    private void showResults(RunResult result) {
        results.show(result, landing.getPlayer().getPlayerName());
        frame.showScreen("results");
    }

    /**
     * [PURPOSE]       Returns user from results panel back to main landing page.
     * [OOP / PATTERN] Screen Transition / Navigation Handler.
     * [CALLED BY]     btd/ui/screens/ResultsPanel.java (menu button).
     * [CONNECTS TO]   btd/ui/MainFrame.java (showScreen()).
     */
    private void showMenu() {
        landing.repaint();
        frame.showScreen("landing");
    }
}
