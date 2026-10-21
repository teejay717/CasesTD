package btd.ui;

import btd.ui.theme.Theme;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.CardLayout;
import java.awt.Dimension;

/** Main window that holds the game screens. Create and use on Swing's event thread. */
public class MainFrame extends JFrame {

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel screenPanel = new JPanel(cardLayout);

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

    /** Registers a panel under a name such as "landing" or "placeholder". */
    public void addScreen(String name, JPanel panel) {
        screenPanel.add(panel, name);
    }

    /** Shows a screen that has already been registered. */
    public void showScreen(String name) {
        cardLayout.show(screenPanel, name);
    }
}
