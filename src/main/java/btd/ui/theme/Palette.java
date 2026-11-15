package btd.ui.theme;

import btd.util.FontLoader;

import java.awt.Color;
import java.awt.Font;

/**
 * [PURPOSE]       Color palette and typography definitions for the in-game HUD, side panels, and shop overlays.
 * [OOP / PATTERN] Constants Class / Design Tokens / Aesthetic Encapsulation.
 * [CALLED BY]     btd/ui/screens/SidePanel.java, btd/ui/screens/GamePanel.java,
 *                 btd/ui/screens/ShopOverlay.java, btd/ui/screens/ResultsPanel.java, btd/ui/components/TextButton.java.
 * [CONNECTS TO]   btd/util/FontLoader.java.
 * [DEFENSE NOTE]  Centralizes warm cartoon wood/cream palette to preserve visual consistency across all UI components.
 */
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

    /**
     * [PURPOSE]       Derives the Luckiest Guy display font at requested point size for titles and buttons.
     * [OOP / PATTERN] Static Factory / Delegation.
     * [CALLED BY]     btd/ui/screens/SidePanel.java, btd/ui/components/TextButton.java.
     * [CONNECTS TO]   btd/util/FontLoader.java.
     */
    public static Font heading(float size) {
        return FontLoader.getFont(size);
    }

    /**
     * [PURPOSE]       Returns a clean bold system sans-serif font for small stat and tooltip descriptions.
     * [OOP / PATTERN] Static Factory / Fallback Typography.
     * [CALLED BY]     btd/ui/screens/SidePanel.java, btd/ui/screens/ShopOverlay.java.
     */
    public static Font body(float size) {
        return new Font(Font.SANS_SERIF, Font.BOLD, 1).deriveFont(size);
    }
}
