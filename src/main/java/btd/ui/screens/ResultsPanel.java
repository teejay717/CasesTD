package btd.ui.screens;

import btd.core.RunResult;
import btd.ui.components.Gfx;
import btd.ui.components.TextButton;
import btd.ui.theme.Palette;
import btd.ui.theme.Theme;
import btd.util.ImageLoader;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * [PURPOSE]       The "RUN OVER" screen: waves cleared, zombies defeated, cases opened, and new record or old best.
 * [OOP / PATTERN] Inheritance (extends JPanel) / Callbacks (onPlayAgain, onMenu are Runnables from AppController).
 * [CALLED BY]     btd/ui/AppController.java (constructor creates it; showResults() calls show()).
 * [CONNECTS TO]   btd/core/RunResult.java, btd/ui/components/TextButton.java,
 *                 btd/ui/components/Gfx.java, btd/ui/theme/Palette.java, btd/util/ImageLoader.java.
 * [DEFENSE NOTE]  It only shows data; it never reads the DB. The score was already saved by GamePanel.finishRun().
 *                 "NEW HIGH SCORE!" in green if result.newRecord(), otherwise "Highest score: X".
 */
public class ResultsPanel extends JPanel {

    private final Image background = ImageLoader.load("bg_menu.png");
    private final TextButton playAgain = new TextButton("PLAY AGAIN", 380, 520, 240, 70, Palette.GOOD, 30f);
    private final TextButton menu = new TextButton("MENU", 660, 520, 240, 70, Theme.ORANGE_COLOR, 30f);

    private RunResult result;
    private String playerName = "";

    /**
     * [PURPOSE]       Adds the click handler (PLAY AGAIN -> onPlayAgain, MENU -> onMenu) and the hover handler.
     * [OOP / PATTERN] Constructor / Event Listeners / Callbacks.
     * [CALLED BY]     btd/ui/AppController.java constructor.
     */
    public ResultsPanel(Runnable onPlayAgain, Runnable onMenu) {
        setPreferredSize(new Dimension(Theme.CONTENT_WIDTH, Theme.CONTENT_HEIGHT));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (playAgain.contains(e.getX(), e.getY())) {
                    onPlayAgain.run();
                } else if (menu.contains(e.getX(), e.getY())) {
                    onMenu.run();
                }
            }
        });
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                playAgain.updateHover(e.getX(), e.getY());
                menu.updateHover(e.getX(), e.getY());
                repaint();
            }
        });
    }

    /**
     * [PURPOSE]       Stores the run's result and the player's name, then repaints.
     * [OOP / PATTERN] Setter + repaint().
     * [CALLED BY]     btd/ui/AppController.java (showResults()).
     * [CONNECTS TO]   btd/core/RunResult.java.
     */
    public void show(RunResult result, String playerName) {
        this.result = result;
        this.playerName = playerName;
        repaint();
    }

    /**
     * [PURPOSE]       Draws the background, a cream card, the stats from RunResult, and the two buttons.
     * [OOP / PATTERN] Method Overriding (JPanel.paintComponent).
     * [CALLED BY]     Swing, whenever the panel needs drawing (after repaint()).
     * [CONNECTS TO]   btd/ui/components/Gfx.java (outlinedCentered()).
     */
    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        if (background != null) {
            g.drawImage(background, 0, 0, Theme.CONTENT_WIDTH, Theme.CONTENT_HEIGHT, this);
        } else {
            g.setColor(Theme.BACKGROUND_COLOR);
            g.fillRect(0, 0, Theme.CONTENT_WIDTH, Theme.CONTENT_HEIGHT);
        }
        if (result == null) {
            return;
        }

        g.setColor(new Color(0, 0, 0, 90));
        g.fillRoundRect(340, 60, 600, 600, 40, 40);
        g.setColor(Palette.CREAM);
        g.fillRoundRect(350, 50, 580, 600, 40, 40);
        g.setColor(Palette.PANEL_DARK);
        g.setStroke(new BasicStroke(6f));
        g.drawRoundRect(350, 50, 580, 600, 40, 40);

        Gfx.outlinedCentered(g, "RUN OVER", 640, 135, Palette.heading(64f), Palette.BAD, Palette.PANEL_DARK);
        Gfx.centered(g, playerName, 640, 175, Palette.body(20f), Palette.PANEL_DARK);

        Gfx.centered(g, "WAVES CLEARED", 640, 245, Palette.heading(28f), Palette.PANEL_DARK);
        Gfx.outlinedCentered(g, String.valueOf(result.wavesCleared()), 640, 340, Palette.heading(100f), Palette.GOLD, Palette.PANEL_DARK);

        if (result.newRecord()) {
            Gfx.outlinedCentered(g, "NEW HIGH SCORE!", 640, 395, Palette.heading(34f), Palette.GOOD, Palette.PANEL_DARK);
        } else {
            Gfx.centered(g, "Highest score: " + result.previousBest(), 640, 395, Palette.body(20f), Palette.PANEL_DARK);
        }

        Gfx.centered(g, "Zombies defeated: " + result.kills(), 640, 450, Palette.body(22f), Palette.PANEL_DARK);
        Gfx.centered(g, "Cases opened: " + result.casesOpened(), 640, 485, Palette.body(22f), Palette.PANEL_DARK);

        playAgain.draw(g);
        menu.draw(g);
    }
}
