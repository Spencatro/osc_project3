import javax.swing.*;
import java.awt.*;

/**
 * Created by sxh112430 on 4/11/15.
 */
public class WebBrowser {

    private String startingURL = "";
    private boolean modeIsGUI = true;

    public WebBrowser(String args[]) {

        if(args.length > 0) {
            startingURL = args[0];
        }

        if(args.length > 1) {
            if(args[1].equals("cli")) {
                modeIsGUI = false;
            }
        }

        if(modeIsGUI) {
            JFrame frame = new JFrame("BrowserForm");
            frame.setContentPane(new BrowserForm().mainPanel);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setPreferredSize(new Dimension(600, 800));
            frame.setMinimumSize(new Dimension(300,400));
            frame.pack();
            frame.setVisible(true);
        } else {
            // TODO: this
            System.out.println("CLI mode not yet implemented");
        }

    }

    public static void main(String[] args) {
        WebBrowser wb = new WebBrowser(args);
    }
}
