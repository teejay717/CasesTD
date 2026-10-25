package btd.ui.components;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

/** Small text-drawing helpers shared by the game screens. */
public final class Gfx {

    private Gfx() {
    }

    /** Draws text centered on centerX. */
    public static void centered(Graphics2D g, String text, double centerX, double baselineY, Font font, Color color) {
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        g.setColor(color);
        g.drawString(text, (float) (centerX - fm.stringWidth(text) / 2.0), (float) baselineY);
    }

    /** Draws text with a dark outline so it reads on any background. */
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

    public static void outlinedCentered(Graphics2D g, String text, double centerX, double y, Font font,
                                        Color fill, Color outline) {
        g.setFont(font);
        double width = g.getFontMetrics().stringWidth(text);
        outlined(g, text, centerX - width / 2.0, y, font, fill, outline);
    }

    /** Splits text into lines that fit in maxWidth pixels with the font currently set on g. */
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
