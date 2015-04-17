import javax.swing.*;
import java.awt.*;
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
            BrowserForm bf = new BrowserForm(startingURL);
            frame.setContentPane(bf.mainPanel);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setPreferredSize(new Dimension(600, 800));
            frame.setMinimumSize(new Dimension(300,400));
            frame.setTitle("Spencer's GUI Web Browser");
            frame.pack();
            frame.setVisible(true);
        } else {
            // TODO: this
            String html = "";
            try {
                ByteArrayOutputStream[] results = PageLoader.loadUrl(startingURL);
                html = results[1].toString();
            } catch (IOException e) {
                e.printStackTrace();
            }
            XMLParser parser = new XMLParser(html, startingURL);
            try {
                parser.parse();
            } catch (ParseException e) {
                e.printStackTrace();
            }

            ArrayList<XMLParser.VerySimpleEntity> parseResults = parser.getParseList();

            for(XMLParser.VerySimpleEntity entity : parseResults) {
                if(entity.text.equals(""))
                    continue;
                if(entity.textIsImageURL) {
                    System.out.println("Image: "+entity.text);
                } else if(!entity.textIsScript && !entity.textIsStyle) {
                    System.out.println(entity.text);
                }
            }
        }

    }

    public static void main(String[] args) {
        WebBrowser wb = new WebBrowser(args);
    }
}
