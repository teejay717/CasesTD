package btd.ui.screens;

import btd.core.GameEngine;
import btd.core.GameSession;
import btd.model.tower.Tower;
import btd.model.tower.TowerType;
import btd.ui.components.Gfx;
import btd.ui.components.TextButton;
import btd.ui.components.TowerArt;
import btd.ui.theme.Palette;
import btd.ui.theme.Theme;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The right-hand panel: inventory of tower copies, info about the selected tower, and the
 * SHOP / START WAVE / SPEED buttons. It only draws and reports clicks; GamePanel acts on them.
 *
 * PLACEHOLDER VISUALS: the UI/UX team is still designing the real look, so this screen uses simple
 * shapes and colors. The game rules live in btd.core and do not depend on how things are drawn.
 */
public class SidePanel {

    /** What GamePanel must do when the player uses the panel. */
    public interface Actions {
        void selectInventory(TowerType type);
        void sellInventory(TowerType type, int screenX, int screenY);
        void upgradeSelected();
        void sellSelected();
        void openShop();
        void startWave();
        void cycleSpeed();
    }

    public static final int X = 960;
    private static final int WIDTH = Theme.CONTENT_WIDTH - X;

    private record InvCard(Rectangle bounds, TowerType type) {
    }

    private final Actions actions;
    private GameSession session;
    private GameEngine engine;

    private final List<InvCard> cards = new ArrayList<>();
    private final TextButton shopButton = new TextButton("SHOP", X + 10, 646, 92, 52, Theme.ORANGE_COLOR, 24f);
    private final TextButton waveButton = new TextButton("START WAVE", X + 108, 646, 146, 52, Palette.GOOD, 22f);
    private final TextButton speedButton = new TextButton("x1", X + 260, 646, 52, 52, new Color(0x4A90D9), 22f);
    private final TextButton upgradeButton = new TextButton("UPGRADE", X + 16, 578, 172, 38, Palette.GOOD, 20f);
    private final TextButton sellButton = new TextButton("SELL", X + 194, 578, 110, 38, Palette.BAD, 20f);

    private Tower selectedTower;
    private TowerType placing;
    private int speed = 1;

    public SidePanel(Actions actions) {
        this.actions = actions;
    }

    public void bind(GameSession session, GameEngine engine) {
        this.session = session;
        this.engine = engine;
        selectedTower = null;
        placing = null;
        speed = 1;
    }

    public void setSelectedTower(Tower t) { this.selectedTower = t; }
    public void setPlacing(TowerType t) { this.placing = t; }
    public void setSpeed(int speed) { this.speed = speed; }

    // ---- input ----

    public void mouseMoved(int x, int y) {
        shopButton.updateHover(x, y);
        waveButton.updateHover(x, y);
        speedButton.updateHover(x, y);
        upgradeButton.updateHover(x, y);
        sellButton.updateHover(x, y);
    }

    /** Left or right click inside the panel. */
    public void click(int x, int y, boolean rightClick) {
        for (InvCard card : cards) {
            if (card.bounds().contains(x, y)) {
                if (rightClick) {
                    actions.sellInventory(card.type(), x, y);
                } else {
                    actions.selectInventory(card.type());
                }
                return;
            }
        }
        if (rightClick) {
            return;
        }
        if (shopButton.contains(x, y)) {
            actions.openShop();
        } else if (waveButton.contains(x, y) && waveButton.isEnabled()) {
            actions.startWave();
        } else if (speedButton.contains(x, y)) {
            actions.cycleSpeed();
        } else if (selectedTower != null && upgradeButton.contains(x, y) && upgradeButton.isEnabled()) {
            actions.upgradeSelected();
        } else if (selectedTower != null && sellButton.contains(x, y)) {
            actions.sellSelected();
        }
    }

    // ---- painting ----

    public void paint(Graphics2D g) {
        g.setColor(Palette.PANEL);
        g.fillRect(X, 0, WIDTH, Theme.CONTENT_HEIGHT);
        g.setColor(Palette.PANEL_DARK);
        g.fillRect(X, 0, 5, Theme.CONTENT_HEIGHT);

        Gfx.outlined(g, "INVENTORY", X + 14, 40, Palette.heading(30f), Palette.GOLD, Palette.PANEL_DARK);
        paintInventory(g);
        paintInfo(g);

        waveButton.setEnabled(engine.getState() == GameEngine.State.BETWEEN_WAVES);
        waveButton.setText(engine.getState() == GameEngine.State.WAVE_ACTIVE ? "WAVE " + engine.getWave()
                : "START WAVE");
        speedButton.setText("x" + speed);
        shopButton.draw(g);
        waveButton.draw(g);
        speedButton.draw(g);
    }

