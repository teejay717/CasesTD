package btd.ui.components;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

/**
 * [PURPOSE]       Static drawing utilities for rendered text: center alignment, high-contrast drop outlines, and word wrapping.
 * [OOP / PATTERN] Utility Class / Pure Fabrication / DRY Principle.
 * [CALLED BY]     btd/ui/components/TextButton.java, btd/ui/screens/SidePanel.java,
 *                 btd/ui/screens/GamePanel.java, btd/ui/screens/ShopOverlay.java.
 * [CONNECTS TO]   java.awt.Graphics2D, java.awt.FontMetrics.
 * [DEFENSE NOTE]  Outlined text ensures readability over both bright grass and dark UI panels.
 */
public final class Gfx {

    private Gfx() {
    }

    /**
     * [PURPOSE]       Draws text centered horizontally at centerX with baseline at baselineY.
     * [OOP / PATTERN] Typography Helper.
     * [CALLED BY]     UI panels.
     */
    public static void centered(Graphics2D g, String text, double centerX, double baselineY, Font font, Color color) {
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        g.setColor(color);
        g.drawString(text, (float) (centerX - fm.stringWidth(text) / 2.0), (float) baselineY);
    }

    /**
     * [PURPOSE]       Renders text with a 2-pixel dark border outline using an 8-way directional offset stamp.
     * [OOP / PATTERN] Graphic Filter / Outline Effect.
     * [CALLED BY]     btd/ui/screens/GamePanel.java, btd/ui/components/TextButton.java.
     * [DEFENSE NOTE]  Draws 8 surrounding outline stamps before drawing the center fill text.
     */
    public static void outlined(Graphics2D g, String text, double x, double y, Font font, Color fill, Color outline) {
        g.setFont(font);
        g.setColor(outline);
        for (int dx = -2; dx <= 2; dx += 2) {
            for (int dy = -2; dy <= 2; dy += 2) {
                if (dx != 0 || dy != 0) {
                    g.drawString(text, (float) (x + dx), (float) (y + dy));
                }
            }
        }
        g.setColor(fill);
        g.drawString(text, (float) x, (float) y);
    }

    /**
     * [PURPOSE]       Combines outline effect with horizontal center alignment.
     * [OOP / PATTERN] Composite Utility Method.
     * [CALLED BY]     btd/ui/components/TextButton.java, btd/ui/screens/SidePanel.java.
     */
    public static void outlinedCentered(Graphics2D g, String text, double centerX, double y, Font font,
                                        Color fill, Color outline) {
        g.setFont(font);
        double width = g.getFontMetrics().stringWidth(text);
        outlined(g, text, centerX - width / 2.0, y, font, fill, outline);
    }

    /**
     * [PURPOSE]       Splits multi-word prose into wrapped lines that fit within maxWidth pixels.
     * [OOP / PATTERN] Greedy Word Wrap Algorithm.
     * [CALLED BY]     btd/ui/screens/SidePanel.java, btd/ui/screens/ShopOverlay.java.
     * [CONNECTS TO]   java.awt.FontMetrics.stringWidth().
     */
    public static List<String> wrap(Graphics2D g, String text, Font font, int maxWidth) {
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String test = line.length() == 0 ? word : line + " " + word;
            if (fm.stringWidth(test) > maxWidth && line.length() > 0) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(test);
            }
        }
        if (line.length() > 0) {
            lines.add(line.toString());
        }
        return lines;
    }
}
