package btd.core;

import java.awt.Color;

/** A short-lived picture of a shot (a line) or a blast (a circle), drawn by the game panel. */
public class VisualEffect {

    public enum Kind { LINE, CIRCLE }

    private final Kind kind;
    private final double x1, y1, x2, y2; // CIRCLE uses x1, y1 as the center and x2 as the radius
    private final Color color;
    private final double duration;
    private double timeLeft;

    private VisualEffect(Kind kind, double x1, double y1, double x2, double y2, Color color, double duration) {
        this.kind = kind;
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.color = color;
        this.duration = duration;
        this.timeLeft = duration;
    }

    public static VisualEffect line(double x1, double y1, double x2, double y2, Color color, double seconds) {
        return new VisualEffect(Kind.LINE, x1, y1, x2, y2, color, seconds);
    }

    public static VisualEffect circle(double x, double y, double radius, Color color, double seconds) {
        return new VisualEffect(Kind.CIRCLE, x, y, radius, 0, color, seconds);
    }

    public void update(double dt) {
        timeLeft -= dt;
    }

    public boolean isDone() {
        return timeLeft <= 0;
    }

    /** 1.0 when new, fading to 0.0 when finished. */
    public double getFade() {
        return Math.max(0, Math.min(1, timeLeft / duration));
    }

    public Kind getKind() { return kind; }
    public double getX1() { return x1; }
    public double getY1() { return y1; }
    public double getX2() { return x2; }
    public double getY2() { return y2; }
    public Color getColor() { return color; }
}
