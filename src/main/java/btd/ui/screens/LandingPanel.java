package btd.ui.screens;

import btd.dao.PlayerDAO;
import btd.model.player.Player;
import btd.ui.components.GameButton;
import btd.ui.theme.Theme;
import btd.util.ImageLoader;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;

/**
 * [PURPOSE]       Two-stage landing screen: Screen 1 authenticates player username via crate opening,
 *                 Screen 2 displays high score, play button, profile pill, and tutorial trigger.
 * [OOP / PATTERN] State Machine View Pattern / Composite Component.
 * [CALLED BY]     btd/ui/AppController.java.
 * [CONNECTS TO]   btd/dao/PlayerDAO.java, btd/model/player/Player.java,
 *                 btd/ui/screens/HelpPanel.java, btd/ui/components/GameButton.java.
 * [DEFENSE NOTE]  Opening the crate calls PlayerDAO.findOrCreate(), creating a seamless account login/registration flow.
 */
public class LandingPanel extends JPanel {

    // Images (null if a file is missing)
    private final Image bgStart = ImageLoader.load("bg_start.png");
    private final Image bgMenu = ImageLoader.load("bg_menu.png");
    private final Image crateImage = ImageLoader.load("panel_crate.png");
    private final Image pillImage = ImageLoader.load("btn_pill_orange.png");
    private final Image creamBox = ImageLoader.load("label_cream.png");
    private final Image scoreBox = ImageLoader.load("label_score_blank.png");
    private final Image highestScoreText = ImageLoader.load("text_highest_score.png");
    private final Image playText = ImageLoader.load("text_play.png");
    private final Image profileIcon = ImageLoader.load("icon_profile.png");
    private final Image logo = ImageLoader.load("casesdt_logo.png");
    private final Image clickToOpen = ImageLoader.load("click_to_open.png");

    // Clickable buttons: size, then position
    private final GameButton quitButton = new GameButton.Builder(90, 92)
            .position(1165, 16).icon(ImageLoader.load("btn_close_x.png")).build();
    private final GameButton crateButton = new GameButton.Builder(699, 330)
            .position(291, 337).icon(ImageLoader.load("panel_crate.png")).build();
    private final GameButton playButton = new GameButton.Builder(209, 209)
            .position(536, 130).icon(ImageLoader.load("btn_play.png")).build();
    private final GameButton helpButton = new GameButton.Builder(94, 98)
            .position(832, 273).icon(ImageLoader.load("icon_help.png")).build();
    private final GameButton howToPlayButton = new GameButton.Builder(147, 58)
            .position(800, 370).icon(ImageLoader.load("btn_how_to_play.png")).build();

    private final JTextField nameField = new JTextField();
    private final HelpPanel helpPanel = new HelpPanel();
    private final PlayerDAO playerDao = new PlayerDAO();
    private final Runnable onPlay;

    private Player player;
    private boolean opened = false;

