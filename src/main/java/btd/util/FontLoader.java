package btd.util;

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.File;
import java.io.IOException;

/**
 * [PURPOSE]       Loads the custom TrueType font 'LuckiestGuy-Regular.ttf' from disk with graceful fallback to SansSerif.
 * [OOP / PATTERN] Resource Loader / Static Factory / Singleton Font Cache.
 * [CALLED BY]     btd/ui/theme/Palette.java, btd/ui/theme/Theme.java.
 * [CONNECTS TO]   assets/fonts/LuckiestGuy-Regular.ttf.
 * [DEFENSE NOTE]  Prevents crash if font asset is missing by logging warning and falling back to system font.
 */
public final class FontLoader {

    private static final Font BASE_FONT = loadBaseFont();

    private FontLoader() {
    }

    /**
     * [PURPOSE]       Derives the loaded base font at the specified floating point size.
     * [OOP / PATTERN] Factory Method / Immutable Font Derivation.
     * [CALLED BY]     btd/ui/theme/Palette.java (heading()), btd/ui/theme/Theme.java.
     */
    public static Font getFont(float size) {
        return BASE_FONT.deriveFont(size);
    }

    /** [PURPOSE] Reads TrueType font file from filesystem or defaults to SansSerif plain 12pt. */
    private static Font loadBaseFont() {
        try {
            return Font.createFont(Font.TRUETYPE_FONT,
                    new File("assets/fonts/LuckiestGuy-Regular.ttf"));
        } catch (FontFormatException | IOException e) {
            System.err.println("Could not load Luckiest Guy; using SansSerif: " + e.getMessage());
            return new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        }
    }
}
