package btd.ui.theme;

import btd.util.FontLoader;

import java.awt.Color;
import java.awt.Font;

/** Colors and fonts used by the in-game screens (map, side panel, shop). */
public final class Palette {

    public static final Color GRASS_A = new Color(0x8ED081);
    public static final Color GRASS_B = new Color(0x86C979);
    public static final Color PATH_EDGE = new Color(0xA67C52);
    public static final Color PATH_FILL = new Color(0xEBDDB5);

    public static final Color PANEL = new Color(0x4A3424);
    public static final Color PANEL_DARK = new Color(0x2B1B10);
    public static final Color CREAM = new Color(0xFFF1D0);
    public static final Color CREAM_DARK = new Color(0xE9D3A2);

    public static final Color GOOD = new Color(0x3FAE49);
    public static final Color BAD = new Color(0xD64545);
    public static final Color GOLD = new Color(0xFFC933);

    private Palette() {
    }

    /** The Luckiest Guy display font, for titles and buttons. */
    public static Font heading(float size) {
        return FontLoader.getFont(size);
    }

    /** A plain bold font that stays readable at small sizes. */
    public static Font body(float size) {
        return new Font(Font.SANS_SERIF, Font.BOLD, 1).deriveFont(size);
    }
}
