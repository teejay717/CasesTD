package btd.ui.screens;

import btd.core.Catalog;
import btd.core.GameSession;
import btd.model.gacha.CaseDef;
import btd.model.gacha.Rarity;
import btd.model.tower.TowerType;
import btd.ui.components.Gfx;
import btd.ui.components.TextButton;
import btd.ui.components.TowerArt;
import btd.ui.theme.Palette;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The case shop drawn on top of the map. It has three views: the list of cases, the spinning
 * strip while a case opens, and the result card. The tower is rolled the moment the player
 * pays (in GameSession); the spinner is only the animation that reveals it.
 *
 * PLACEHOLDER VISUALS: the UI/UX team is still designing the real look, so this screen uses simple
 * shapes and colors. The game rules live in btd.core and do not depend on how things are drawn.
 */
public class ShopOverlay {

    private enum View { CASES, SPINNING, RESULT }

    // Overlay panel position and size on the screen
    private static final int X = 60;
    private static final int Y = 70;
    private static final int W = 840;
    private static final int H = 580;

    private static final int CARD_W = 250;
    private static final int CARD_H = 380;
    private static final int ITEM_W = 112;
    private static final int ITEM_GAP = 8;
    private static final int STRIP_ITEMS = 46;
    private static final int WIN_INDEX = 38;
    private static final double SPIN_SECONDS = 5.0;
    private static final int WINDOW_W = 760;

    private final Catalog catalog;
    private final GameSession session;
    private final Random random = new Random();

    private final TextButton closeButton = new TextButton("X", X + W - 62, Y + 14, 46, 44, Palette.BAD, 24f);
    private final TextButton collectButton = new TextButton("COLLECT", X + W / 2 - 110, Y + H - 90, 220, 60, Palette.GOOD, 30f);
    private final List<TextButton> caseButtons = new ArrayList<>();

    private boolean open;
    private View view = View.CASES;

    private CaseDef spinningCase;
    private TowerType won;
    private final List<TowerType> strip = new ArrayList<>();
    private double spinTime;
    private double finalOffset;
    private double offset;

    public ShopOverlay(Catalog catalog, GameSession session) {
        this.catalog = catalog;
        this.session = session;
        for (int i = 0; i < catalog.getCases().size(); i++) {
            caseButtons.add(new TextButton("", cardX(i) + 25, Y + 100 + CARD_H - 70, CARD_W - 50, 52, btd.ui.theme.Theme.ORANGE_COLOR, 26f));
        }
    }

    private int cardX(int i) {
        int count = catalog.getCases().size();
        int total = count * CARD_W + (count - 1) * 20;
        return X + (W - total) / 2 + i * (CARD_W + 20);
    }

    public boolean isOpen() { return open; }

    public void open() {
        open = true;
        view = View.CASES;
    }

    /** Closing is not allowed while a case is still spinning. */
    public void close() {
        if (view != View.SPINNING) {
            open = false;
        }
    }

    // ---- logic ----

    public void update(double dt) {
        if (!open || view != View.SPINNING) {
            return;
        }
        spinTime += dt;
        double t = Math.min(1.0, spinTime / SPIN_SECONDS);
        double eased = 1 - Math.pow(1 - t, 4);
        offset = finalOffset * eased;
        if (t >= 1.0) {
            view = View.RESULT;
        }
    }

    /** Handles a click at screen position (x, y). */
    public void click(int x, int y) {
        if (!open) {
            return;
        }
        switch (view) {
            case CASES -> clickCases(x, y);
            case SPINNING -> {
                // clicking skips the animation
                spinTime = SPIN_SECONDS;
                offset = finalOffset;
                view = View.RESULT;
            }
            case RESULT -> {
                if (collectButton.contains(x, y)) {
                    view = View.CASES;
                }
            }
        }
    }

    public void mouseMoved(int x, int y) {
        closeButton.updateHover(x, y);
        collectButton.updateHover(x, y);
        for (TextButton b : caseButtons) {
            b.updateHover(x, y);
        }
    }

