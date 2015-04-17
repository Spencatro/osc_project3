import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;

import static java.awt.EventQueue.*;

/**
 * Created by sxh112430 on 4/11/15.
 */
public class PageLoader extends Thread {

    private String myUrl;
    private JPanel myRenderPanel;
    private JLabel statusLabel;

    public PageLoader(String url, final JPanel renderPanel, final JLabel statusLabel) {
        this.myUrl = url;
        this.myRenderPanel = renderPanel;
        this.statusLabel = statusLabel;
    }

    public PageLoader(String url) {
        this.myUrl = url;
    }

    public static ByteArrayOutputStream[] loadUrl(String url) throws IOException {

        System.out.println("makin a url");
        URL urlObj = new URL(url);
        String getString = urlObj.getPath();
        if(getString.equals("")) {
            getString = "/";
        }
        String urlRoot = urlObj.getHost();
        int portNum = urlObj.getPort();
        if (portNum == -1) {
            portNum = 80;
        }

        System.out.println(urlRoot+","+getString+","+portNum);

        System.out.println("made the url, makin a socket");

        Socket socket = new Socket(InetAddress.getByName(urlRoot), portNum);
        System.out.println("sport"+socket.getPort());

        System.out.println("Mad a socket");

        DataOutputStream socketOut = null;
        try {
            socketOut = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        socketOut.writeBytes("GET " + getString + " HTTP/1.1\r\n");
        socketOut.writeBytes("Host: " + urlRoot + "\r\n");
        socketOut.writeBytes("Connection: close\r\n\r\n");
        socketOut.flush();

        InputStream dataIn = null;

        try {
            dataIn = socket.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }

        int contentIdx;
        int headerIdx;

        byte[] contentBuffer = new byte[2048];
        byte[] headerBuffer = new byte[2048];

        boolean endOfHeader = false;

        ByteArrayOutputStream contentStream = new ByteArrayOutputStream();
        ByteArrayOutputStream headerStream = new ByteArrayOutputStream();

        int indexOfEOH = -1;


        while ((contentIdx = dataIn.read(contentBuffer)) != -1) {
            headerIdx = contentIdx;
            if (!endOfHeader) {
                for (int idx = 0; idx < contentBuffer.length - 4; idx++) {
                    if (contentBuffer[idx] == '\r' && contentBuffer[idx + 1] == '\n' && contentBuffer[idx + 2] == '\r' && contentBuffer[idx + 3] == '\n') {
                        indexOfEOH = idx + 4;
                    }
                }
                if (indexOfEOH != -1) {
                    // the endOfHeader was reached
                    contentBuffer = Arrays.copyOfRange(contentBuffer, indexOfEOH, contentBuffer.length);
                    contentIdx = contentIdx - indexOfEOH;
                    headerBuffer = Arrays.copyOfRange(headerBuffer, 0, indexOfEOH);
                    headerIdx = headerBuffer.length;
                    endOfHeader = true;
                } else {
                    // this solely belongs to header
                    contentIdx = 0;
                    headerBuffer = contentBuffer;
                    headerIdx = headerBuffer.length;
                }
            } else {
                headerIdx = 0;
            }
            String test = new String(contentBuffer, 0, contentIdx);
//            System.out.println("Adding string:");
//            System.out.println(test);
//            System.out.println("======================================");
            headerStream.write(headerBuffer, 0, headerIdx);
            headerStream.flush();
            contentStream.write(contentBuffer, 0, contentIdx);
            contentStream.flush();
        }
        headerStream.close();
        contentStream.close();

//        dataIn.close();
//        socketOut.close();

        /*System.out.println("CONTENT STRING DUMP");
        System.out.println("\n\n\n\n\n\n\n");
        System.out.println(contentStream.toString());
        System.out.println("\n\n\n\n\n\n\n");*/

        return new ByteArrayOutputStream[]{headerStream, contentStream};
    }

    public static BufferedImage loadImage(String url) throws IOException {
        //TODO: this
        ByteArrayOutputStream[] boutArr = loadUrl(url);

        String header = boutArr[0].toString();

        if (header.indexOf("image/") == -1) {
            System.out.println("PageLoader WARNING: Requested image, but server returned something else!");
        }

        InputStream imageStream = new ByteArrayInputStream(boutArr[1].toByteArray());
        return ImageIO.read(imageStream);
    }

    @Override
    public void run() {
        // load the page here
        ByteArrayOutputStream[] pageLoadResults = new ByteArrayOutputStream[2];
        try {
            pageLoadResults = loadUrl(this.myUrl);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String contentType = pageLoadResults[0].toString();
        if (!contentType.contains("text/")) {
            //TODO: Handle this error
        }
        String html = pageLoadResults[1].toString();

        XMLParser pageParser = new XMLParser(html, this.myUrl);//TODO: fix rootUrl, probably not correct
        try {
            pageParser.parse();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        final ArrayList<XMLParser.VerySimpleEntity> parseResults = pageParser.getParseList();


        /*
        Now, need to get BufferedImages for each Image in parseResults
         */
        for (XMLParser.VerySimpleEntity entity : parseResults) {
            if (entity.textIsImageURL) {
                statusLabel.setText("Loading " + entity.text);
                boolean success = true;
                try {
                    entity.bufferedImage = loadImage(entity.text);
                } catch (Exception e) {
                    System.out.println("PageLoader ERROR loading image: " + entity.text);
                    success = false;
                }
                if (!success) {
                    // do not attempt to display images that won't load
                    entity.textIsImageURL = false;
                    entity.text = "Unloadable image: " + entity.text;
                }
            }
        }

        if (myRenderPanel != null) {
            // do GUI specific stuff here
            Runnable updateUI = new Runnable() {
                @Override
                public void run() {
                    for (XMLParser.VerySimpleEntity entity : parseResults) {
                        JLabel labelToInsert = entity.toJLabel();
                        myRenderPanel.add(labelToInsert);
                        statusLabel.setText("Idle");
                        myRenderPanel.revalidate();
                        myRenderPanel.repaint();
                    }
                }
            };

            try {
                invokeAndWait(updateUI);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }
}
