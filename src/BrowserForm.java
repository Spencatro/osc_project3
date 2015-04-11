import sun.plugin.converter.util.CommandLine;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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

    public BrowserForm() {
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
    }

    private void clearRenderPanel() {
        // leave the spacer, but delete everything else
        while(renderPanel.getComponents().length > 0)
            renderPanel.remove(0);
    }

    private void getURLFromUI() {
        String url = urlField.getText();
        if(!url.startsWith("http")) {
            url = "http://"+url;
        }
        System.out.println("url is: "+url);
        currentURL = url;
        urlField.setText(currentURL);

        clearRenderPanel();
        count *=2;
        for(int i = 0; i < count; i++) {
            JLabel testLabel = new JLabel("test"+i);
            renderPanel.add(testLabel);
        }

        renderPanel.revalidate();

        //PageLoader loader = new PageLoader("test",renderPanel);
        //loader.start();
    }

}
