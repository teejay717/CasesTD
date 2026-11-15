package btd.ui.screens;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Consumer;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.Timer;

import btd.core.CaseRNGManager;
import btd.core.Catalog;
import btd.core.GameConfig;
import btd.core.GameEngine;
import btd.core.GameMap;
import btd.core.GameSession;
import btd.core.RunResult;
import btd.core.VisualEffect;
import btd.dao.PlayerDAO;
import btd.model.enemy.Enemy;
import btd.model.player.Player;
import btd.model.tower.SupportTower;
import btd.model.tower.Tower;
import btd.model.tower.TowerType;
import btd.ui.components.Gfx;
import btd.ui.components.TextButton;
import btd.ui.components.TowerArt;
import btd.ui.theme.Palette;
import btd.ui.theme.Theme;
import btd.util.SpriteLoader;

/**
 * [PURPOSE]       The game screen. It runs the game loop (Swing Timer + fixed 1/60 s steps), handles mouse and keys,
 *                 and draws everything: map, towers, zombies, shot effects, HUD, SidePanel and ShopOverlay.
 * [OOP / PATTERN] View + Controller (draws and handles input; the rules are in GameEngine / GameSession) /
 *                 Fixed-Timestep Game Loop / implements the SidePanel.Actions interface.
 * [CALLED BY]     Created in btd/ui/AppController.java (constructor); AppController.startRun() calls startRun() here.
 * [CONNECTS TO]   btd/core/GameEngine.java, btd/core/GameSession.java,
 *                 btd/core/GameMap.java, btd/ui/screens/SidePanel.java,
 *                 btd/ui/screens/ShopOverlay.java, btd/dao/PlayerDAO.java.
 * [DEFENSE NOTE]  SidePanel does not know GamePanel; it only knows the Actions interface. When a SidePanel button is
 *                 clicked it calls actions.upgradeSelected() etc., and GamePanel's version of that method runs.
 */
public class GamePanel extends JPanel implements SidePanel.Actions {

    private static final double STEP = 1.0 / 60.0;
    private static final int TOWER_SIZE = 34;
    // the tower art has empty space around it, so the image is drawn larger than the tower itself
    private static final int SPRITE_SIZE = GameMap.CELL * 2;

    private final Consumer<RunResult> onRunFinished;
    private final PlayerDAO playerDao = new PlayerDAO();
    private final SidePanel side = new SidePanel(this);
    private final TextButton quitButton = new TextButton("QUIT", 876, 8, 76, 36, Palette.BAD, 18f);
    private final Timer timer = new Timer(16, e -> tick());
    private BufferedImage background;

    private Player player;
    private Catalog catalog;
    private GameConfig config;
    private GameMap map;
    private GameSession session;
    private GameEngine engine;
    private ShopOverlay shop;

    private Tower selectedTower;
    private TowerType placing;
    private int mouseX;
    private int mouseY;
    private int speed = 1;

    private double accumulator;
    private long lastNanos;
    private double gameOverTimer;
    private boolean finished;
    private int lastClearedWave;
    private String toast = "";
    private double toastTimer;

    /**
     * [PURPOSE]       Sets up the panel once: size, mouse listeners, and the Esc / Space keys. No run is started yet.
     * [OOP / PATTERN] Constructor / Event Listeners (Observer: Swing calls us back on mouse events).
     * [CALLED BY]     btd/ui/AppController.java constructor.
     */
    public GamePanel(Consumer<RunResult> onRunFinished) {
        this.onRunFinished = onRunFinished;
        setPreferredSize(new Dimension(Theme.CONTENT_WIDTH, Theme.CONTENT_HEIGHT));
        setBackground(Palette.GRASS_A);

        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handlePress(e);
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                handleMove(e);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                handleMove(e);
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);

