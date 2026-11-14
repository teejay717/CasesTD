package btd;

import btd.ui.AppController;
import btd.ui.MainFrame;

import javax.swing.SwingUtilities;

/**
 * [PURPOSE]       Application entry point and desktop launch bootstrap for CasesTD.
 * [OOP / PATTERN] Bootstrap Pattern / Facade Initialization.
 * [CALLED BY]     JVM at application launch (must be executed with project root as working directory).
 * [CONNECTS TO]   btd/ui/MainFrame.java (instantiates top-level window),
 *                 btd/ui/AppController.java (binds card layout navigation).
 * [DEFENSE NOTE]  Dispatches GUI initialization onto Swing's Event Dispatch Thread (EDT)
 *                 via SwingUtilities.invokeLater() to ensure thread-safe component construction.
 */
public class Main {

    /**
     * [PURPOSE]       Launches the Swing GUI on the Event Dispatch Thread (EDT).
     * [OOP / PATTERN] Concurrency / Thread Safety.
     * [CALLED BY]     Operating System / JVM runtime.
     * [CONNECTS TO]   btd/ui/MainFrame.java, btd/ui/AppController.java.
     * [DEFENSE NOTE]  Never construct Swing UI elements on the main thread; invokeLater()
     *                 prevents deadlocks and race conditions during component tree initialization.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            new AppController(frame);
            frame.setVisible(true);
        });
    }
}
