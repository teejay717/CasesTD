package btd.util;

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.File;
import java.io.IOException;

/**
 * [PURPOSE]       Loads the 'LuckiestGuy-Regular.ttf' font file once; falls back to SansSerif if it fails.
 * [OOP / PATTERN] Utility Class / Load Once (static final field filled when the class loads).
 * [CALLED BY]     btd/ui/theme/Palette.java (heading()), btd/ui/theme/Theme.java (TITLE_FONT, BUTTON_FONT).
 * [CONNECTS TO]   assets/fonts/LuckiestGuy-Regular.ttf.
 * [DEFENSE NOTE]  A missing font prints a warning (System.err) instead of crashing.
 *                 getFont() only calls deriveFont(size), so the file is read from disk just once.
 */
public final class FontLoader {

    private static final Font BASE_FONT = loadBaseFont();

    private FontLoader() {
    }

    /**
     * [PURPOSE]       Derives the loaded base font at the specified floating point size.
     * [OOP / PATTERN] Factory Method / Immutable Font Derivation.
     * [CALLED BY]     btd/ui/theme/Palette.java (heading()), btd/ui/theme/Theme.java.
     * [DEFENSE NOTE]  Font objects are immutable, so deriveFont() returns a NEW font and BASE_FONT never changes.
     */
    public static Font getFont(float size) {
        return BASE_FONT.deriveFont(size);
    }

    /** [PURPOSE] Reads the .ttf file, or returns SansSerif 12 pt if the file is missing or broken. Runs once. */
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
