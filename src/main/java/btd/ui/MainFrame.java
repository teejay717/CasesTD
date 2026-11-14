package btd.ui;

import btd.ui.theme.Theme;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.CardLayout;
import java.awt.Dimension;

/**
 * [PURPOSE]       Top-level desktop window (JFrame) hosting all game views using a CardLayout manager.
 * [OOP / PATTERN] Top-Level Container / Composite Pattern / View Host.
 * [CALLED BY]     btd/Main.java (main()).
 * [CONNECTS TO]   btd/ui/AppController.java, btd/ui/theme/Theme.java.
 * [DEFENSE NOTE]  Fixed 1280x720 canvas dimensions ensure consistent UI positioning across different operating systems.
 */
public class MainFrame extends JFrame {

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel screenPanel = new JPanel(cardLayout);

    /**
     * [PURPOSE]       Initializes top-level application window with title, non-resizable borders, and centered positioning.
     * [OOP / PATTERN] Constructor / Window Initialization.
     * [CALLED BY]     btd/Main.java (main()).
     * [CONNECTS TO]   btd/ui/theme/Theme.java (CONTENT_WIDTH, CONTENT_HEIGHT).
     */
    public MainFrame() {
        super("CasesTD");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        screenPanel.setPreferredSize(new Dimension(Theme.CONTENT_WIDTH, Theme.CONTENT_HEIGHT));
        screenPanel.setBackground(Theme.BACKGROUND_COLOR);
        setContentPane(screenPanel);

        pack();
        setLocationRelativeTo(null);
    }

    /**
     * [PURPOSE]       Registers a screen panel with the internal CardLayout manager under a unique card identifier.
     * [OOP / PATTERN] Registry Pattern / Adapter.
     * [CALLED BY]     btd/ui/AppController.java constructor.
     */
    public void addScreen(String name, JPanel panel) {
        screenPanel.add(panel, name);
    }

    /**
     * [PURPOSE]       Flips active view to the registered card matching name string.
     * [OOP / PATTERN] CardLayout Switching / State Display.
     * [CALLED BY]     btd/ui/AppController.java.
     */
    public void showScreen(String name) {
        cardLayout.show(screenPanel, name);
    }
}
