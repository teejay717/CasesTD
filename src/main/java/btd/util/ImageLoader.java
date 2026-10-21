package btd.util;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.io.File;
import java.io.IOException;

/** Loads UI images from assets/ui. Run from the project root. */
public final class ImageLoader {

    private ImageLoader() {
    }

    /** Returns the image, or null (with a message) if it can't be read. */
    public static Image load(String fileName) {
        try {
            return ImageIO.read(new File("assets/ui/" + fileName));
        } catch (IOException e) {
            System.err.println("Could not load " + fileName + ": " + e.getMessage());
            return null;
        }
    }
}
