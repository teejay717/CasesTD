package btd.util;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * [PURPOSE]       Loads and caches tower and enemy sprite images from relative filesystem paths.
 * [OOP / PATTERN] Flyweight Pattern / In-Memory Cache / Lazy Loading.
 * [CALLED BY]     btd/ui/components/TowerArt.java (draw()), btd/ui/screens/GamePanel.java (paintEnemy()).
 * [CONNECTS TO]   btd/model/tower/TowerType.java (spritePath).
 * [DEFENSE NOTE]  Caches loaded images in a static HashMap to eliminate redundant disk reads on every 60 FPS frame.
 */
public final class SpriteLoader {

    private static final Map<String, Image> CACHE = new HashMap<>();

    private SpriteLoader() {
    }

    /**
     * [PURPOSE]       Retrieves cached sprite Image or loads from disk if first requested.
     * [OOP / PATTERN] Lazy Cache Lookup (Flyweight Pattern).
     * [CALLED BY]     btd/ui/components/TowerArt.java, btd/ui/screens/GamePanel.java.
     * [CONNECTS TO]   javax.imageio.ImageIO.
     * [DEFENSE NOTE]  Returns null if file does not exist, triggering procedural fallback shapes in TowerArt.
     */
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
