package btd.ui.screens;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class LandingPanel extends JPanel {

    public static final int WIDTH = 1280;
    public static final int HEIGHT = 720;

    private final Rectangle crate = new Rectangle(352, 340, 575, 250);

    private boolean opened = false;

    public LandingPanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!opened && crate.contains(e.getPoint())) {
                    opened = true;
                    repaint();
                }
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Background: light blue before the click, darker after
        g.setColor(opened ? new Color(40, 150, 170) : new Color(100, 230, 250));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // Crate placeholder
        g.setColor(new Color(240, 150, 60));
        g.fillRect(crate.x, crate.y, crate.width, crate.height);

        // Text so we can tell which state we are in
        g.setColor(Color.BLACK);
        g.drawString(opened ? "OPENED (screen 2)" : "CLICK TO OPEN (screen 1)", 560, 320);
    }

    // Temporary: lets us run this panel by itself to test it
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("CasesTD");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(new LandingPanel());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}