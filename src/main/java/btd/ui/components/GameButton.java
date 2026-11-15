package btd.ui.components;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;

/**
 * [PURPOSE]       A button that is just a picture (PNG). Knows its box, draws itself, and tells if the mouse is on it.
 * [OOP / PATTERN] Builder Pattern (GameButton.Builder) / Encapsulation.
 * [CALLED BY]     btd/ui/screens/LandingPanel.java (X, crate, Play, help and How to Play buttons).
 * [CONNECTS TO]   java.awt.Graphics. (The images are loaded by LandingPanel with ImageLoader and passed in.)
 * [DEFENSE NOTE]  Builder: new GameButton.Builder(209, 209).position(536, 130).icon(img).build()
 *                 reads better than a constructor with 6 arguments, and you only set what you need.
 */
public class GameButton {
    private final int x, y, width, height;
    private final Image icon;
    private final Image hoverIcon;
    private boolean hover = false;

    /**
     * [PURPOSE]       Constructs button from Builder instance values.
     * [OOP / PATTERN] Builder Pattern Constructor.
     * [CALLED BY]     Builder.build().
     */
    public GameButton(Builder b) {
        this.x = b.x;
        this.y = b.y;
        this.width = b.width;
        this.height = b.height;
        this.icon = b.icon;
        this.hoverIcon = b.hoverIcon;
    }

    /** [PURPOSE] Hit-test checking if cursor coordinate (mouseX, mouseY) falls within button bounds. */
    public boolean contains(int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    /** [PURPOSE] Updates internal hover state based on cursor position. */
    public void updateHover(int mouseX, int mouseY) {
        hover = contains(mouseX, mouseY);
    }

    /** [PURPOSE] Queries active hover status. */
    public boolean isHovered() {
        return hover;
    }

    /**
     * [PURPOSE]       Draws the icon (or hoverIcon when hovered). With no hoverIcon, hovering adds a light white overlay.
     * [OOP / PATTERN] Component Drawing.
     * [CALLED BY]     btd/ui/screens/LandingPanel.java (paintComponent() and its helpers).
     * [DEFENSE NOTE]  LandingPanel never calls updateHover(), so right now the hover look is never shown.
     */
    public void draw(Graphics g) {
        Image toDraw = (hover && hoverIcon != null) ? hoverIcon : icon;
        g.drawImage(toDraw, x, y, width, height, null);
        if (hover && hoverIcon == null) {
            g.setColor(new Color(255, 255, 255, 60));
            g.fillRect(x, y, width, height);
        }
    }

    /**
     * [PURPOSE]       Collects the settings step by step; build() makes the GameButton.
     * [OOP / PATTERN] Builder Pattern (GoF creational pattern). Each setter returns 'this', so calls can be chained.
     * [CALLED BY]     btd/ui/screens/LandingPanel.java (field initializers).
     */
    public static class Builder {
        private int width, height, x, y;
        private Image icon, hoverIcon;

        public Builder(int width, int height) {
            this.width = width;
            this.height = height;
        }

        public Builder x(int x)                 { this.x = x; return this; }
        public Builder y(int y)                 { this.y = y; return this; }
        public Builder position(int x, int y)   { this.x = x; this.y = y; return this; }
        public Builder width(int width)         { this.width = width; return this; }
        public Builder height(int height)       { this.height = height; return this; }
        public Builder size(int w, int h)       { this.width = w; this.height = h; return this; }
        public Builder icon(Image icon)         { this.icon = icon; return this; }
        public Builder hoverIcon(Image icon)    { this.hoverIcon = icon; return this; }

        public GameButton build() {
            return new GameButton(this);
        }
    }
}
