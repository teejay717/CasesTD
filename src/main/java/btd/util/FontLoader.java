package btd.util;

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.File;
import java.io.IOException;

/** Loads the shared UI font from assets/fonts. Run from the project root. */
public final class FontLoader {

    private static final Font BASE_FONT = loadBaseFont();

    private FontLoader() {
    }

    /** Returns the shared font at the requested size in points. */
    public static Font getFont(float size) {
        return BASE_FONT.deriveFont(size);
    }

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
