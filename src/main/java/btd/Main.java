package btd;

import btd.ui.AppController;
import btd.ui.MainFrame;

import javax.swing.SwingUtilities;

/**
 * [PURPOSE]       Application entry point and desktop launch bootstrap for CasesTD.
 * [OOP / PATTERN] Entry Point (the only main() the game needs).
 * [CALLED BY]     The JVM when you run the game. Run it from the project root folder, because files like
 *                 data/db_config.properties and assets/... are opened with relative paths.
 * [CONNECTS TO]   btd/ui/MainFrame.java (creates the window),
 *                 btd/ui/AppController.java (creates the screens and switches between them).
 * [DEFENSE NOTE]  Nothing is loaded from the database here. The DB is first used when the player opens the crate
 *                 (PlayerDAO), and towers/cases are loaded when Play is pressed (AppController.loadData()).
 */
public class Main {

    /**
     * [PURPOSE]       Launches the Swing GUI on the Event Dispatch Thread (EDT).
     * [OOP / PATTERN] Concurrency / Thread Safety.
     * [CALLED BY]     Operating System / JVM runtime.
     * [CONNECTS TO]   btd/ui/MainFrame.java, btd/ui/AppController.java.
     * [DEFENSE NOTE]  Swing rule: build and change the UI only on the Event Dispatch Thread (EDT).
     *                 invokeLater() puts the lambda on that thread instead of running it on the main thread.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            new AppController(frame);
            frame.setVisible(true);
        });
    }
}
