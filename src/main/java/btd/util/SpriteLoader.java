package btd.util;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * [PURPOSE]       Loads tower and zombie PNGs from disk and remembers them, so each file is read only once.
 * [OOP / PATTERN] Cache (static HashMap: path -> Image) / Lazy Loading (only loaded the first time it is asked for).
 * [CALLED BY]     btd/ui/components/TowerArt.java (draw()), btd/ui/screens/GamePanel.java (paintEnemy(), artSize()).
 * [CONNECTS TO]   btd/model/tower/TowerType.java (spritePath, from the DB), assets/sprites/...
 * [DEFENSE NOTE]  Drawing happens ~60 times a second. Without the cache the game would read the PNG from disk
 *                 every frame for every tower and zombie.
 */
public final class SpriteLoader {

    private static final Map<String, Image> CACHE = new HashMap<>();

    private SpriteLoader() {
    }

    /**
     * [PURPOSE]       Returns the image for this path: from the cache if loaded before, otherwise from disk (ImageIO.read).
     * [OOP / PATTERN] Lazy Cache Lookup.
     * [CALLED BY]     btd/ui/components/TowerArt.java, btd/ui/screens/GamePanel.java.
     * [CONNECTS TO]   javax.imageio.ImageIO.
     * [DEFENSE NOTE]  Returns null if the file is missing; the caller then draws a placeholder
     *                 (TowerArt: colored circle, GamePanel: paintEnemyCircle()).
     *                 A missing file is ALSO cached (as null), so the game doesn't keep checking the disk for it.
     */
    public static Image get(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        if (CACHE.containsKey(path)) {
            return CACHE.get(path); // Checks if sprite is loaded
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
