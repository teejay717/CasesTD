package btd.ui.components;

import btd.model.tower.TowerType;
import btd.util.SpriteLoader;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.Ellipse2D;

/**
 * Draws a tower icon. If the sprite file from the database exists it is used, otherwise a
 * simple shape is drawn so the game works before the art is ready.
 *
 * PLACEHOLDER VISUALS: the UI/UX team is still designing the real look, so this screen uses simple
 * shapes and colors. The game rules live in btd.core and do not depend on how things are drawn.
 */
public final class TowerArt {

    private TowerArt() {
    }

    public static Color classColor(TowerType t) {
        return switch (t.towerClass()) {
            case "SingleTargetTower" -> new Color(0xC9A66B);
            case "SplashTower" -> new Color(0xE0734A);
            case "SniperTower" -> new Color(0x4F7F52);
            default -> new Color(0x4AA3A8);
        };
    }

    /** Draws the icon centered on (cx, cy) inside a square of the given size. */
    public static void draw(Graphics2D g, TowerType t, double cx, double cy, double size) {
        Image sprite = SpriteLoader.get(t.spritePath());
        if (sprite != null) {
            g.drawImage(sprite, (int) (cx - size / 2), (int) (cy - size / 2), (int) size, (int) size, null);
            return;
        }
        double r = size / 2;
        float line = (float) Math.max(2, size / 12);

        g.setColor(new Color(0, 0, 0, 60));
        g.fill(new Ellipse2D.Double(cx - r + 2, cy - r + 3, size, size));
        g.setColor(classColor(t));
        g.fill(new Ellipse2D.Double(cx - r, cy - r, size, size));
        g.setColor(t.rarity().getColor());
        g.setStroke(new BasicStroke(line));
        g.draw(new Ellipse2D.Double(cx - r, cy - r, size, size));

        g.setColor(new Color(0x2B1B10));
        g.setStroke(new BasicStroke(line, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (t.towerClass()) {
            case "SingleTargetTower" -> {
                g.drawLine((int) cx, (int) cy, (int) cx, (int) (cy - r * 0.8));
                g.fill(new Ellipse2D.Double(cx - r * 0.3, cy - r * 0.3, r * 0.6, r * 0.6));
            }
            case "SplashTower" -> {
                double d = r * 0.42;
                g.fill(new Ellipse2D.Double(cx - d, cy - d * 1.1, d * 1.1, d * 1.1));
                g.fill(new Ellipse2D.Double(cx, cy - d * 1.1, d * 1.1, d * 1.1));
                g.fill(new Ellipse2D.Double(cx - d * 0.55, cy, d * 1.1, d * 1.1));
            }
            case "SniperTower" -> {
                g.draw(new Ellipse2D.Double(cx - r * 0.45, cy - r * 0.45, r * 0.9, r * 0.9));
                g.drawLine((int) (cx - r * 0.7), (int) cy, (int) (cx + r * 0.7), (int) cy);
                g.drawLine((int) cx, (int) (cy - r * 0.7), (int) cx, (int) (cy + r * 0.7));
            }
            default -> {
                g.drawLine((int) (cx - r * 0.5), (int) cy, (int) (cx + r * 0.5), (int) cy);
                g.drawLine((int) cx, (int) (cy - r * 0.5), (int) cx, (int) (cy + r * 0.5));
            }
        }
        g.setStroke(new BasicStroke(1f));
    }
}
