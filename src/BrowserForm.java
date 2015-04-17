import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Created by sxh112430 on 4/10/15.
 */
public class BrowserForm {

    private String currentURL;
    private int count = 1;

    private JTextField urlField;
    private JLabel faviconLabel;
    private JPanel topPanel;
    public JPanel mainPanel;
    public JPanel renderPanel;
    private JScrollPane scrollPane;
    private JButton goButton;
    private JLabel statusLabel;

    public BrowserForm(String startingUrl) {
        goButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                getURLFromUI();
            }
        });
        urlField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                getURLFromUI();
            }
        });
        renderPanel.setLayout(new BoxLayout(renderPanel, BoxLayout.PAGE_AXIS));

        if(startingUrl != null) {
            currentURL = startingUrl;
            if(!currentURL.startsWith("http://")) {
                currentURL = "http://"+currentURL;
            }
            urlField.setText(currentURL);
            PageLoader pl = new PageLoader(currentURL, renderPanel, statusLabel);
            pl.start();
        }

    }

    private void clearRenderPanel() {
        // leave the spacer, but delete everything else
        while(renderPanel.getComponents().length > 0)
            renderPanel.remove(0);
    }

    private void getURLFromUI() {
        String url = urlField.getText();
        currentURL = url;
        urlField.setText(currentURL);

        clearRenderPanel();

        statusLabel.setText("Loading: " + currentURL);

        PageLoader pl = new PageLoader(currentURL, renderPanel, statusLabel);
        pl.start();

        renderPanel.revalidate();
        renderPanel.repaint();

    }

}
