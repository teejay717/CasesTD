package btd;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;

public class GameButton {
    private final int x, y, width, height;
    private final Image icon;
    private final Image hoverIcon;
    private boolean hover = false;

    public GameButton (Builder b) {
        this.x = b.x;
        this.y = b.y;
        this.width = b.width;
        this.height = b.height;
        this.icon = b.icon;
        this.hoverIcon = b.hoverIcon;
    }

    public boolean contains(int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public void updateHover(int mouseX, int mouseY) {
        hover = contains(mouseX, mouseY);
    }

    public boolean isHovered() {
        return hover;
    }

    public void draw(Graphics g) {
        Image toDraw = (hover && hoverIcon != null) ? hoverIcon : icon;
        g.drawImage(toDraw, x, y, width, height, null);
        if (hover && hoverIcon == null) {
            g.setColor(new Color(255, 255, 255, 60));
            g.fillRect(x, y, width, height);
        }
        
    }
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
