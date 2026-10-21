package btd;

import btd.ui.MainFrame;
import btd.ui.screens.PlaceholderPanel;

import javax.swing.SwingUtilities;

/** Application entry point. */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.addScreen("placeholder", new PlaceholderPanel());
            frame.showScreen("placeholder");
            frame.setVisible(true);
        });
    }
}
