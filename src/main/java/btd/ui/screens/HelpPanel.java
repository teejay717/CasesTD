package btd.ui.screens;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;

import javax.swing.JButton;
import javax.swing.JPanel;

import btd.util.ImageLoader;

public class HelpPanel extends JPanel {

    private static final String[] SPREAD_IMAGES = {"help_book_1.png", "help_book_2.png"};

    /** Set to true if your art has no arrows and you want them drawn. */
    private static final boolean DRAW_ARROWS = false;

    private final Image[] spreads = new Image[SPREAD_IMAGES.length];
    private final ArrowButton prevButton = new ArrowButton(true, "Previous page");
    private final ArrowButton nextButton = new ArrowButton(false, "Next page");
    private final JButton closeButton;

    private int current = 0;

    public HelpPanel() {
        super(null); // manual layout, positioned relative to the book
        setOpaque(false);

        for (int i = 0; i < SPREAD_IMAGES.length; i++) {
            try {
                spreads[i] = ImageLoader.load(SPREAD_IMAGES[i]);
            } catch (Exception e) {
                spreads[i] = null; // missing art -> placeholder is drawn
            }
        }

        closeButton = new JButton(); // invisible hit area over the X drawn in the art
        closeButton.setBorderPainted(false);
        closeButton.setContentAreaFilled(false);
        closeButton.setFocusPainted(false);
        closeButton.setOpaque(false);
        closeButton.setToolTipText("Close How to Play");
        closeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeButton.addActionListener(e -> setVisible(false));

        prevButton.addActionListener(e -> showSpread(current - 1));
        nextButton.addActionListener(e -> showSpread(current + 1));

        add(prevButton);
        add(nextButton);
        add(closeButton);

        // Swallow clicks so they don't reach the game underneath the overlay.
        addMouseListener(new MouseAdapter() { });
        addMouseMotionListener(new MouseAdapter() { });

        showSpread(0);
        setVisible(false);
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            showSpread(0); // always reopen on the first spread
        }
        super.setVisible(visible);
    }

    private void showSpread(int index) {
        current = Math.max(0, Math.min(spreads.length - 1, index));
        prevButton.setVisible(current > 0);
        nextButton.setVisible(current < spreads.length - 1);
        revalidate();
        repaint();
    }

    // ----------------------------------------------------------------- layout

    private Rectangle bookBounds() {
        int maxW = (int) (getWidth() * 0.90);
        int maxH = (int) (getHeight() * 0.80);
        double aspect = 2.2;
        Image img = spreads[current];
        if (img != null && img.getWidth(null) > 0 && img.getHeight(null) > 0) {
            aspect = (double) img.getWidth(null) / img.getHeight(null);
        }
        int w = maxW;
        int h = (int) (w / aspect);
        if (h > maxH) {
            h = maxH;
            w = (int) (h * aspect);
        }
        return new Rectangle((getWidth() - w) / 2, (getHeight() - h) / 2, w, h);
    }

    @Override
    public void doLayout() {
        if (getWidth() <= 0 || getHeight() <= 0) {
            return;
        }
        Rectangle b = bookBounds();

        // Hit areas centered on the arrows / X drawn in the art (fractions of the image).
        int arrow = (int) (b.width * 0.075);
        placeCentered(prevButton, b, 0.082, 0.479, arrow, arrow);
        placeCentered(nextButton, b, 0.921, 0.467, arrow, arrow);
        int x = (int) (b.width * 0.05);
        placeCentered(closeButton, b, 0.930, 0.071, x, x);
    }

    private static void placeCentered(java.awt.Component c, Rectangle b,
                                      double fx, double fy, int w, int h) {
        int cx = b.x + (int) (b.width * fx);
        int cy = b.y + (int) (b.height * fy);
        c.setBounds(cx - w / 2, cy - h / 2, w, h);
    }

    // --------------------------------------------------------------- painting

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        Rectangle b = bookBounds();
        Image img = spreads[current];
        if (img != null) {
            g2.drawImage(img, b.x, b.y, b.width, b.height, this);
        } else {
            // Placeholder until the art exists.
            g2.setColor(new Color(0xEBD7A5));
            g2.fillRoundRect(b.x, b.y, b.width, b.height, 40, 40);
            g2.setColor(new Color(0x6B4A2B));
            g2.setStroke(new BasicStroke(4f));
            g2.drawRoundRect(b.x, b.y, b.width, b.height, 40, 40);
        }
        g2.dispose();
    }

    // ----------------------------------------------------------- arrow button

    /** Arrow hit area; only draws a triangle when DRAW_ARROWS is true. */
    private static class ArrowButton extends JButton {
        private final boolean pointsLeft;

        ArrowButton(boolean pointsLeft, String tooltip) {
            this.pointsLeft = pointsLeft;
            setToolTipText(tooltip);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (!DRAW_ARROWS) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            int[] xs = pointsLeft ? new int[]{w - 4, 4, w - 4} : new int[]{4, w - 4, 4};
            int[] ys = {2, h / 2, h - 2};
            g2.setColor(getModel().isRollover() ? new Color(0xC0843F) : new Color(0x8B5A2B));
            g2.fillPolygon(xs, ys, 3);
            g2.setColor(new Color(0x3B2410));
            g2.setStroke(new BasicStroke(2f));
            g2.drawPolygon(xs, ys, 3);
            g2.dispose();
        }
    }
}