    /**
     * [PURPOSE]       Constructs LandingPanel, embeds custom name text input, help tutorial overlay, and mouse handlers.
     * [OOP / PATTERN] Constructor / Component Setup.
     * [CALLED BY]     btd/ui/AppController.java constructor.
     */
    public LandingPanel(Runnable onPlay) {
        this.onPlay = onPlay;
        setPreferredSize(new Dimension(Theme.CONTENT_WIDTH, Theme.CONTENT_HEIGHT));
        setLayout(null);

        // Name field sits on top of the cream box, so it has no background or border of its own
        nameField.setBounds(552, 218, 176, 44);
        nameField.setOpaque(false);
        nameField.setBorder(null);
        nameField.setHorizontalAlignment(SwingConstants.CENTER);
        nameField.setFont(new Font(Font.MONOSPACED, Font.BOLD, 20));
        nameField.setForeground(new Color(130, 40, 30));
        add(nameField);

        helpPanel.setBounds(240, 110, 800, 500);
        add(helpPanel);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int x = e.getX();
                int y = e.getY();
                if (quitButton.contains(x, y)) {
                    System.exit(0);
                } else if (!opened && crateButton.contains(x, y) && !getPlayerName().isEmpty()) {
                    openCrate();
                } else if (opened && playButton.contains(x, y)) {
                    onPlay.run();
                } else if (opened && (helpButton.contains(x, y) || howToPlayButton.contains(x, y))) {
                    helpPanel.setVisible(!helpPanel.isVisible());
                }
            }
        });
    }

    /**
     * [PURPOSE]       Authenticates/registers user profile via PlayerDAO and transitions view state to Screen 2.
     * [OOP / PATTERN] State Transition / DAO Delegation.
     * [CALLED BY]     mouseClicked() handler when crateButton clicked.
     * [CONNECTS TO]   btd/dao/PlayerDAO.java (findOrCreate()).
     * [DEFENSE NOTE]  Enforces 50-character maximum length limit before querying database.
     */
    private void openCrate() {
        String name = getPlayerName();
        if (name.length() > 50) {
            JOptionPane.showMessageDialog(this, "Name must be 50 characters or less.");
            return;
        }
        try {
            player = playerDao.findOrCreate(name);
        } catch (SQLException | IllegalStateException ex) {
            JOptionPane.showMessageDialog(this, "Could not load your profile: " + ex.getMessage());
            return;
        }
        opened = true;
        nameField.setVisible(false);
        repaint();
    }

    /** [PURPOSE] Active authenticated Player profile object. */
    public Player getPlayer() {
        return player;
    }

    /** [PURPOSE] Retrieves sanitized trimmed string from the username input text field. */
    public String getPlayerName() {
        return nameField.getText().trim();
    }

    /**
     * [PURPOSE]       Main render entry point switching between Start Screen and Menu Screen based on opened state.
     * [OOP / PATTERN] Rendering Pipeline / View State Dispatch.
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        Image bg = opened ? bgMenu : bgStart;
        g2.drawImage(bg, 0, 0, Theme.CONTENT_WIDTH, Theme.CONTENT_HEIGHT, this); 

        if (opened) {
            paintMenuScreen(g2);
        } else {
            paintStartScreen(g2);
        }
        quitButton.draw(g2);
    }

    /** [PURPOSE] Renders Screen 1: CasesTD logo, name prompt text, cream text input box, and closed crate button. */
    private void paintStartScreen(Graphics2D g2) {
        drawCropped(g2, logo, 137, 260, 318, 75, 418, 40, 459, 108);

        drawText(g2, "type your name here...", 640, 205, new Font(Font.MONOSPACED, Font.BOLD, 16),
                new Color(150, 40, 30), null);
        g2.drawImage(creamBox, 538, 213, 204, 58, this);

        crateButton.draw(g2);

        drawCropped(g2, clickToOpen, 129, 280, 342, 45, 490, 285, 300, 40);
    }

    /** [PURPOSE] Renders Screen 2: High score badge, play button, player avatar pill, and how-to-play trigger. */
    private void paintMenuScreen(Graphics2D g2) {
        drawCropped(g2, logo, 137, 260, 318, 75, 30, 22, 170, 40);

        g2.drawImage(highestScoreText, 445, 26, 82, 45, this);
        g2.drawImage(scoreBox, 538, 18, 204, 58, this);
        drawText(g2, String.valueOf(player.getHighestScore()), 640, 61,
                Theme.BUTTON_FONT.deriveFont(30f), Theme.YELLOW_COLOR, Color.BLACK);

        g2.drawImage(crateImage, 293, 450, 695, 328, this);

        playButton.draw(g2);
        g2.drawImage(pillImage, 550, 340, 181, 66, this);
        g2.drawImage(playText, 604, 352, 73, 30, this);

        g2.drawImage(profileIcon, 365, 273, 94, 98, this);
        g2.drawImage(pillImage, 334, 372, 147, 54, this);
        String name = player.getPlayerName();
        if (name.length() > 14) {
            name = name.substring(0, 12) + "...";
        }
        drawText(g2, name, 407, 403, new Font(Font.MONOSPACED, Font.BOLD, 16), new Color(130, 40, 30), null);

        helpButton.draw(g2);
        howToPlayButton.draw(g2);
    }

    /** [PURPOSE] Sub-image cropping utility clipping transparent borders from 600x600 icon assets. */
    private void drawCropped(Graphics2D g2, Image img, int sx, int sy, int sw, int sh,
                             int dx, int dy, int dw, int dh) {
        g2.drawImage(img, dx, dy, dx + dw, dy + dh, sx, sy, sx + sw, sy + sh, this);
    }

    /** [PURPOSE] Draws center-aligned text with optional 8-way drop shadow outline. */
    private void drawText(Graphics2D g2, String text, int centerX, int baselineY, Font font, Color fill, Color outline) {
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();
        int x = centerX - fm.stringWidth(text) / 2;
        if (outline != null) {
            g2.setColor(outline);
            for (int dx = -2; dx <= 2; dx += 2) {
                for (int dy = -2; dy <= 2; dy += 2) {
                    g2.drawString(text, x + dx, baselineY + dy);
                }
            }
        }
        g2.setColor(fill);
        g2.drawString(text, x, baselineY);
    }
}
