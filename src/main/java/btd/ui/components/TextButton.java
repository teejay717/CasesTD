package btd.ui.components;

import btd.ui.theme.Palette;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 * [PURPOSE]       A button drawn with code (no image): rounded box, shadow, outlined text, greys out when disabled.
 * [OOP / PATTERN] Custom UI Component / Encapsulation.
 *                 Not a JButton: the screen calls draw(), contains() and updateHover() itself.
 * [CALLED BY]     btd/ui/screens/SidePanel.java, btd/ui/screens/ShopOverlay.java, btd/ui/screens/GamePanel.java,
 *                 btd/ui/screens/ResultsPanel.java.
 * [CONNECTS TO]   btd/ui/theme/Palette.java, btd/ui/components/Gfx.java.
 * [DEFENSE NOTE]  If the text is too wide, the font shrinks 1 pt at a time until it fits (minimum 9 pt).
 */
public class TextButton {

    private final Rectangle bounds;
    private String text;
    private final Color color;
    private final float fontSize;
    private boolean enabled = true;
    private boolean hover;

    /**
     * [PURPOSE]       Constructs rounded vector button with geometry, base color, and initial font size.
     * [OOP / PATTERN] Constructor / State Initialization.
     * [CALLED BY]     Field initializers in SidePanel, ShopOverlay, GamePanel and ResultsPanel.
     */
    public TextButton(String text, int x, int y, int width, int height, Color color, float fontSize) {
        this.text = text;
        this.bounds = new Rectangle(x, y, width, height);
        this.color = color;
        this.fontSize = fontSize;
    }

    /** [PURPOSE] Updates label text string. */
    public void setText(String text) { this.text = text; }
    /** [PURPOSE] Sets interactive enabled state (greys out button when disabled). */
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    /** [PURPOSE] Queries interactive enabled state. */
    public boolean isEnabled() { return enabled; }
    /** [PURPOSE] Accessor for button bounding rectangle. */
    public Rectangle getBounds() { return bounds; }

    /** [PURPOSE] Repositions button location on canvas. */
    public void setLocation(int x, int y) {
        bounds.setLocation(x, y);
    }

    /** [PURPOSE] Hit-test checking if cursor coordinate (x, y) falls inside button bounds. */
    public boolean contains(int x, int y) {
        return bounds.contains(x, y);
    }

    /** [PURPOSE] Updates hover status (only active if enabled is true). */
    public void updateHover(int x, int y) {
        hover = enabled && bounds.contains(x, y);
    }

    /**
     * [PURPOSE]       Renders vector rounded rectangle with drop shadow, border stroke, and centered outlined text.
     * [OOP / PATTERN] Custom Component Drawing.
     * [CALLED BY]     The paint methods of the screens that own the button.
     * [CONNECTS TO]   btd/ui/components/Gfx.java (outlinedCentered()).
     */
    public void draw(Graphics2D g) {
        Color fill = enabled ? color : new Color(0x9A9A9A);
        if (hover) {
            fill = fill.brighter();
        }
        g.setColor(new Color(0, 0, 0, 70));
        g.fillRoundRect(bounds.x, bounds.y + 3, bounds.width, bounds.height, 16, 16);
        g.setColor(fill);
        g.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 16, 16);
        g.setColor(Palette.PANEL_DARK);
        g.setStroke(new BasicStroke(3f));
        g.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 16, 16);

        float size = fontSize;
        Font font = Palette.heading(size);
        g.setFont(font);
        while (g.getFontMetrics().stringWidth(text) > bounds.width - 14 && size > 9) {
            size -= 1;
            font = Palette.heading(size);
            g.setFont(font);
        }
        double baseline = bounds.y + bounds.height / 2.0 + g.getFontMetrics().getAscent() / 2.0 - 3;
        Gfx.outlinedCentered(g, text, bounds.getCenterX(), baseline, font,
                enabled ? Color.WHITE : new Color(0xE5E5E5), Palette.PANEL_DARK);
    }
}
