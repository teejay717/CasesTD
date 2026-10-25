package btd;

import btd.ui.AppController;
import btd.ui.MainFrame;

import javax.swing.SwingUtilities;

/** Application entry point. Run from the project root so the assets/ and data/ folders are found. */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            new AppController(frame);
            frame.setVisible(true);
        });
    }
}
