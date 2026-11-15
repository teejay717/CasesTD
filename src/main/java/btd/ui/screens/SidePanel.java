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
 * [PURPOSE]       Right-side HUD sidebar displaying tower inventory cards, tower stat cards,
 *                 upgrade/sell action buttons, speed toggle (1x/2x/3x), and wave start triggers.
 * [OOP / PATTERN] View Component / Interface Segregation Principle / Command Pattern delegation.
 * [CALLED BY]     btd/ui/screens/GamePanel.java.
 * [CONNECTS TO]   btd/core/GameSession.java, btd/core/GameEngine.java,
 *                 btd/model/tower/Tower.java, btd/ui/components/TextButton.java.
 * [DEFENSE NOTE]  SidePanel purely handles rendering and click coordinates, delegating all domain mutations
 *                 back to GamePanel through the strongly-typed Actions callback interface.
 */
public class SidePanel {

    /**
     * [PURPOSE]       Callback interface decoupling SidePanel UI input from GamePanel game loop state.
     * [OOP / PATTERN] Interface Segregation Principle / Observer Pattern / Command Pattern.
     * [CALLED BY]     btd/ui/screens/SidePanel.java (click()).
     * [CONNECTS TO]   btd/ui/screens/GamePanel.java (implements this contract).
     */
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

    /** [PURPOSE] Encapsulates on-screen bounding rectangle and tower metadata for an inventory grid slot. */
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

    /**
     * [PURPOSE]       Constructs SidePanel tied to action callback handler.
     * [OOP / PATTERN] Constructor / Dependency Injection.
     * [CALLED BY]     btd/ui/screens/GamePanel.java constructor.
     */
    public SidePanel(Actions actions) {
        this.actions = actions;
    }

    /**
     * [PURPOSE]       Binds active session and engine to this panel upon new run initialization.
     * [OOP / PATTERN] Model Binding.
     * [CALLED BY]     btd/ui/screens/GamePanel.java (startRun()).
     */
    public void bind(GameSession session, GameEngine engine) {
        this.session = session;
        this.engine = engine;
        selectedTower = null;
        placing = null;
        speed = 1;
    }

    /** [PURPOSE] Sets currently highlighted placed tower instance for stat card inspection. */
    public void setSelectedTower(Tower t) { this.selectedTower = t; }
    /** [PURPOSE] Sets tower type currently being placed onto the map grid. */
    public void setPlacing(TowerType t) { this.placing = t; }
    /** [PURPOSE] Updates simulation speed multiplier display label (x1, x2, x3). */
    public void setSpeed(int speed) { this.speed = speed; }

    // ---- input ----

    /**
     * [PURPOSE]       Dispatches mouse coordinates to buttons for hover state rendering.
     * [OOP / PATTERN] Event Propagation.
     * [CALLED BY]     btd/ui/screens/GamePanel.java (handleMove()).
     */
    public void mouseMoved(int x, int y) {
        shopButton.updateHover(x, y);
        waveButton.updateHover(x, y);
        speedButton.updateHover(x, y);
        upgradeButton.updateHover(x, y);
        sellButton.updateHover(x, y);
    }

    /**
     * [PURPOSE]       Processes mouse clicks in side panel: selects cards, opens shop, starts waves, sells, or upgrades.
     * [OOP / PATTERN] Input Dispatcher / Command Invocation.
     * [CALLED BY]     btd/ui/screens/GamePanel.java (handlePress()).
     * [CONNECTS TO]   Actions interface callbacks.
     * [DEFENSE NOTE]  Right-click on inventory card opens a "Sell 1 ... for N coins" popup (GamePanel.sellInventory()).
     */
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

    /**
     * [PURPOSE]       Renders entire side panel: background wood bar, inventory grid, stat card, and control buttons.
     * [OOP / PATTERN] Composite View Rendering Pipeline.
     * [CALLED BY]     btd/ui/screens/GamePanel.java (paintComponent()).
     */
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

    /**
     * [PURPOSE]       Aggregates inventory cards by archetype and renders 2-column card grid with copy count badges.
     * [OOP / PATTERN] Data Aggregation / Grid Layout Rendering.
     * [CALLED BY]     paint().
     * [CONNECTS TO]   btd/ui/components/TowerArt.java, btd/core/GameSession.java (getInventory()).
     * [DEFENSE NOTE]  Uses LinkedHashMap to preserve first-seen order while counting duplicate stack copies.
     */
    private void paintInventory(Graphics2D g) {
        cards.clear();
        Map<Integer, TowerType> unique = new LinkedHashMap<>();
        Map<Integer, Integer> counts = new LinkedHashMap<>();
        for (TowerType t : session.getInventory()) {
            unique.putIfAbsent(t.id(), t);
            counts.put(t.id(), counts.getOrDefault(t.id(), 0) + 1);
        }

        int index = 0;
        for (TowerType t : unique.values()) {
            if (index >= 10) {
                break;
            }
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

    /** [PURPOSE] Truncates label string with ellipsis if longer than max characters. */
    private String shorten(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + ".";
    }

    /**
     * [PURPOSE]       Renders bottom contextual info card: selected tower stats, placement instructions, or hints.
     * [OOP / PATTERN] Conditional Context Rendering.
     * [CALLED BY]     paint().
     */
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

    /**
     * [PURPOSE]       Renders detailed stats for selected placed tower: damage, fire rate, range, passive, and upgrade buttons.
     * [OOP / PATTERN] Inspection View / Dynamic Binding.
     * [CALLED BY]     paintInfo().
     * [CONNECTS TO]   btd/core/GameSession.java (upgradeBlockReason(), upgradeCost()).
     */
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

    /**
     * [PURPOSE]       Renders placement help card when a tower card is primed for construction on map.
     * [OOP / PATTERN] Contextual Instruction View.
     * [CALLED BY]     paintInfo().
     */
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
