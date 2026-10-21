package btd.ui.theme;

import btd.util.FontLoader;

import java.awt.Color;
import java.awt.Font;

/** Shared content size, colors, and fonts for the game UI. */
public final class Theme {

    public static final int CONTENT_WIDTH = 1280;
    public static final int CONTENT_HEIGHT = 720;

    public static final Color BACKGROUND_COLOR = new Color(0x5CE1E6);
    public static final Color ORANGE_COLOR = new Color(0xFFAA00);
    public static final Color YELLOW_COLOR = new Color(0xFFD43B);
    public static final Color TEXT_COLOR = new Color(0x222222);

    public static final Font TITLE_FONT = FontLoader.getFont(48f);
    public static final Font BUTTON_FONT = FontLoader.getFont(28f);

    private Theme() {
    }
}
