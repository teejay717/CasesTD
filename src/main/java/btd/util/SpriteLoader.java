package btd.util;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads tower sprites by the sprite_path stored in the database. If the file does not exist yet
 * it returns null and the game draws a placeholder shape instead, so art can be added later
 * without changing any code.
 */
public final class SpriteLoader {

    private static final Map<String, Image> CACHE = new HashMap<>();

    private SpriteLoader() {
    }

    public static Image get(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        if (CACHE.containsKey(path)) {
            return CACHE.get(path);
        }
        Image image = null;
        File file = new File(path);
        if (file.isFile()) {
            try {
                image = ImageIO.read(file);
            } catch (IOException e) {
                System.err.println("Could not read sprite " + path + ": " + e.getMessage());
            }
        }
        CACHE.put(path, image);
        return image;
    }
}