        bindKey("ESCAPE", this::cancelAction);
        bindKey("SPACE", this::startWave);
    }

    /**
     * [PURPOSE]       Makes a key (e.g. "SPACE") run an action, using Swing's InputMap / ActionMap.
     * [OOP / PATTERN] Swing Key Binding / the action is passed as a Runnable (this::startWave).
     * [CALLED BY]     GamePanel constructor.
     * [DEFENSE NOTE]  WHEN_IN_FOCUSED_WINDOW = the key works even if this panel itself does not have focus.
     */
    private void bindKey(String key, Runnable action) {
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key), key);
        getActionMap().put(key, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }

    /**
     * [PURPOSE]       Starts a fresh run: new GameSession, GameEngine and ShopOverlay, resets all fields, starts the Timer.
     * [OOP / PATTERN] Object Creation / Dependency Injection (passes config, map, session into the engine).
     * [CALLED BY]     btd/ui/AppController.java (startRun()), on Play and on PLAY AGAIN.
     * [CONNECTS TO]   btd/core/GameSession.java, btd/core/GameEngine.java, btd/ui/screens/ShopOverlay.java,
     *                 btd/ui/screens/SidePanel.java (bind()).
     * [DEFENSE NOTE]  Everything is created NEW here, so PLAY AGAIN never keeps coins or towers from the last run.
     *                 If assets/map/map.png is missing, background = null and paintGround()/paintPath() draw the map in code.
     */
    public void startRun(Player player, Catalog catalog, GameConfig config, GameMap map) {
        this.player = player;
        this.catalog = catalog;
        this.config = config;
        this.map = map;
        session = new GameSession(config, new CaseRNGManager());
        engine = new GameEngine(config, map, session);
        shop = new ShopOverlay(catalog, session);
        side.bind(session, engine);

        selectedTower = null;
        placing = null;
        speed = 1;
        accumulator = 0;
        gameOverTimer = 0;
        finished = false;
        lastClearedWave = 0;
        toast = "";
        toastTimer = 0;
        lastNanos = System.nanoTime();
        timer.start();
        showToast("Open the SHOP and use your free case!", 5);

        try {
            background = ImageIO.read(new File("assets/map/map.png"));
        } catch (IOException ex) {
            background = null; // falls back to the code-drawn map
}
    }

    // ---- game loop ----

    /**
     * [PURPOSE]       Runs about every 16 ms: measure real time since last tick, advance the game, then repaint.
     * [OOP / PATTERN] Game Loop / Timer Callback.
     * [CALLED BY]     The javax.swing.Timer field (new Timer(16, e -> tick())), on the Swing thread.
     * [DEFENSE NOTE]  The time is capped at 0.05 s, so if the PC freezes for a second the game does not jump ahead.
     */
    private void tick() {
        long now = System.nanoTime();
        double real = Math.min(0.05, (now - lastNanos) / 1e9);
        lastNanos = now;
        advance(real);
        repaint();
    }

    /**
     * [PURPOSE]       Turns real time into fixed 1/60 s engine steps. Also pauses for the shop and handles game over.
     * [OOP / PATTERN] Fixed Timestep with an Accumulator.
     * [CALLED BY]     tick().
     * [DEFENSE NOTE]  accumulator += real x speed; then "while (accumulator >= STEP) engine.update(STEP)".
     *                 So the game runs the same on fast and slow PCs, and at x2 / x3 speed it just does 2-3x more steps.
     *                 Shop open = the engine is not updated (paused). Game over = wait 2 s, then finishRun().
     */
    public void advance(double real) {
        if (toastTimer > 0) {
            toastTimer -= real;
        }
        if (shop.isOpen()) {
            shop.update(real); // the game is paused while the shop is open
            return;
        }
        if (engine.getState() == GameEngine.State.GAME_OVER) {
            gameOverTimer += real;
            if (gameOverTimer > 2.0) {
                finishRun();
            }
            return;
        }
        accumulator += real * speed;
        while (accumulator >= STEP) {
            engine.update(STEP);
            accumulator -= STEP;
        }
        if (engine.getWavesCleared() > lastClearedWave) {
            lastClearedWave = engine.getWavesCleared();
            showToast("Wave " + lastClearedWave + " cleared!  +" + engine.getLastWaveReward() + " coins", 3);
        }
        if (selectedTower != null && !session.getTowers().contains(selectedTower)) {
            selectTower(null);
        }
    }

    /** [PURPOSE] Shows a gold message at the top for some seconds ("Wave 3 cleared!", "BOSS WAVE!", "You can't build there."). */
    private void showToast(String text, double seconds) {
        toast = text;
        toastTimer = seconds;
    }

    /**
     * [PURPOSE]       Ends the run: stop the Timer, save the score if it is a new record, then tell AppController.
     * [OOP / PATTERN] Callback (onRunFinished is AppController::showResults) / DAO call.
     * [CALLED BY]     advance() (2 s after GAME OVER), confirmQuit() (QUIT button -> Yes).
     * [CONNECTS TO]   btd/dao/PlayerDAO.java (updateHighestScore()), btd/core/RunResult.java.
     * [DEFENSE NOTE]  'finished' stops it from running twice. "NEW HIGH SCORE" is only shown if the DB save worked.
     */
    private void finishRun() {
        if (finished) {
            return;
        }
        finished = true;
        timer.stop();
        int cleared = engine.getWavesCleared();
        int previousBest = player.getHighestScore();
        boolean record = false;
        if (cleared > previousBest) {
            try {
                playerDao.updateHighestScore(player.getPlayerId(), cleared);
                player.setHighestScore(cleared);
                record = true;
            } catch (SQLException | IllegalStateException ex) {
                // only report a new record once it is actually saved
                System.err.println("Could not save the score: " + ex.getMessage());
            }
        }
        onRunFinished.accept(new RunResult(cleared, engine.getKills(), session.getCasesOpened(), previousBest, record));
    }

    // ---- input ----

    /** [PURPOSE] Saves the mouse position (for the placing ghost) and updates hover effects on SidePanel, QUIT and the shop. */
    private void handleMove(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
        side.mouseMoved(mouseX, mouseY);
        quitButton.updateHover(mouseX, mouseY);
        if (shop != null) {
            shop.mouseMoved(mouseX, mouseY);
        }
    }

    /**
     * [PURPOSE] Decides where a click goes, in this order: shop (if open) -> SidePanel (x >= 960) ->
     *           right-click = cancel -> QUIT button -> place the tower (if placing) -> otherwise select a tower.
     */
    private void handlePress(MouseEvent e) {
        requestFocusInWindow();
        if (finished || engine.getState() == GameEngine.State.GAME_OVER) {
            return;
        }
        int x = e.getX();
        int y = e.getY();
        boolean right = javax.swing.SwingUtilities.isRightMouseButton(e);

        if (shop.isOpen()) {
            if (!right) {
                shop.click(x, y);
            }
            return;
        }
        if (x >= SidePanel.X) {
            side.click(x, y, right);
            return;
        }
        if (right) {
            cancelAction();
            return;
        }
        if (quitButton.contains(x, y)) {
            confirmQuit();
        } else if (placing != null) {
            placeAt(x, y);
        } else {
            selectTower(towerAt(x, y));
        }
    }

    /** [PURPOSE] Returns the placed tower under the mouse (within 21 px of its center), or null. */
    private Tower towerAt(int x, int y) {
        for (Tower t : session.getTowers()) {
            if (Math.hypot(t.getX() - x, t.getY() - y) <= TOWER_SIZE / 2.0 + 4) {
                return t;
            }
        }
        return null;
    }

    /**
     * [PURPOSE] Click on the map while placing: find that card in the inventory and call GameSession.place().
     *           If place() returns null (bad spot), show "You can't build there." and stay in placing mode.
     */
    private void placeAt(int x, int y) {
        List<TowerType> inventory = session.getInventory();
        int index = -1;
        for (int i = 0; i < inventory.size(); i++) {
            if (inventory.get(i).id() == placing.id()) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            setPlacing(null);
            return;
        }
        Tower placed = session.place(index, map, x, y);
        if (placed == null) {
            showToast("You can't build there.", 1.5);
            return;
        }
        setPlacing(null);
        selectTower(placed);
    }

    /** [PURPOSE] QUIT button: asks "End this run now?" (JOptionPane). Yes = finishRun(), which saves the score. */
    private void confirmQuit() {
        int answer = JOptionPane.showConfirmDialog(this, "End this run now?\nYour waves cleared so far will be saved.",
                "Quit run", JOptionPane.YES_NO_OPTION);
        if (answer == JOptionPane.YES_OPTION) {
            finishRun();
        }
    }

    /** [PURPOSE] Esc or right-click: close the shop if it is open, otherwise stop placing and deselect the tower. */
    private void cancelAction() {
        if (shop != null && shop.isOpen()) {
            shop.close();
        } else {
            setPlacing(null);
            selectTower(null);
        }
    }

    /** [PURPOSE] Selects a placed tower (or null = none) and tells SidePanel so it shows that tower's stats and buttons. */
    private void selectTower(Tower t) {
        selectedTower = t;
        side.setSelectedTower(t);
        if (t != null) {
            setPlacing(null);
        }
    }

    /** [PURPOSE] Enters placing mode for a tower type (null = leave it). While placing, the ghost and red grid are drawn. */
    private void setPlacing(TowerType type) {
        placing = type;
        side.setPlacing(type);
        if (type != null) {
            selectedTower = null;
            side.setSelectedTower(null);
        }
    }

    // ---- SidePanel.Actions ----
    // The 7 methods below are the SidePanel.Actions interface. SidePanel.click() calls them.

    /** [PURPOSE] Left-click an inventory card: start placing it, or stop if that card was already picked. */
    @Override
    public void selectInventory(TowerType type) {
        if (placing != null && placing.id() == type.id()) {
            setPlacing(null);
        } else {
            setPlacing(type);
        }
    }

    /** [PURPOSE] Right-click an inventory card: pops up "Sell 1 X for N coins". Clicking it calls GameSession.sellInventory(). */
    @Override
    public void sellInventory(TowerType type, int screenX, int screenY) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem item = new JMenuItem("Sell 1 " + type.name() + " for " + session.refundFor(type) + " coins");
        item.addActionListener(e -> {
            List<TowerType> inventory = session.getInventory();
            for (int i = 0; i < inventory.size(); i++) {
                if (inventory.get(i).id() == type.id()) {
                    session.sellInventory(i);
                    break;
                }
            }
            if (placing != null && session.copiesOf(placing) == 0) {
                setPlacing(null);
            }
        });
        menu.add(item);
        menu.show(this, screenX, screenY);
    }

    /** [PURPOSE] UPGRADE button: GameSession.upgrade() checks the rules and spends the spare copies. */
    @Override
    public void upgradeSelected() {
        if (selectedTower != null && session.upgrade(selectedTower)) {
            showToast(selectedTower.getType().name() + " is now Level " + selectedTower.getLevel() + "!", 2);
        }
    }

    /** [PURPOSE] SELL button: GameSession.sell() removes the selected tower and refunds coins. */
    @Override
    public void sellSelected() {
        if (selectedTower != null) {
            session.sell(selectedTower);
            selectTower(null);
        }
    }

    /** [PURPOSE] SHOP button: opens the case shop. While it is open, advance() pauses the game. */
    @Override
    public void openShop() {
        shop.open();
    }

    /** [PURPOSE] START WAVE button or Space: starts the next wave (not while the shop is open). Shows "BOSS WAVE!" on every 10th. */
    @Override
    public void startWave() {
        if (shop != null && !shop.isOpen() && engine.startNextWave() && engine.getWave() % config.scaling.bossEveryNthWave == 0) {
            showToast("BOSS WAVE!", 3);
        }
    }

    /** [PURPOSE] Speed button: 1x -> 2x -> 3x -> 1x. (speed % 3 + 1: 1 -> 2, 2 -> 3, 3 -> 1) */
    @Override
    public void cycleSpeed() {
        speed = speed % 3 + 1;
        side.setSpeed(speed);
    }

    // ---- painting ----

    /**
     * [PURPOSE] Swing calls this after every repaint(). Draw order = back to front:
     *           ground -> path -> red grid (placing) -> towers -> zombies -> shot effects -> ghost (placing)
     *           -> HUD -> banners -> SidePanel -> shop. Later things are drawn on top of earlier ones.
     */
    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        if (engine == null) {
            return;
        }
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        paintGround(g);
        paintPath(g);
        if (placing != null) {
            paintPlacementGrid(g);
        }
        paintTowers(g);
        for (Enemy e : engine.getEnemies()) {
            paintEnemy(g, e);
        }
        for (VisualEffect fx : engine.getEffects()) {
            paintEffect(g, fx);
        }
        if (placing != null) {
            paintGhost(g);
        }
        paintHud(g);
        paintBanners(g);
        side.paint(g);
        shop.paint(g);
    }

    /** [PURPOSE] Draws map.png; if it is missing, draws a grey checkerboard instead. */
    private void paintGround(Graphics2D g) {
        if (background != null) {
            g.drawImage(background, 0, 0, GameMap.WIDTH, GameMap.HEIGHT, null);
            return;
        }
        Color tileA = new Color(0x8E8E8E);
        Color tileB = new Color(0x838383);
        for (int cx = 0; cx < GameMap.WIDTH / GameMap.CELL; cx++) {
            for (int cy = 0; cy < GameMap.HEIGHT / GameMap.CELL; cy++) {
                g.setColor((cx + cy) % 2 == 0 ? tileA : tileB);
                g.fillRect(cx * GameMap.CELL, cy * GameMap.CELL, GameMap.CELL, GameMap.CELL);
            }
        }
    }

    /**
     * [PURPOSE] If map.png is missing, draws the roads in code (dark edge, asphalt, dashed lane line).
     *           Always draws the "ZOMBIES" label at the path start and "BASE" at the end.
     */
    private void paintPath(Graphics2D g) {
        List<double[]> points = map.getPoints();

        if (background == null) {
            Path2D.Double road = new Path2D.Double();
            road.moveTo(points.get(0)[0], points.get(0)[1]);
            for (int i = 1; i < points.size(); i++) {
                road.lineTo(points.get(i)[0], points.get(i)[1]);
            }
            for (double[] s : map.getExtraRoads()) {
                road.moveTo(s[0], s[1]);
                road.lineTo(s[2], s[3]);
        }

        g.setStroke(new BasicStroke(66f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(0x1B1B1B));
        g.draw(road);
        g.setStroke(new BasicStroke(56f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(0x2F2F2F));
        g.draw(road);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1f,
                new float[]{12f, 10f}, 0f));
        g.setColor(new Color(0xB8B8B8));
        g.draw(road);
    }

        double[] start = points.get(0);
        double[] end = points.get(points.size() - 1);
        Gfx.outlined(g, "ZOMBIES", start[0] + 6, start[1] - 36, Palette.heading(16f), Color.WHITE, Palette.PANEL_DARK);
        Gfx.outlined(g, "BASE", end[0] - 70, end[1] - 36, Palette.heading(20f), Palette.GOLD, Palette.PANEL_DARK);
    }

    /** [PURPOSE] While placing: tints every 40x40 cell red if its center is not buildable (GameSession.canPlaceAt()). */
    private void paintPlacementGrid(Graphics2D g) {
        for (int cx = 0; cx < GameMap.WIDTH / GameMap.CELL; cx++) {
            for (int cy = 0; cy < GameMap.HEIGHT / GameMap.CELL; cy++) {
                double x = cx * GameMap.CELL + GameMap.CELL / 2.0;
                double y = cy * GameMap.CELL + GameMap.CELL / 2.0;
                if (!session.canPlaceAt(map, x, y)) {
                    g.setColor(new Color(200, 40, 40, 40));
                    g.fillRect(cx * GameMap.CELL, cy * GameMap.CELL, GameMap.CELL, GameMap.CELL);
                }
            }
        }
    }

    /** [PURPOSE] While placing: draws the tower and its range circle under the mouse. White = OK, red = can't build. */
    private void paintGhost(Graphics2D g) {
        if (mouseX >= SidePanel.X) {
            return;
        }
        boolean ok = session.canPlaceAt(map, mouseX, mouseY);
        g.setColor(ok ? new Color(255, 255, 255, 60) : new Color(220, 40, 40, 70));
        double r = placing.range();
        g.fill(new Ellipse2D.Double(mouseX - r, mouseY - r, r * 2, r * 2));
        g.setColor(ok ? new Color(255, 255, 255, 200) : new Color(220, 40, 40, 220));
        g.setStroke(new BasicStroke(2f));
        g.draw(new Ellipse2D.Double(mouseX - r, mouseY - r, r * 2, r * 2));
        TowerArt.draw(g, placing, mouseX, mouseY, artSize(placing));
    }

    /** Size to draw the tower: 80 px (CELL x 2) if a PNG exists, else 34 px for the placeholder circle. */
    private static int artSize(TowerType type) {
        return SpriteLoader.get(type.spritePath()) != null ? SPRITE_SIZE : TOWER_SIZE;
    }

    /**
     * [PURPOSE] For each placed tower: range circle (if selected), Engineer turret dots, the tower art (TowerArt.draw),
     *           gold ring (if selected), and one dot per level underneath (gold dots at Lv 5).
     */
    private void paintTowers(Graphics2D g) {
        for (Tower t : session.getTowers()) {
            if (t == selectedTower) {
                double r = t.getRange();
                g.setColor(new Color(255, 255, 255, 55));
                g.fill(new Ellipse2D.Double(t.getX() - r, t.getY() - r, r * 2, r * 2));
                g.setColor(new Color(255, 255, 255, 210));
                g.setStroke(new BasicStroke(2f));
                g.draw(new Ellipse2D.Double(t.getX() - r, t.getY() - r, r * 2, r * 2));
            }
            if (t instanceof SupportTower support && support.getTurretCount() > 0) {
                int n = support.getTurretCount();
                for (int i = 0; i < n; i++) {
                    double[] p = support.turretPosition(i, n);
                    g.setColor(new Color(0x2B1B10));
                    g.fill(new Ellipse2D.Double(p[0] - 6, p[1] - 6, 12, 12));
                    g.setColor(Palette.GOLD);
                    g.fill(new Ellipse2D.Double(p[0] - 3.5, p[1] - 3.5, 7, 7));
                }
            }
            TowerArt.draw(g, t.getType(), t.getX(), t.getY(), artSize(t.getType()));
            if (t == selectedTower) {
                g.setColor(Palette.GOLD);
                g.setStroke(new BasicStroke(3f));
                g.draw(new Ellipse2D.Double(t.getX() - TOWER_SIZE / 2.0 - 3, t.getY() - TOWER_SIZE / 2.0 - 3,
                        TOWER_SIZE + 6, TOWER_SIZE + 6));
            }
            // level pips under the tower
            for (int i = 0; i < t.getLevel(); i++) {
                double px = t.getX() - (t.getLevel() - 1) * 3.5 + i * 7;
                g.setColor(t.getLevel() == Tower.MAX_LEVEL ? Palette.GOLD : Color.WHITE);
                g.fill(new Ellipse2D.Double(px - 2.5, t.getY() + TOWER_SIZE / 2.0 + 2, 5, 5));
                g.setColor(Palette.PANEL_DARK);
                g.setStroke(new BasicStroke(1f));
                g.draw(new Ellipse2D.Double(px - 2.5, t.getY() + TOWER_SIZE / 2.0 + 2, 5, 5));
            }
        }
    }

    /**
     * [PURPOSE] Draws one zombie: its sprite (left/right foot, rotated to walk direction) or a circle if no PNG,
     *           then colored rings (blue = stunned, purple = poisoned, orange = burning) and a health bar once hurt.
     */
    private void paintEnemy(Graphics2D g, Enemy e) {
        double r = e.getRadius();
        double x = e.getX();
        double y = e.getY();
        // swap feet every 12 pixels walked, e.g. assets/sprites/enemies/BasicEnemy_leftfoot.png
        String foot = (int) (e.getDistance() / 12) % 2 == 0 ? "_leftfoot" : "_rightfoot";
        Image sprite = SpriteLoader.get("assets/sprites/enemies/" + e.getTypeName() + foot + ".png");
        if (sprite != null) {
            // the sprite faces right, so turn it to the walking direction
            // the boss art fills more of its image, so it needs a smaller multiplier to fit the road
            double size = r * (e.getTypeName().equals("BossEnemy") ? 4 : 6);
            AffineTransform old = g.getTransform();
            g.rotate(e.getAngle(), x, y);
            g.drawImage(sprite, (int) (x - size / 2), (int) (y - size / 2), (int) size, (int) size, null);
            g.setTransform(old);
        } else {
            paintEnemyCircle(g, e, r, x, y);
        }

        // status rings
        g.setStroke(new BasicStroke(2.5f));
        if (e.isStunned()) {
            g.setColor(new Color(90, 160, 255));
            g.draw(new Ellipse2D.Double(x - r - 3, y - r - 3, r * 2 + 6, r * 2 + 6));
        }
        if (e.isPoisoned()) {
            g.setColor(new Color(170, 80, 230));
            g.draw(new Ellipse2D.Double(x - r - 5, y - r - 5, r * 2 + 10, r * 2 + 10));
        }
        if (e.isBurning()) {
            g.setColor(new Color(255, 120, 30));
            g.draw(new Ellipse2D.Double(x - r - 7, y - r - 7, r * 2 + 14, r * 2 + 14));
        }

        // health bar
        if (e.getHealth() < e.getMaxHealth()) {
            double w = r * 2 + 6;
            double frac = Math.max(0, e.getHealth() / e.getMaxHealth());
            g.setColor(Palette.PANEL_DARK);
            g.fillRect((int) (x - w / 2) - 1, (int) (y - r - 12), (int) w + 2, 6);
            g.setColor(frac > 0.5 ? Palette.GOOD : frac > 0.25 ? Palette.GOLD : Palette.BAD);
            g.fillRect((int) (x - w / 2), (int) (y - r - 11), (int) (w * frac), 4);
        }
    }

    /** The old placeholder look, used for zombie types that don't have a sprite yet. */
    private void paintEnemyCircle(Graphics2D g, Enemy e, double r, double x, double y) {
        Color body = switch (e.getTypeName()) {
            case "FastEnemy" -> new Color(0xB5D64B);
            case "ArmoredEnemy" -> new Color(0x6E8C7A);
            case "BossEnemy" -> new Color(0x7A2F4F);
            default -> new Color(0x5FAE4B);
        };
        g.setColor(new Color(0, 0, 0, 55));
        g.fill(new Ellipse2D.Double(x - r + 2, y - r + 4, r * 2, r * 2));
        g.setColor(body);
        g.fill(new Ellipse2D.Double(x - r, y - r, r * 2, r * 2));
        g.setColor(Palette.PANEL_DARK);
        g.setStroke(new BasicStroke(2f));
        g.draw(new Ellipse2D.Double(x - r, y - r, r * 2, r * 2));

        // eyes
        double eye = Math.max(2.5, r * 0.28);
        g.setColor(Color.WHITE);
        g.fill(new Ellipse2D.Double(x - r * 0.55, y - r * 0.4, eye * 2, eye * 2));
        g.fill(new Ellipse2D.Double(x + r * 0.55 - eye * 2, y - r * 0.4, eye * 2, eye * 2));
        g.setColor(Color.BLACK);
        g.fill(new Ellipse2D.Double(x - r * 0.55 + eye * 0.7, y - r * 0.4 + eye * 0.7, eye, eye));
        g.fill(new Ellipse2D.Double(x + r * 0.55 - eye * 1.3, y - r * 0.4 + eye * 0.7, eye, eye));

        if (e.getTypeName().equals("ArmoredEnemy")) {
            g.setColor(new Color(0x9AA5B1));
            g.fillArc((int) (x - r), (int) (y - r), (int) (r * 2), (int) (r * 2), 20, 140);
            g.setColor(Palette.PANEL_DARK);
            g.drawArc((int) (x - r), (int) (y - r), (int) (r * 2), (int) (r * 2), 20, 140);
        } else if (e.getTypeName().equals("BossEnemy")) {
            g.setColor(Palette.GOLD);
            g.fillPolygon(new int[]{(int) (x - r * 0.7), (int) (x - r * 0.4), (int) x, (int) (x + r * 0.4), (int) (x + r * 0.7)},
                    new int[]{(int) (y - r * 0.8), (int) (y - r * 1.3), (int) (y - r * 0.85), (int) (y - r * 1.3), (int) (y - r * 0.8)}, 5);
        }
    }

    /** [PURPOSE] Draws one VisualEffect: a line or a filled circle, with transparency from getFade() so it fades out. */
    private void paintEffect(Graphics2D g, VisualEffect fx) {
        float fade = (float) fx.getFade();
        Color c = fx.getColor();
        if (fx.getKind() == VisualEffect.Kind.LINE) {
            g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (255 * fade)));
            g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine((int) fx.getX1(), (int) fx.getY1(), (int) fx.getX2(), (int) fx.getY2());
        } else {
            double r = fx.getX2();
            g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (90 * fade)));
            g.fill(new Ellipse2D.Double(fx.getX1() - r, fx.getY1() - r, r * 2, r * 2));
            g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (220 * fade)));
            g.setStroke(new BasicStroke(2f));
            g.draw(new Ellipse2D.Double(fx.getX1() - r, fx.getY1() - r, r * 2, r * 2));
        }
    }

    /** [PURPOSE] Top bar: LIVES (turns red at 5 or less), COINS, WAVE ("n left" during a wave), and the QUIT button. */
    private void paintHud(Graphics2D g) {
        pill(g, 12, "LIVES  " + session.getLives(), session.getLives() <= 5 ? Palette.BAD : Palette.GOOD, 160);
        pill(g, 184, "COINS  " + session.getCoins(), new Color(0xC99700), 190);
        String waveText = engine.getState() == GameEngine.State.WAVE_ACTIVE
                ? "WAVE " + engine.getWave() + "  (" + engine.getEnemiesLeft() + " left)"
                : "WAVE " + (engine.getWave() + 1) + " next";
        pill(g, 386, waveText, new Color(0x4A90D9), 260);
        quitButton.draw(g);
    }

    /** [PURPOSE] Helper rendering rounded status pill with drop shadow and centered label. */
    private void pill(Graphics2D g, int x, String text, Color color, int width) {
        g.setColor(new Color(0, 0, 0, 80));
        g.fillRoundRect(x, 11, width, 36, 18, 18);
        g.setColor(color);
        g.fillRoundRect(x, 8, width, 36, 18, 18);
        g.setColor(Palette.PANEL_DARK);
        g.setStroke(new BasicStroke(3f));
        g.drawRoundRect(x, 8, width, 36, 18, 18);
        Gfx.outlinedCentered(g, text, x + width / 2.0, 34, Palette.heading(20f), Color.WHITE, Palette.PANEL_DARK);
    }

    /** [PURPOSE] Draws "GAME OVER" (dark screen), the "press START WAVE" hint between waves, and the current toast message. */
    private void paintBanners(Graphics2D g) {
        if (engine.getState() == GameEngine.State.GAME_OVER) {
            g.setColor(new Color(0, 0, 0, 140));
            g.fillRect(0, 0, GameMap.WIDTH, GameMap.HEIGHT);
            Gfx.outlinedCentered(g, "GAME OVER", GameMap.WIDTH / 2.0, 360, Palette.heading(80f), Palette.BAD, Palette.PANEL_DARK);
            return;
        }
        if (engine.getState() == GameEngine.State.BETWEEN_WAVES && toastTimer <= 0) {
            Gfx.outlinedCentered(g, "Place towers, then press START WAVE (or Space)",
                    GameMap.WIDTH / 2.0, 700, Palette.heading(22f), Color.WHITE, Palette.PANEL_DARK);
        }
        if (toastTimer > 0) {
            Gfx.outlinedCentered(g, toast, GameMap.WIDTH / 2.0, 100, Palette.heading(32f), Palette.GOLD, Palette.PANEL_DARK);
        }
    }
}
