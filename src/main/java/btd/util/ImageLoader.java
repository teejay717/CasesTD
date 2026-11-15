package btd.util;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.io.File;
import java.io.IOException;

/**
 * [PURPOSE]       Loads menu pictures (buttons, icons, backgrounds) from the 'assets/ui/' folder.
 * [OOP / PATTERN] Utility Class (final + private constructor).
 * [CALLED BY]     btd/ui/screens/LandingPanel.java, btd/ui/screens/HelpPanel.java,
 *                 btd/ui/screens/ResultsPanel.java.
 * [CONNECTS TO]   assets/ui/ directory.
 * [DEFENSE NOTE]  Returns null (and prints a warning) instead of crashing if a file is missing.
 *                 Unlike SpriteLoader it has NO cache, because each screen loads its images once into fields.
 *                 Tower and zombie sprites use SpriteLoader instead.
 */
public final class ImageLoader {

    private ImageLoader() {
    }

    /**
     * [PURPOSE]       Reads image file from 'assets/ui/' directory using standard ImageIO.
     * [OOP / PATTERN] Static Factory Method.
     * [CALLED BY]     Field initializers of LandingPanel and ResultsPanel, and the HelpPanel constructor.
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
