package btd.ui.theme;

import btd.util.FontLoader;

import java.awt.Color;
import java.awt.Font;

/**
 * [PURPOSE]       Window size (1280x720) plus a few shared colors and fonts.
 * [OOP / PATTERN] Constants Class (final class, public static final fields, private constructor).
 * [CALLED BY]     btd/ui/MainFrame.java, btd/ui/screens/LandingPanel.java, btd/ui/screens/ResultsPanel.java,
 *                 btd/ui/screens/GamePanel.java, btd/ui/screens/SidePanel.java, btd/ui/screens/ShopOverlay.java.
 * [CONNECTS TO]   btd/util/FontLoader.java.
 * [DEFENSE NOTE]  1280x720 is 16:9. The game map uses the left 960 px; SidePanel uses the right 320 px.
 */
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
