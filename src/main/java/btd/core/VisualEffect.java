package btd.core;

import java.awt.Color;

/**
 * [PURPOSE]       A short-lived shot animation: a LINE (bullet/laser) or a CIRCLE (explosion / stun wave).
 *                 It is only for looks; the damage was already done by the tower.
 * [OOP / PATTERN] Private Constructor + Static Factory Methods (line(), circle()).
 * [CALLED BY]     Created in the towers' attack() methods; updated by btd/core/GameEngine.java (updateEffects()).
 * [CONNECTS TO]   btd/ui/screens/GamePanel.java (paintEffect()) draws it.
 * [DEFENSE NOTE]  getFade() goes from 1.0 to 0.0 over its life (0.1 - 0.3 s), so it fades out; then the engine removes it.
 */
public class VisualEffect {

    /** Enumeration of supported geometric animation shapes. */
    public enum Kind { LINE, CIRCLE }

    private final Kind kind;
    private final double x1, y1, x2, y2; // CIRCLE uses x1, y1 as the center and x2 as the radius
    private final Color color;
    private final double duration;
    private double timeLeft;

    /**
     * [PURPOSE]       Constructs visual effect with position, color, and lifespan duration.
     * [OOP / PATTERN] Private Constructor (other classes must use line() or circle()).
     * [CALLED BY]     line(), circle().
     * [CONNECTS TO]   timeLeft starts equal to duration.
     */
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

    /**
     * [PURPOSE]       Creates a shot line from (x1, y1) to (x2, y2).
     * [OOP / PATTERN] Static Factory Method (the name says what you get, unlike "new VisualEffect(Kind.LINE, ...)").
     * [CALLED BY]     SingleTargetTower.attack(), SniperTower.attack() and pierce(), SupportTower.fireTurrets().
     * [CONNECTS TO]   btd/core/VisualEffect constructor.
     */
    public static VisualEffect line(double x1, double y1, double x2, double y2, Color color, double seconds) {
        return new VisualEffect(Kind.LINE, x1, y1, x2, y2, color, seconds);
    }

    /**
     * [PURPOSE]       Creates a fading circle (explosion or stun wave). The circle does not grow; it only fades.
     * [OOP / PATTERN] Static Factory Method.
     * [CALLED BY]     SplashTower.attack(), SupportTower.stunAll().
     * [CONNECTS TO]   btd/core/VisualEffect constructor.
     */
    public static VisualEffect circle(double x, double y, double radius, Color color, double seconds) {
        return new VisualEffect(Kind.CIRCLE, x, y, radius, 0, color, seconds);
    }

    /**
     * [PURPOSE]       Decrements remaining lifespan timer by delta time dt.
     * [OOP / PATTERN] Lifecycle Tick / State Update.
     * [CALLED BY]     btd/core/GameEngine.java (updateEffects()).
     */
    public void update(double dt) {
        timeLeft -= dt;
    }

    /** [PURPOSE] Returns true when lifespan timer has reached zero. */
    public boolean isDone() {
        return timeLeft <= 0;
    }

    /**
     * [PURPOSE]       Calculates normalized opacity alpha ratio from 1.0 (fresh) down to 0.0 (expired).
     * [OOP / PATTERN] Interpolation / Derived Property.
     * [CALLED BY]     btd/ui/screens/GamePanel.java (paintEffect()).
     * [CONNECTS TO]   GamePanel multiplies the color's alpha by this value (new Color(r, g, b, 255 * fade)).
     */
    public double getFade() {
        return Math.max(0, Math.min(1, timeLeft / duration));
    }

    /** [PURPOSE] Geometric kind (LINE or CIRCLE). */
    public Kind getKind() { return kind; }
    /** [PURPOSE] Line start X or circle center X. */
    public double getX1() { return x1; }
    /** [PURPOSE] Line start Y or circle center Y. */
    public double getY1() { return y1; }
    /** [PURPOSE] Line end X or circle blast radius. */
    public double getX2() { return x2; }
    /** [PURPOSE] Line end Y. */
    public double getY2() { return y2; }
    /** [PURPOSE] Render color for line stroke or circular fill. */
    public Color getColor() { return color; }
}
