import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;

import static java.awt.EventQueue.*;

/**
 * Created by sxh112430 on 4/11/15.
 */
public class PageLoader extends Thread {

    private String myUrl;
    private JPanel myRenderPanel;

    public PageLoader(String url, final JPanel renderPanel) {
        this.myUrl = url;
        this.myRenderPanel = renderPanel;
    }

    @Override
    public void run() {

        System.out.println(myUrl);
        System.out.println(myRenderPanel);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Runnable updateUI = new Runnable() {
            @Override
            public void run() {
                JLabel testLabel = new JLabel("this a test");
                myRenderPanel.add(testLabel);
            }
        };

        try {
            invokeAndWait(updateUI);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        System.out.println("finished!");
    }
}
