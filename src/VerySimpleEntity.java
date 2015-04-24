import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Created by sxh112430 on 4/18/15.
 */
public class VerySimpleEntity {

    public String text;
    public boolean textIsImageURL;
    public boolean textIsScript;
    public boolean textIsStyle;
    public BufferedImage bufferedImage;

    public VerySimpleEntity(String text, boolean isImage, boolean textIsScript, boolean textIsStyle) {
            /*
            if it's an image, use text as a src URL
             */
        this.text = text.trim();
        this.textIsImageURL = isImage;
        this.textIsScript = textIsScript;
        this.textIsStyle = textIsStyle;
    }

    public Component toJLabel() {
        Component label;
        if (textIsImageURL && bufferedImage != null) {
            label = new JLabel(new ImageIcon(bufferedImage));
            ((JLabel) label).setAlignmentX(0);
        } else if (textIsScript || textIsStyle || text.trim().length() == 0) {
            label = null;
        } else {
            label = new JTextArea(text.trim());
            ((JTextArea)label).setWrapStyleWord(true);
            ((JTextArea)label).setLineWrap(true);
            ((JTextArea) label).setMaximumSize(new Dimension(560, Integer.MAX_VALUE));
            ((JTextArea) label).setAlignmentX(0);
            ((JTextArea) label).setEditable(false);
            ((JTextArea) label).setBorder(new EmptyBorder(5,5,5,5));
        }
        return label;
    }
}

