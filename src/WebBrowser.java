import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;

/**
 * Created by sxh112430 on 4/11/15.
 */
public class WebBrowser {

    private String startingURL = "";
    private boolean modeIsGUI = true;
    private JFrame frame;
    private JPanel mainPanel;
    private JPanel renderPanel;
    private JScrollPane scrollPane;

    class ResizeListener extends ComponentAdapter {
        public void componentResized(ComponentEvent e) {
            for (Component comp : renderPanel.getComponents()) {
                if (comp instanceof JTextArea) {
                    ((JTextArea) comp).setMaximumSize(new Dimension(frame.getWidth() - 40, Integer.MAX_VALUE));
                    comp.revalidate();
                    comp.repaint();
                }
            }
        }
    }

    public WebBrowser(String args[]) {

        if (args.length > 0) {
            startingURL = args[0];
        }

        if (args.length > 1) {
            if (args[1].equals("cli")) {
                modeIsGUI = false;
            }
        }

        if (modeIsGUI) {
            frame = new JFrame("BrowserForm");
            BrowserForm bf = new BrowserForm(startingURL);
            mainPanel = bf.mainPanel;
            scrollPane = bf.scrollPane;
            renderPanel = bf.renderPanel;
            frame.addComponentListener(new ResizeListener());
            frame.setContentPane(bf.mainPanel);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setPreferredSize(new Dimension(600, 800));
            frame.setMinimumSize(new Dimension(300, 400));
            frame.setTitle("Spencer's GUI Web Browser");
            frame.pack();
            frame.setVisible(true);
        } else {
            String html = "";
            String header = "";
            try {
                ByteArrayOutputStream[] results = PageLoader.loadUrl(startingURL);
                header = results[0].toString();
                html = results[1].toString();
            } catch (IOException e) {
                e.printStackTrace();
            }

            int httpCode = PageLoader.getHTTPCode(header);
            String codeDescription = PageLoader.getHTTPCodeDescription(header);

            String contentType = PageLoader.getContentType(header);

            if (httpCode > 299 || httpCode < 200) {
                //report the error
                System.out.println("HTTP Error: "+httpCode+" "+codeDescription);
            } else {

                if (contentType.contains("image/")) {
                    //this is a single image, just print the url and quit
                    System.out.println("Image: "+startingURL);
                    return;
                }

                XMLParser parser = new XMLParser(html, startingURL);
                try {
                    parser.parse();
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                ArrayList<VerySimpleEntity> parseResults = parser.getParseList();

                for (VerySimpleEntity entity : parseResults) {
                    if (entity.text.equals(""))
                        continue;
                    if (entity.textIsImageURL) {
                        System.out.println("Image: " + entity.text);
                    } else if (!entity.textIsScript && !entity.textIsStyle) {
                        System.out.println(entity.text);
                    }
                }
            }
        }

    }

    public static void main(String[] args) {
        WebBrowser wb = new WebBrowser(args);
    }
}
