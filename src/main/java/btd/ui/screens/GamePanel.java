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
 * [PURPOSE]       Primary gameplay canvas running the 60 FPS fixed-timestep loop: renders procedural and bitmap
 *                 map terrain, rotates walking zombies, handles tower targeting lasers/explosions, manages tower placement,
 *                 and hosts the HUD, SidePanel, and ShopOverlay.
 * [OOP / PATTERN] Master View & Input Controller (MVC) / Mediator Pattern / Fixed Timestep Game Loop Pattern.
 * [CALLED BY]     btd/ui/AppController.java (startRun()).
 * [CONNECTS TO]   btd/core/GameEngine.java, btd/core/GameSession.java,
 *                 btd/core/GameMap.java, btd/ui/screens/SidePanel.java,
 *                 btd/ui/screens/ShopOverlay.java, btd/dao/PlayerDAO.java.
 * [DEFENSE NOTE]  Implements SidePanel.Actions to decouple UI sidebar button clicks from core game simulation logic.
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
     * [PURPOSE]       Constructs GamePanel, attaches mouse listeners, and binds ESC/SPACE keyboard shortcuts.
     * [OOP / PATTERN] Constructor / Input Binding / Observer Pattern.
     * [CALLED BY]     btd/ui/AppController.java constructor.
     * [CONNECTS TO]   handlePress(), handleMove(), cancelAction(), startWave().
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
     * [PURPOSE]       Binds keyboard shortcut key to executable action runnable using Swing InputMap/ActionMap.
     * [OOP / PATTERN] Command Pattern / Swing Key Binding.
     * [CALLED BY]     GamePanel constructor.
     * [CONNECTS TO]   javax.swing.InputMap, javax.swing.ActionMap.
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
     * [PURPOSE]       Initializes new gameplay session, loads map background, starts 60 FPS Swing Timer.
     * [OOP / PATTERN] Lifecycle Hook / Session Hydration.
     * [CALLED BY]     btd/ui/AppController.java (startRun()).
     * [CONNECTS TO]   btd/core/GameSession.java, btd/core/GameEngine.java, btd/ui/screens/SidePanel.java.
     * [DEFENSE NOTE]  Loads map background asset 'assets/map/map.png' with safe procedural fallback.
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
     * [PURPOSE]       Swing Timer tick handler: computes elapsed delta time, advances game simulation, triggers repaint.
     * [OOP / PATTERN] Game Loop / Observer Callback / Delta Time Measurement.
     * [CALLED BY]     javax.swing.Timer (timer.start() in GamePanel.startRun()).
     * [CONNECTS TO]   advance(), repaint().
     * [DEFENSE NOTE]  Delta Time (real) math:
     *                 1. (now - lastNanos) / 1e9 converts nanoseconds elapsed into fractional seconds (≈ 0.0166s at 60 FPS).
     *                 2. Math.min(0.05, ...) clamps delta to 50ms (spiral-of-death prevention) so window dragging or
     *                    OS background lag spikes don't queue hundreds of catch-up ticks.
     */
    private void tick() {
        long now = System.nanoTime();
        double real = Math.min(0.05, (now - lastNanos) / 1e9);
        lastNanos = now;
        advance(real);
        repaint();
    }

    /**
     * [PURPOSE]       Fixed-timestep simulation tick accumulator: runs GameEngine updates at 60 steps per second.
     * [OOP / PATTERN] Fixed Timestep Accumulator Pattern.
     * [CALLED BY]     tick().
     * [CONNECTS TO]   btd/ui/screens/ShopOverlay.java (update()), btd/core/GameEngine.java (update()),
     *                 finishRun(), showToast(), selectTower().
     * [DEFENSE NOTE]  Fixed-Timestep Accumulator (STEP = 1.0 / 60.0):
     *                 Variable frame times (real) are accumulated. The engine updates ONLY in discrete 1/60s slices
     *                 (while accumulator >= STEP). This guarantees 100% deterministic physics, prevents waypoint tunneling,
     *                 and completely decouples simulation consistency from monitor refresh rate (60Hz vs 144Hz).
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

    /**
     * [PURPOSE]       Displays temporary overlay banner message for notifications, wave clears, or illegal placement alerts.
     * [OOP / PATTERN] Transient State / UI Notification.
     * [CALLED BY]     startRun(), advance(), placeAt(), upgradeSelected(), startWave().
     * [CONNECTS TO]   paintBanners().
     */
    private void showToast(String text, double seconds) {
        toast = text;
        toastTimer = seconds;
    }

    /**
     * [PURPOSE]       Concludes run session, updates player highest score in DB if beaten, triggers onRunFinished callback.
     * [OOP / PATTERN] Lifecycle Hook / Data Access Synchronization.
     * [CALLED BY]     advance() (on GAME_OVER), confirmQuit() (on confirmed quit dialog).
     * [CONNECTS TO]   btd/dao/PlayerDAO.java (updateHighestScore()), btd/ui/AppController.java (via onRunFinished callback).
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

    /**
     * [PURPOSE]       Tracks cursor coordinates and forwards hover updates to SidePanel, quit button, and ShopOverlay.
     * [OOP / PATTERN] Event Handler / Delegator Pattern.
     * [CALLED BY]     GamePanel constructor (MouseAdapter.mouseMoved(), mouseDragged()).
     * [CONNECTS TO]   btd/ui/screens/SidePanel.java (mouseMoved()), btd/ui/screens/ShopOverlay.java (mouseMoved()),
     *                 btd/ui/components/TextButton.java (updateHover()).
     */
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
     * [PURPOSE]       Dispatches mouse press events: shop interactions, sidebar clicks, cancel, quit modal, or tower placing/selection.
     * [OOP / PATTERN] Input Router / Command Dispatcher Pattern.
     * [CALLED BY]     GamePanel constructor (MouseAdapter.mousePressed()).
     * [CONNECTS TO]   btd/ui/screens/ShopOverlay.java (click()), btd/ui/screens/SidePanel.java (click()),
     *                 confirmQuit(), placeAt(), selectTower(), cancelAction().
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

    /**
     * [PURPOSE]       Radial hit-testing on placed towers around coordinates (x, y).
     * [OOP / PATTERN] Spatial Hit-Testing / Query Helper.
     * [CALLED BY]     handlePress().
     * [CONNECTS TO]   btd/core/GameSession.java (getTowers()).
     */
    private Tower towerAt(int x, int y) {
        for (Tower t : session.getTowers()) {
            if (Math.hypot(t.getX() - x, t.getY() - y) <= TOWER_SIZE / 2.0 + 4) {
                return t;
            }
        }
        return null;
    }

    /**
     * [PURPOSE]       Places pending inventory tower onto map cell if valid path/distance constraints pass.
     * [OOP / PATTERN] Action Execution / State Mutation.
     * [CALLED BY]     handlePress().
     * [CONNECTS TO]   btd/core/GameSession.java (place()), btd/core/GameMap.java, showToast(), selectTower(), setPlacing().
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

    /**
     * [PURPOSE]       Shows Swing confirmation dialog before quitting active run and saving score.
     * [OOP / PATTERN] Modal Dialog / Confirmation Gate.
     * [CALLED BY]     handlePress().
     * [CONNECTS TO]   javax.swing.JOptionPane, finishRun().
     */
    private void confirmQuit() {
        int answer = JOptionPane.showConfirmDialog(this, "End this run now?\nYour waves cleared so far will be saved.",
                "Quit run", JOptionPane.YES_NO_OPTION);
        if (answer == JOptionPane.YES_OPTION) {
            finishRun();
        }
    }

    /**
     * [PURPOSE]       Cancels active tower placement, closes shop overlay, or clears tower selection.
     * [OOP / PATTERN] State Reset / Command Pattern.
     * [CALLED BY]     GamePanel constructor (bindKey ESCAPE), handlePress() (right-click).
     * [CONNECTS TO]   btd/ui/screens/ShopOverlay.java (close()), setPlacing(), selectTower().
     */
    private void cancelAction() {
        if (shop != null && shop.isOpen()) {
            shop.close();
        } else {
            setPlacing(null);
            selectTower(null);
        }
    }

    /**
     * [PURPOSE]       Selects clicked tower on map and updates SidePanel to display stats/upgrades.
     * [OOP / PATTERN] Selection State Management / Observer Notification.
     * [CALLED BY]     handlePress(), placeAt(), advance(), sellSelected(), cancelAction().
     * [CONNECTS TO]   btd/ui/screens/SidePanel.java (setSelectedTower()), setPlacing().
     */
    private void selectTower(Tower t) {
        selectedTower = t;
        side.setSelectedTower(t);
        if (t != null) {
            setPlacing(null);
        }
    }

    /**
     * [PURPOSE]       Sets pending tower placement mode with ghost preview.
     * [OOP / PATTERN] Mode Toggle / Mutually Exclusive State.
     * [CALLED BY]     selectTower(), placeAt(), cancelAction(), selectInventory(), sellInventory().
     * [CONNECTS TO]   btd/ui/screens/SidePanel.java (setPlacing(), setSelectedTower()).
     */
    private void setPlacing(TowerType type) {
        placing = type;
        side.setPlacing(type);
        if (type != null) {
            selectedTower = null;
            side.setSelectedTower(null);
        }
    }

    // ---- SidePanel.Actions ----

    /**
     * [PURPOSE]       Selects or deselects inventory tower type for map placement.
     * [OOP / PATTERN] Callback Implementation (SidePanel.Actions) / State Toggle.
     * [CALLED BY]     btd/ui/screens/SidePanel.java (click()).
     * [CONNECTS TO]   setPlacing().
     */
    @Override
    public void selectInventory(TowerType type) {
        if (placing != null && placing.id() == type.id()) {
            setPlacing(null);
        } else {
            setPlacing(type);
        }
    }

    /**
     * [PURPOSE]       Displays right-click context menu allowing player to sell spare inventory copies for coins.
     * [OOP / PATTERN] Context Menu / Callback Implementation (SidePanel.Actions).
     * [CALLED BY]     btd/ui/screens/SidePanel.java (click() on right-click).
     * [CONNECTS TO]   btd/core/GameSession.java (sellInventory(), refundFor()).
     */
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

    /**
     * [PURPOSE]       Upgrades currently selected tower level if player has required card copies.
     * [OOP / PATTERN] Callback Implementation (SidePanel.Actions) / State Mutation.
     * [CALLED BY]     btd/ui/screens/SidePanel.java (click() on UPGRADE button).
     * [CONNECTS TO]   btd/core/GameSession.java (upgrade()), showToast().
     */
    @Override
    public void upgradeSelected() {
        if (selectedTower != null && session.upgrade(selectedTower)) {
            showToast(selectedTower.getType().name() + " is now Level " + selectedTower.getLevel() + "!", 2);
        }
    }

    /**
     * [PURPOSE]       Sells currently selected tower on map, refunding coins to session.
     * [OOP / PATTERN] Callback Implementation (SidePanel.Actions).
     * [CALLED BY]     btd/ui/screens/SidePanel.java (click() on SELL button).
     * [CONNECTS TO]   btd/core/GameSession.java (sell()), selectTower().
     */
    @Override
    public void sellSelected() {
        if (selectedTower != null) {
            session.sell(selectedTower);
            selectTower(null);
        }
    }

    /**
     * [PURPOSE]       Opens gacha crate shop modal overlay, pausing real-time game simulation.
     * [OOP / PATTERN] Callback Implementation (SidePanel.Actions).
     * [CALLED BY]     btd/ui/screens/SidePanel.java (click() on SHOP button).
     * [CONNECTS TO]   btd/ui/screens/ShopOverlay.java (open()).
     */
    @Override
    public void openShop() {
        shop.open();
    }

    /**
     * [PURPOSE]       Triggers next combat wave spawn; shows BOSS WAVE banner if boss wave index.
     * [OOP / PATTERN] Callback Implementation (SidePanel.Actions) / Command Pattern.
     * [CALLED BY]     GamePanel constructor (bindKey SPACE), btd/ui/screens/SidePanel.java (click() on START WAVE button).
     * [CONNECTS TO]   btd/core/GameEngine.java (startNextWave()), showToast().
     */
    @Override
    public void startWave() {
        if (shop != null && !shop.isOpen() && engine.startNextWave() && engine.getWave() % config.scaling.bossEveryNthWave == 0) {
            showToast("BOSS WAVE!", 3);
        }
    }

    /**
     * [PURPOSE]       Cycles game simulation speed: 1x -> 2x -> 3x -> 1x.
     * [OOP / PATTERN] Callback Implementation (SidePanel.Actions) / State Cycler.
     * [CALLED BY]     btd/ui/screens/SidePanel.java (click() on speed button).
     * [CONNECTS TO]   btd/ui/screens/SidePanel.java (setSpeed()).
     */
    @Override
    public void cycleSpeed() {
        speed = speed % 3 + 1;
        side.setSpeed(speed);
    }

    // ---- painting ----

    /**
     * [PURPOSE]       Master paint method: renders map terrain, paths, grid, towers, enemies, attack effects, HUD, and overlays.
     * [OOP / PATTERN] Template Method / Swing Paint Pipeline.
     * [CALLED BY]     javax.swing.RepaintManager / Swing EDT (via repaint()).
     * [CONNECTS TO]   paintGround(), paintPath(), paintPlacementGrid(), paintTowers(), paintEnemy(), paintEffect(),
     *                 paintGhost(), paintHud(), paintBanners(), btd/ui/screens/SidePanel.java (paint()), btd/ui/screens/ShopOverlay.java (paint()).
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

    /**
     * [PURPOSE]       Draws custom map background image or checkered procedural grass fallback.
     * [OOP / PATTERN] Fallback Rendering Strategy.
     * [CALLED BY]     paintComponent().
     * [CONNECTS TO]   btd/core/GameMap.java (WIDTH, HEIGHT).
     */
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
     * [PURPOSE]       Draws asphalt paths and lane markings if procedural, plus ZOMBIES spawn and BASE labels.
     * [OOP / PATTERN] Procedural Vector Drawing.
     * [CALLED BY]     paintComponent().
     * [CONNECTS TO]   btd/core/GameMap.java (getPoints(), getExtraRoads()), btd/ui/components/Gfx.java (outlined()).
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

    /**
     * [PURPOSE]       Draws red semi-transparent overlay highlights on unbuildable map tiles when placing a tower.
     * [OOP / PATTERN] Spatial Grid Visualizer.
     * [CALLED BY]     paintComponent().
     * [CONNECTS TO]   btd/core/GameSession.java (canPlaceAt()), btd/core/GameMap.java.
     */
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

    /**
     * [PURPOSE]       Draws translucent range circle and tower sprite tracking cursor in placement mode.
     * [OOP / PATTERN] Visual Feedback / Ghost Preview.
     * [CALLED BY]     paintComponent().
     * [CONNECTS TO]   btd/core/GameSession.java (canPlaceAt()), btd/ui/components/TowerArt.java (draw()), artSize().
     */
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

    /**
     * [PURPOSE]       Determines draw size: padded 80 px (SPRITE_SIZE) for PNG sprites vs 34 px (TOWER_SIZE) for placeholder vector.
     * [OOP / PATTERN] Adapter Helper.
     * [CALLED BY]     paintGhost(), paintTowers().
     * [CONNECTS TO]   btd/util/SpriteLoader.java (get()).
     */
    private static int artSize(TowerType type) {
        return SpriteLoader.get(type.spritePath()) != null ? SPRITE_SIZE : TOWER_SIZE;
    }

    /**
     * [PURPOSE]       Draws all active placed towers, support turrets, range rings, selection halos, and level pips.
     * [OOP / PATTERN] Entity List Renderer / Pattern Matching instanceof.
     * [CALLED BY]     paintComponent().
     * [CONNECTS TO]   btd/core/GameSession.java (getTowers()), btd/model/tower/SupportTower.java,
     *                 btd/ui/components/TowerArt.java (draw()), artSize().
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
     * [PURPOSE]       Renders walking zombie sprites facing travel angle, status rings, and health bars.
     * [OOP / PATTERN] Sprite Transformation / Animation Renderer.
     * [CALLED BY]     paintComponent().
     * [CONNECTS TO]   btd/model/enemy/Enemy.java, btd/util/SpriteLoader.java (get()), paintEnemyCircle().
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

    /**
     * [PURPOSE]       Procedural vector zombie drawing with eyes, armor plate, or boss crown for units without PNG sprites.
     * [OOP / PATTERN] Procedural Fallback Strategy.
     * [CALLED BY]     paintEnemy().
     * [CONNECTS TO]   btd/model/enemy/Enemy.java.
     */
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

    /**
     * [PURPOSE]       Renders attack animation: alpha-fading line for lasers or expanding circle for splash blast waves.
     * [OOP / PATTERN] Visual FX Renderer.
     * [CALLED BY]     paintComponent().
     * [CONNECTS TO]   btd/core/VisualEffect.java.
     */
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

    /**
     * [PURPOSE]       Renders top HUD status pills: LIVES, COINS, WAVE PROGRESS, and QUIT button.
     * [OOP / PATTERN] HUD Overlay Renderer.
     * [CALLED BY]     paintComponent().
     * [CONNECTS TO]   pill(), btd/core/GameSession.java, btd/core/GameEngine.java, btd/ui/components/TextButton.java.
     */
    private void paintHud(Graphics2D g) {
        pill(g, 12, "LIVES  " + session.getLives(), session.getLives() <= 5 ? Palette.BAD : Palette.GOOD, 160);
        pill(g, 184, "COINS  " + session.getCoins(), new Color(0xC99700), 190);
        String waveText = engine.getState() == GameEngine.State.WAVE_ACTIVE
                ? "WAVE " + engine.getWave() + "  (" + engine.getEnemiesLeft() + " left)"
                : "WAVE " + (engine.getWave() + 1) + " next";
        pill(g, 386, waveText, new Color(0x4A90D9), 260);
        quitButton.draw(g);
    }

    /**
     * [PURPOSE]       Helper rendering rounded status pill with drop shadow and centered label.
     * [OOP / PATTERN] GUI Primitive Renderer.
     * [CALLED BY]     paintHud().
     * [CONNECTS TO]   btd/ui/components/Gfx.java (outlinedCentered()).
     */
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

    /**
     * [PURPOSE]       Renders GAME OVER banner screen dimming overlay, placement instruction, and gold toast announcements.
     * [OOP / PATTERN] Overlay Banner Renderer.
     * [CALLED BY]     paintComponent().
     * [CONNECTS TO]   btd/core/GameEngine.java (getState()), btd/ui/components/Gfx.java (outlinedCentered()).
     */
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
