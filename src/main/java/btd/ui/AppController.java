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
 * [PURPOSE]       Creates the 3 screens (landing, game, results), switches between them, and loads the game data once.
 * [OOP / PATTERN] Controller / Mediator (screens never talk to each other directly; they call back into this class).
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
     * [OOP / PATTERN] Constructor / Callbacks via method references (this::startRun is a Runnable).
     * [CALLED BY]     btd/Main.java (main()).
     * [CONNECTS TO]   btd/ui/MainFrame.java (addScreen(), showScreen()).
     * [DEFENSE NOTE]  LandingPanel gets this::startRun, so pressing Play there runs startRun() here.
     *                 GamePanel gets this::showResults, so game over runs showResults() here.
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
     * [PURPOSE]       Starts a run: load data if needed, give it to GamePanel, show the game screen.
     * [OOP / PATTERN] Callback / Screen Transition.
     * [CALLED BY]     btd/ui/screens/LandingPanel.java (Play button), btd/ui/screens/ResultsPanel.java (PLAY AGAIN button).
     * [CONNECTS TO]   btd/ui/screens/GamePanel.java (startRun()).
     * [DEFENSE NOTE]  Does nothing if no player has logged in yet, or if loading the data failed.
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
     * [PURPOSE]       Loads towers + cases (MySQL), waves_config.json and map1_path.csv, but only the first time.
     * [OOP / PATTERN] Lazy Initialization / Caching (the "if (x == null)" checks).
     * [CALLED BY]     startRun().
     * [CONNECTS TO]   btd/core/Catalog.java (load()), btd/core/GameConfig.java (load()), btd/core/GameMap.java (load()).
     * [DEFENSE NOTE]  On the 2nd run (PLAY AGAIN) all three are already loaded, so nothing is read again.
     *                 If MySQL is down or a file is missing, it shows a popup instead of crashing.
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
     * [PURPOSE]       Shows the results screen with the finished run's numbers.
     * [OOP / PATTERN] Screen Transition / Callback (GamePanel holds it as a Consumer<RunResult>).
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
