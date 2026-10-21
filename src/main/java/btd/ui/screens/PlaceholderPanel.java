package btd.ui.screens;

import btd.ui.theme.Theme;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;

/** Temporary screen shown until the game screen is ready. */
public class PlaceholderPanel extends JPanel {

    public PlaceholderPanel() {
        super(new BorderLayout());
        setBackground(Theme.BACKGROUND_COLOR);

        JLabel message = new JLabel("Game screen coming soon", SwingConstants.CENTER);
        message.setFont(Theme.TITLE_FONT);
        message.setForeground(Theme.TEXT_COLOR);
        add(message, BorderLayout.CENTER);
    }
}
