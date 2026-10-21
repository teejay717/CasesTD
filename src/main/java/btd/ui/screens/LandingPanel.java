package btd.ui.screens;

import btd.dao.PlayerDAO;
import btd.model.player.Player;
import btd.ui.theme.Theme;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;

public class LandingPanel extends JPanel {

    private final Rectangle crate = new Rectangle(352, 340, 575, 250);
    private final Rectangle play = new Rectangle(560, 170, 160, 160);
    private final JTextField nameField = new JTextField();
    private final Rectangle help = new Rectangle(830, 275, 100, 100);
    private final HelpPanel helpPanel = new HelpPanel();
    private final PlayerDAO playerDao = new PlayerDAO();
    private Player player;
    private final Runnable onPlay; 

    private boolean opened = false;

    public LandingPanel(Runnable onPlay) {
        this.onPlay = onPlay;
        setPreferredSize(new Dimension(Theme.CONTENT_WIDTH, Theme.CONTENT_HEIGHT));

        setLayout(null);
        nameField.setBounds(490, 235, 300, 40);
        nameField.setHorizontalAlignment(SwingConstants.CENTER);
        nameField.setFont(Theme.BUTTON_FONT);
        add(nameField);

        helpPanel.setBounds(240, 110, 800, 500);
        add(helpPanel);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!opened && crate.contains(e.getPoint()) && !getPlayerName().isEmpty()) {
                    openCrate();
                } else if (opened && play.contains(e.getPoint())) {
                    onPlay.run();
                } else if (opened && help.contains(e.getPoint())) {
                    helpPanel.setVisible(!helpPanel.isVisible());
                }
            }
        });
    }

    private void openCrate() {
        String name = getPlayerName();
        if (name.length() > 50) {
            JOptionPane.showMessageDialog(this, "Name must be 50 characters or less.");
            return;
        }
        try {
            player = playerDao.findOrCreate(name);
        } catch (SQLException | IllegalStateException ex) {
            JOptionPane.showMessageDialog(this, "Could not load your profile: " + ex.getMessage());
            return;
        }
        opened = true;
        nameField.setVisible(false);
        repaint();
    }

    public String getPlayerName() {
        return nameField.getText().trim();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Background: light blue before the click, darker after
        g.setColor(opened ? new Color(40, 150, 170) : new Color(100, 230, 250));
        g.fillRect(0, 0, Theme.CONTENT_WIDTH, Theme.CONTENT_HEIGHT);

        // Crate placeholder
        g.setColor(new Color(240, 150, 60));
        g.fillRect(crate.x, crate.y, crate.width, crate.height);

        if (opened) {
            g.setColor(Theme.YELLOW_COLOR);
            g.fillRect(play.x, play.y, play.width, play.height);
        }

        if (opened) {
        g.setColor(Color.CYAN);
        g.fillOval(help.x, help.y, help.width, help.height);
        g.setColor(Color.BLACK);
        g.drawString("?", help.x + 48, help.y + 55);
}

        // player info from db
        if (opened) {
            g.setColor(Color.BLACK);
            g.drawString("Player: " + player.getPlayerName(), 330, 400);
            g.drawString("Highest score: " + player.getHighestScore(), 540, 50);
        }

        // Text so we can tell which state we are in
        g.setColor(Color.BLACK);
        g.drawString(opened ? "OPENED (screen 2)" : "CLICK TO OPEN (screen 1)", 560, 320);
    }

}