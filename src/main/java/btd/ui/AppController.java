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
 * Connects the screens: landing -> game -> results -> landing (or a new game).
 * It also loads the database catalog and config files once, when the first run starts.
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

    /** Loads the catalog from MySQL and the JSON/CSV files, only the first time. */
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

    private void showResults(RunResult result) {
        results.show(result, landing.getPlayer().getPlayerName());
        frame.showScreen("results");
    }

    private void showMenu() {
        landing.repaint();
        frame.showScreen("landing");
    }
}
