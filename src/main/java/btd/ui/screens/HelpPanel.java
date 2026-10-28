package btd.ui.screens;

import btd.util.ImageLoader;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextArea;

public class HelpPanel extends JPanel {

    /*
    Add to imports later after getting bg
    import java.awt.Graphics;
    import java.awt.Image;
    import java.io.File;
    import java.io.IOException;
    import javax.imageio.ImageIO;
    */

    //private static final String BG_PATH = "data/images/help_bg.png";
    //private Image background;
    private static final Font TEXT_FONT = new Font("Iosevka Charon Mono", Font.BOLD, 16);
    private static final Color TEXT_COLOR = Color.BLACK;
    private static final String HELP_TEXT = """
            HOW TO PLAY

            lorem ipsum dolor sit amet consectetur adipiscing 
            elit mollit duis consequat deserunt sit pariatur laboris 
            quidem distinctio et est in possimus omnis cum dolor esse 
            esse eos cupiditate omnis esse et aute deserunt fuga dolore 
            non nam atque accusamus culpa excepteur fugiat cupiditate 
            blanditiis quas optio consequatur dolorum exercitation ut
        
            """;

    public HelpPanel() {
        super(new BorderLayout());

        //background = ImageIO.read(new File(BG_PATH));

        JTextArea text = new JTextArea(HELP_TEXT);
        text.setEditable(false);
        text.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        //text.setOpaque(false);
        text.setFont(TEXT_FONT);
        text.setForeground(TEXT_COLOR);

        JButton closeButton = new JButton(new ImageIcon(
                ImageLoader.load("btn_close_x.png").getScaledInstance(45, 46, Image.SCALE_SMOOTH)));
        closeButton.setBorderPainted(false);
        closeButton.setContentAreaFilled(false);
        closeButton.setMargin(new Insets(0, 0, 0, 0));
        closeButton.setToolTipText("Close How to Play");
        closeButton.addActionListener(e -> setVisible(false));

        JPanel header = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 8));
        header.setBackground(text.getBackground());
        header.add(closeButton);

        add(header, BorderLayout.NORTH);
        add(text, BorderLayout.CENTER);

        setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 3));
        setVisible(false);
    }
    /* 
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(background, 0, 0, getWidth(), getHeight(), this);
    }
    */
}
