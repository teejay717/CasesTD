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
import btd.ui.theme.Theme;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * [PURPOSE]       The case shop drawn on top of the game: case cards with drop chances, the spinning strip,
 *                 and the "YOU GOT" result card.
 * [OOP / PATTERN] State Machine with an enum (CASES -> SPINNING -> RESULT -> back to CASES) / Modal Overlay.
 *                 Not a JPanel: GamePanel calls paint(), click(), update() and mouseMoved() on it.
 * [CALLED BY]     btd/ui/screens/GamePanel.java (created in startRun(); opened by openShop()).
 * [CONNECTS TO]   btd/core/GameSession.java (buyCase(), openFreeCase()),
 *                 btd/model/gacha/CaseDef.java, btd/core/Catalog.java, btd/ui/components/TowerArt.java.
 * [DEFENSE NOTE]  The prize is decided BEFORE the spin starts (GameSession rolls it and adds it to the inventory).
 *                 The 5 s spin is only an animation that is set up to stop on that prize.
 *                 While the shop is open, GamePanel.advance() does not update the engine, so the game is paused.
 */
public class ShopOverlay {

    /** Internal states of the shop overlay workflow. */
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

    /**
     * [PURPOSE]       Makes one BUY button per case in the catalog (3 cases = 3 buttons).
     * [OOP / PATTERN] Constructor / Component Initialization.
     * [CALLED BY]     btd/ui/screens/GamePanel.java (startRun()).
     */
    public ShopOverlay(Catalog catalog, GameSession session) {
        this.catalog = catalog;
        this.session = session;
        for (int i = 0; i < catalog.getCases().size(); i++) {
            caseButtons.add(new TextButton("", cardX(i) + 25, Y + 100 + CARD_H - 70, CARD_W - 50, 52, btd.ui.theme.Theme.ORANGE_COLOR, 26f));
        }
    }

    /** [PURPOSE] Left x of case card i, so all cards (250 px wide, 20 px gap) are centered in the shop. */
    private int cardX(int i) {
        int count = catalog.getCases().size();
        int total = count * CARD_W + (count - 1) * 20;
        return X + (W - total) / 2 + i * (CARD_W + 20);
    }

    /** [PURPOSE] Queries whether shop overlay is currently visible. */
    public boolean isOpen() { return open; }

    /** [PURPOSE] Opens shop modal and sets view to CASES catalog. */
    public void open() {
        open = true;
        view = View.CASES;
    }

    /**
     * [PURPOSE]       Closes the shop, but not while it is spinning.
     * [OOP / PATTERN] Guard Clause.
     * [CALLED BY]     clickCases() (the X button), btd/ui/screens/GamePanel.java (cancelAction(), Esc / right-click).
     * [DEFENSE NOTE]  Mid-spin, Esc does nothing. The player can still click to skip to the result.
     */
    public void close() {
        if (view != View.SPINNING) {
            open = false;
        }
    }

    // ---- logic ----

    /**
     * [PURPOSE]       Moves the spin animation forward. t goes 0 -> 1 over 5 s; offset = finalOffset x (1 - (1 - t)^4).
     * [OOP / PATTERN] Easing Function ("ease-out": fast at first, then slows down).
     * [CALLED BY]     btd/ui/screens/GamePanel.java (advance(), only while the shop is open).
     * [DEFENSE NOTE]  At t = 0.5 the strip has already moved about 94% of the way, so the last seconds are a slow crawl.
     *                 When t reaches 1 the view switches to RESULT.
     */
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

    /**
     * [PURPOSE]       What a click does depends on the view: CASES = buy / close, SPINNING = skip to result,
     *                 RESULT = COLLECT goes back to the case list. (The tower is already in the inventory.)
     * [OOP / PATTERN] Input State Dispatcher.
     * [CALLED BY]     btd/ui/screens/GamePanel.java (handlePress()).
     */
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

    /** [PURPOSE] Updates hover highlight on the shop buttons. Called by GamePanel.handleMove(). */
    public void mouseMoved(int x, int y) {
        closeButton.updateHover(x, y);
        collectButton.updateHover(x, y);
        for (TextButton b : caseButtons) {
            b.updateHover(x, y);
        }
    }

    /** [PURPOSE] In the CASES view: X closes the shop; an enabled BUY / OPEN FREE button opens that case. */
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