    private void paintInventory(Graphics2D g) {
        cards.clear();
        Map<Integer, Integer> counts = new LinkedHashMap<>();
        Map<Integer, TowerType> types = new LinkedHashMap<>();
        for (TowerType t : session.getInventory()) {
            counts.merge(t.id(), 1, Integer::sum);
            types.putIfAbsent(t.id(), t);
        }

        if (types.isEmpty()) {
            Gfx.centered(g, "No towers yet.", X + WIDTH / 2.0, 120, Palette.body(15f), Palette.CREAM);
            Gfx.centered(g, "Open the SHOP to get some!", X + WIDTH / 2.0, 142, Palette.body(15f), Palette.CREAM);
            return;
        }

        int index = 0;
        for (TowerType t : types.values()) {
            int col = index % 2;
            int row = index / 2;
            Rectangle r = new Rectangle(X + 10 + col * 153, 54 + row * 76, 147, 70);
            cards.add(new InvCard(r, t));
            boolean selected = placing != null && placing.id() == t.id();

            g.setColor(selected ? Palette.GOLD : Palette.CREAM);
            g.fillRoundRect(r.x, r.y, r.width, r.height, 14, 14);
            g.setColor(t.rarity().getColor());
            g.fillRoundRect(r.x, r.y, 8, r.height, 8, 8);
            g.setColor(Palette.PANEL_DARK);
            g.setStroke(new BasicStroke(3f));
            g.drawRoundRect(r.x, r.y, r.width, r.height, 14, 14);

            TowerArt.draw(g, t, r.x + 38, r.y + 35, 40);
            Gfx.outlined(g, "x" + counts.get(t.id()), r.x + 70, r.y + 28, Palette.heading(22f), Color.WHITE, Palette.PANEL_DARK);
            g.setFont(Palette.body(11f));
            g.setColor(Palette.PANEL_DARK);
            g.drawString(shorten(t.name(), 12), r.x + 62, r.y + 48);
            g.setColor(t.rarity().getColor().darker());
            g.drawString(t.rarity().getDisplayName(), r.x + 62, r.y + 62);
            index++;
        }
    }

    private String shorten(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + ".";
    }

    private void paintInfo(Graphics2D g) {
        int top = 446;
        g.setColor(Palette.CREAM);
        g.fillRoundRect(X + 10, top, WIDTH - 20, 196, 16, 16);
        g.setColor(Palette.PANEL_DARK);
        g.setStroke(new BasicStroke(3f));
        g.drawRoundRect(X + 10, top, WIDTH - 20, 196, 16, 16);

        if (selectedTower != null) {
            paintTowerInfo(g, top);
        } else if (placing != null) {
            paintPlacingInfo(g, top);
        } else {
            Gfx.centered(g, "Click a card to place a tower,", X + WIDTH / 2.0, top + 60, Palette.body(14f), Palette.PANEL_DARK);
            Gfx.centered(g, "or click a tower on the map.", X + WIDTH / 2.0, top + 80, Palette.body(14f), Palette.PANEL_DARK);
            Gfx.centered(g, "Right-click a card to sell it.", X + WIDTH / 2.0, top + 110, Palette.body(14f), Palette.PANEL_DARK);
            Gfx.centered(g, "Space = start wave", X + WIDTH / 2.0, top + 140, Palette.body(14f), Palette.PANEL_DARK);
        }
    }

    private void paintTowerInfo(Graphics2D g, int top) {
        Tower t = selectedTower;
        TowerType type = t.getType();
        Gfx.outlined(g, type.name(), X + 20, top + 30, Palette.heading(22f), type.rarity().getColor().darker(), Palette.CREAM);
        String level = "Lv " + t.getLevel();
        g.setFont(Palette.heading(22f));
        g.setColor(Palette.PANEL_DARK);
        g.drawString(level, X + WIDTH - 20 - g.getFontMetrics().stringWidth(level), top + 30);

        g.setFont(Palette.body(13f));
        g.setColor(Palette.PANEL_DARK);
        g.drawString(String.format("Damage %.1f   Rate %.2f/s   Range %d", t.getDamage(), t.getFireRate(), type.range()),
                X + 20, top + 50);

        int y = top + 68;
        for (String line : Gfx.wrap(g, type.description(), Palette.body(13f), WIDTH - 44)) {
            g.drawString(line, X + 20, y);
            y += 16;
        }
        g.setColor(t.getLevel() >= Tower.MAX_LEVEL ? new Color(0xB07A00) : new Color(0x6A5A48));
        for (String line : Gfx.wrap(g, t.getPassiveDescription(), Palette.body(12f), WIDTH - 44)) {
            g.drawString(line, X + 20, y);
            y += 15;
        }

        String reason = session.upgradeBlockReason(t);
        int cost = session.upgradeCost(t);
        upgradeButton.setEnabled(reason == null);
        upgradeButton.setText(t.getLevel() >= Tower.MAX_LEVEL ? "MAX LEVEL" : "UPGRADE  -" + cost);
        sellButton.setText("SELL +" + session.refundFor(type));
        upgradeButton.draw(g);
        sellButton.draw(g);
        if (reason != null) {
            Gfx.centered(g, reason, X + WIDTH / 2.0, top + 188, Palette.body(12f), Palette.BAD);
        }
    }

    private void paintPlacingInfo(Graphics2D g, int top) {
        TowerType type = placing;
        Gfx.outlined(g, type.name(), X + 20, top + 30, Palette.heading(22f), type.rarity().getColor().darker(), Palette.CREAM);
        g.setFont(Palette.body(13f));
        g.setColor(Palette.PANEL_DARK);
        g.drawString(String.format("Damage %d   Rate %.2f/s   Range %d", type.baseDamage(), type.fireRate(), type.range()),
                X + 20, top + 50);
        int y = top + 68;
        for (String line : Gfx.wrap(g, type.description(), Palette.body(13f), WIDTH - 44)) {
            g.drawString(line, X + 20, y);
            y += 16;
        }
        g.setColor(Palette.GOOD.darker());
        g.drawString("Click the map to place it.", X + 20, top + 140);
        g.drawString("Right-click or Esc to cancel.", X + 20, top + 158);
        g.setColor(new Color(0x6A5A48));
        g.drawString("Keep spare copies to upgrade towers.", X + 20, top + 180);
    }
}
