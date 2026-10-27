package btd.ui.components;

import btd.ui.theme.Palette;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/** A simple rounded button with a text label, used on the game screens. */
public class TextButton {

    private final Rectangle bounds;
    private String text;
    private final Color color;
    private final float fontSize;
    private boolean enabled = true;
    private boolean hover;

    public TextButton(String text, int x, int y, int width, int height, Color color, float fontSize) {
        this.text = text;
        this.bounds = new Rectangle(x, y, width, height);
        this.color = color;
        this.fontSize = fontSize;
    }

    public void setText(String text) { this.text = text; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isEnabled() { return enabled; }
    public Rectangle getBounds() { return bounds; }

    public void setLocation(int x, int y) {
        bounds.setLocation(x, y);
    }

    public boolean contains(int x, int y) {
        return bounds.contains(x, y);
    }

    public void updateHover(int x, int y) {
        hover = enabled && bounds.contains(x, y);
    }

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