    /**
     * [PURPOSE]       Opens a case: 1) GameSession rolls the prize (free case or paid), 2) build a strip of 46 towers
     *                 with the prize at slot 38, 3) work out how far to scroll so slot 38 stops under the gold line.
     * [OOP / PATTERN] Animation Setup.
     * [CALLED BY]     clickCases().
     * [CONNECTS TO]   btd/core/GameSession.java (buyCase(), openFreeCase()).
     * [DEFENSE NOTE]  The free starter case only works on the FIRST case card (index 0 = Standard Case).
     *                 The other 45 slots are random towers from the pool, just for show.
     *                 'jitter' moves the stop point a little left or right so it doesn't always land dead center.
     */
    private void openCase(CaseDef def, int index) {
        TowerType result = (index == 0 && session.getFreeCases() > 0)
                ? session.openFreeCase(def)
                : session.buyCase(def);
        if (result == null) {
            return;
        }
        spinningCase = def;
        won = result;

        strip.clear();
        for (int i = 0; i < STRIP_ITEMS; i++) {
            if (i == WIN_INDEX) {
                strip.add(won);
            } else {
                strip.add(def.pool().get(random.nextInt(def.pool().size())));
            }
        }
        double targetCenter = WIN_INDEX * (ITEM_W + ITEM_GAP) + ITEM_W / 2.0;
        double jitter = (random.nextDouble() - 0.5) * (ITEM_W * 0.7);
        finalOffset = targetCenter - WINDOW_W / 2.0 + jitter;
        offset = 0;
        spinTime = 0;
        view = View.SPINNING;
    }

    // ---- painting ----

    /**
     * [PURPOSE]       If open: darkens the game, draws the wooden board, then draws the current view.
     * [OOP / PATTERN] switch on the View enum.
     * [CALLED BY]     btd/ui/screens/GamePanel.java (paintComponent()).
     */
    public void paint(Graphics2D g) {
        if (!open) {
            return;
        }
        // dim the game
        g.setColor(new Color(0, 0, 0, 160));
        g.fillRect(0, 0, Theme.CONTENT_WIDTH, Theme.CONTENT_HEIGHT);

        // wooden board
        g.setColor(new Color(0, 0, 0, 90));
        g.fillRoundRect(X - 8, Y + 12, W + 16, H, 36, 36);
        g.setColor(Palette.PANEL);
        g.fillRoundRect(X, Y, W, H, 32, 32);
        g.setColor(Palette.PANEL_DARK);
        g.setStroke(new BasicStroke(6f));
        g.drawRoundRect(X, Y, W, H, 32, 32);

        switch (view) {
            case CASES -> paintCases(g);
            case SPINNING -> paintSpinner(g);
            case RESULT -> paintResult(g);
        }
    }

    /**
     * [PURPOSE] Draws each case card: name, small icons of every tower in its pool, the drop chances (rarities with 0% hidden),
     *           and the button ("OPEN FREE", or "BUY cost" which is greyed out if you can't afford it).
     */
    private void paintCases(Graphics2D g) {
        Gfx.centered(g, "CASE SHOP", X + W / 2.0, Y + 50, Palette.heading(38f), Palette.GOLD);
        Gfx.centered(g, "Roll duplicate towers to upgrade them to higher levels!",
                X + W / 2.0, Y + 80, Palette.body(15f), Palette.CREAM);

        List<CaseDef> cases = catalog.getCases();
        for (int i = 0; i < cases.size(); i++) {
            CaseDef def = cases.get(i);
            int cx = cardX(i);
            int cy = Y + 105;

            // card back
            g.setColor(Palette.CREAM);
            g.fillRoundRect(cx, cy, CARD_W, CARD_H, 20, 20);
            g.setColor(Palette.PANEL_DARK);
            g.setStroke(new BasicStroke(4f));
            g.drawRoundRect(cx, cy, CARD_W, CARD_H, 20, 20);

            // title
            Gfx.centered(g, def.name().toUpperCase(), cx + CARD_W / 2.0, cy + 38,
                    Palette.heading(22f), Palette.PANEL_DARK);

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

    /** [PURPOSE] SPINNING view: "Opening ..." title, the strip, and "(click to skip)". */
    private void paintSpinner(Graphics2D g) {
        Gfx.centered(g, "Opening " + spinningCase.name() + "...", X + W / 2.0, Y + 150, Palette.heading(30f), Palette.CREAM);
        paintStrip(g);
        Gfx.centered(g, "(click to skip)", X + W / 2.0, Y + H - 40, Palette.body(14f), Palette.CREAM_DARK);
    }

    /**
     * [PURPOSE]       Draws the strip of tower cards shifted left by 'offset', plus the gold line in the middle.
     * [OOP / PATTERN] Clipping (g.setClip) / Scrolling by an offset.
     * [CALLED BY]     paintSpinner().
     * [DEFENSE NOTE]  setClip() means anything drawn outside the 760 px window is cut off, then the old clip is restored.
     *                 Cards fully outside the window are skipped (continue) to save drawing time.
     */
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

    /**
     * [PURPOSE]       RESULT view: the won tower with a rarity-colored border, big art (size 100), name, stars,
     *                 description, "You now have N", and the COLLECT button.
     * [OOP / PATTERN] Result Card.
     * [CALLED BY]     paint().
     */
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
