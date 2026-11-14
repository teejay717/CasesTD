package btd.util;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.io.File;
import java.io.IOException;

/**
 * [PURPOSE]       Loads UI bitmap graphic assets (buttons, icons, backgrounds) from the 'assets/ui/' folder.
 * [OOP / PATTERN] Static Utility / Resource Loader.
 * [CALLED BY]     btd/ui/screens/LandingPanel.java, btd/ui/screens/HelpPanel.java,
 *                 btd/ui/screens/ResultsPanel.java.
 * [CONNECTS TO]   assets/ui/ directory.
 * [DEFENSE NOTE]  Catches IOException and logs error instead of throwing, allowing partial UI rendering.
 */
public final class ImageLoader {

    private ImageLoader() {
    }

    /**
     * [PURPOSE]       Reads image file from 'assets/ui/' directory using standard ImageIO.
     * [OOP / PATTERN] Static Factory Method.
     * [CALLED BY]     Screen panels during initialization.
     * [CONNECTS TO]   javax.imageio.ImageIO.
     */
    public static Image load(String fileName) {
        try {
            return ImageIO.read(new File("assets/ui/" + fileName));
        } catch (IOException e) {
            System.err.println("Could not load " + fileName + ": " + e.getMessage());
            return null;
        }
    }
}