    private void clickCases(int x, int y) {
        if (closeButton.contains(x, y)) {
            close();
            return;
        }
        List<CaseDef> cases = catalog.getCases();
        for (int i = 0; i < cases.size(); i++) {
            if (caseButtons.get(i).contains(x, y) && caseButtons.get(i).isEnabled()) {
                openCase(cases.get(i), i);
                return;
            }
        }
    }

    private void openCase(CaseDef def, int index) {
        TowerType result = (index == 0 && session.getFreeCases() > 0)
                ? session.openFreeCase(def)
                : session.buyCase(def);
        if (result == null) {
            return;
        }
        won = result;
        spinningCase = def;
        strip.clear();
        for (int i = 0; i < STRIP_ITEMS; i++) {
            strip.add(i == WIN_INDEX ? result : def.pool().get(random.nextInt(def.pool().size())));
        }
        double jitter = (random.nextDouble() - 0.5) * ITEM_W * 0.7;
        finalOffset = WIN_INDEX * (ITEM_W + ITEM_GAP) + ITEM_W / 2.0 - WINDOW_W / 2.0 + jitter;
        offset = 0;
        spinTime = 0;
        view = View.SPINNING;
    }

    // ---- painting ----

    public void paint(Graphics2D g) {
        if (!open) {
            return;
        }
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, 960, 720);

        g.setColor(Palette.PANEL);
        g.fillRoundRect(X, Y, W, H, 30, 30);
        g.setColor(Palette.PANEL_DARK);
        g.setStroke(new BasicStroke(5f));
        g.drawRoundRect(X, Y, W, H, 30, 30);

        Gfx.outlinedCentered(g, "CASE SHOP", X + W / 2.0, Y + 52, Palette.heading(44f), Palette.GOLD, Palette.PANEL_DARK);
        Gfx.centered(g, "Coins: " + session.getCoins() + "     Free cases: " + session.getFreeCases(),
                X + W / 2.0, Y + 82, Palette.body(16f), Palette.CREAM);

        switch (view) {
            case CASES -> paintCases(g);
            case SPINNING -> paintSpinner(g);
            case RESULT -> paintResult(g);
        }
    }

    private void paintCases(Graphics2D g) {
        List<CaseDef> cases = catalog.getCases();
        for (int i = 0; i < cases.size(); i++) {
            CaseDef def = cases.get(i);
            int cx = cardX(i);
            int cy = Y + 100;
            g.setColor(Palette.CREAM);
            g.fillRoundRect(cx, cy, CARD_W, CARD_H, 22, 22);
            g.setColor(Palette.PANEL_DARK);
            g.setStroke(new BasicStroke(4f));
            g.drawRoundRect(cx, cy, CARD_W, CARD_H, 22, 22);

            Gfx.centered(g, def.name().toUpperCase(), cx + CARD_W / 2.0, cy + 38, Palette.heading(26f), Palette.PANEL_DARK);

            // tower icons in the pool (5 per row)
            List<TowerType> pool = def.pool();
            for (int t = 0; t < pool.size(); t++) {
                double ix = cx + 36 + (t % 5) * 44;
                double iy = cy + 78 + (t / 5) * 44;
                TowerArt.draw(g, pool.get(t), ix, iy, 34);
            }

            // odds
            int ly = cy + 175;
            Gfx.centered(g, "Drop chances", cx + CARD_W / 2.0, ly, Palette.body(14f), Palette.PANEL_DARK);
            ly += 10;
            for (Rarity r : Rarity.values()) {
                double w = def.weightOf(r);
                if (w <= 0) {
                    continue;
                }
                ly += 24;
                g.setColor(r.getColor());
                g.fillRoundRect(cx + 30, ly - 15, 18, 18, 6, 6);
                g.setFont(Palette.body(15f));
                g.setColor(Palette.PANEL_DARK);
                g.drawString(r.getDisplayName(), cx + 56, ly);
                String pct = (w == Math.floor(w) ? String.valueOf((int) w) : String.valueOf(w)) + "%";
                g.drawString(pct, cx + CARD_W - 30 - g.getFontMetrics().stringWidth(pct), ly);
            }

            TextButton b = caseButtons.get(i);
            boolean free = i == 0 && session.getFreeCases() > 0;
            b.setText(free ? "OPEN FREE" : "BUY  " + def.cost());
            b.setEnabled(free || session.getCoins() >= def.cost());
            b.draw(g);
        }
        closeButton.draw(g);
    }

    private void paintSpinner(Graphics2D g) {
        Gfx.centered(g, "Opening " + spinningCase.name() + "...", X + W / 2.0, Y + 150, Palette.heading(30f), Palette.CREAM);
        paintStrip(g);
        Gfx.centered(g, "(click to skip)", X + W / 2.0, Y + H - 40, Palette.body(14f), Palette.CREAM_DARK);
    }

    private void paintStrip(Graphics2D g) {
        int wx = X + (W - WINDOW_W) / 2;
        int wy = Y + 190;
        int wh = 170;

        g.setColor(Palette.PANEL_DARK);
        g.fillRoundRect(wx - 6, wy - 6, WINDOW_W + 12, wh + 12, 16, 16);

        Shape oldClip = g.getClip();
        g.setClip(wx, wy, WINDOW_W, wh);
        g.setColor(new Color(0x6A4C34));
        g.fillRect(wx, wy, WINDOW_W, wh);
        for (int i = 0; i < strip.size(); i++) {
            TowerType t = strip.get(i);
            double ix = wx + i * (ITEM_W + ITEM_GAP) - offset;
            if (ix + ITEM_W < wx || ix > wx + WINDOW_W) {
                continue;
            }
            Rectangle card = new Rectangle((int) ix, wy + 10, ITEM_W, wh - 20);
            g.setColor(Palette.CREAM);
            g.fillRoundRect(card.x, card.y, card.width, card.height, 14, 14);
            g.setColor(t.rarity().getColor());
            g.fillRoundRect(card.x, card.y + card.height - 26, card.width, 26, 14, 14);
            g.fillRect(card.x, card.y + card.height - 26, card.width, 10);
            g.setStroke(new BasicStroke(3f));
            g.drawRoundRect(card.x, card.y, card.width, card.height, 14, 14);
            TowerArt.draw(g, t, card.getCenterX(), card.y + 52, 62);
            Gfx.centered(g, t.name(), card.getCenterX(), card.y + card.height - 8, Palette.body(12f), Color.WHITE);
        }
        g.setClip(oldClip);

        // center marker
        int mx = wx + WINDOW_W / 2;
        g.setColor(Palette.GOLD);
        g.setStroke(new BasicStroke(4f));
        g.drawLine(mx, wy - 4, mx, wy + wh + 4);
        g.fillPolygon(new int[]{mx - 12, mx + 12, mx}, new int[]{wy - 16, wy - 16, wy + 2}, 3);
        g.fillPolygon(new int[]{mx - 12, mx + 12, mx}, new int[]{wy + wh + 16, wy + wh + 16, wy + wh - 2}, 3);
    }

    private void paintResult(Graphics2D g) {
        Gfx.centered(g, "YOU GOT", X + W / 2.0, Y + 150, Palette.heading(30f), Palette.CREAM);

        int cx = X + W / 2;
        g.setColor(won.rarity().getColor());
        g.fillRoundRect(cx - 150, Y + 170, 300, 270, 26, 26);
        g.setColor(Palette.CREAM);
        g.fillRoundRect(cx - 142, Y + 178, 284, 254, 20, 20);
        TowerArt.draw(g, won, cx, Y + 245, 100);

        Gfx.centered(g, won.name().toUpperCase(), cx, Y + 330, Palette.heading(34f), Palette.PANEL_DARK);
        String gems = "*".repeat(won.rarity().getGems());
        Gfx.centered(g, won.rarity().getDisplayName() + "  " + gems, cx, Y + 358,
                Palette.body(18f), won.rarity().getColor().darker());
        List<String> lines = Gfx.wrap(g, won.description() == null ? "" : won.description(), Palette.body(14f), 250);
        int ty = Y + 384;
        for (String line : lines) {
            Gfx.centered(g, line, cx, ty, Palette.body(14f), Palette.PANEL_DARK);
            ty += 18;
        }
        Gfx.centered(g, "Added to your inventory. You now have " + session.copiesOf(won) + ".",
                cx, Y + 468, Palette.body(15f), Palette.CREAM);
        collectButton.draw(g);
    }
}
