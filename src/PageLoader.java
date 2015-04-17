import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.Socket;
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
        // start by cutting off http:// if it exists
        System.out.println("Loading url: " + url);
        if (url.startsWith("http://")) {
            url = url.substring(7);
        }

        int portNum = 80;

        if (url.contains(":")) {
            int colonIdx = url.indexOf(":");
            String port = url.substring(colonIdx);
            url = url.substring(0, colonIdx);
            try {
                portNum = Integer.parseInt(port);
            } catch (NumberFormatException e) {
                System.out.println("ERROR: port number was not parse-able! defaulting to port number 80");
            }
        }

        int firstSlash = url.indexOf("/");
        if (firstSlash == -1) {
            firstSlash = url.length();
        }
        String urlRoot = url.substring(0, firstSlash);
        String getString = url.substring(firstSlash, url.length());
        if (getString.length() == 0) {
            getString = "/";
        }

        Socket socket = new Socket(InetAddress.getByName(urlRoot), portNum);

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

        BufferedInputStream dataIn = null;

        try {
            dataIn = new BufferedInputStream(socket.getInputStream());
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
            System.out.println("Adding string:");
            System.out.println(test);
            System.out.println("======================================");
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
            System.out.println("WARNING: Requested image, but server returned something else!");
        }

        InputStream imageStream = new ByteArrayInputStream(boutArr[1].toByteArray());
        return ImageIO.read(imageStream);
    }

    @Override
    public void run() {
        System.out.println("loading now...");
        // load the page here
        ByteArrayOutputStream[] pageLoadResults = new ByteArrayOutputStream[2];
        try {
            pageLoadResults = loadUrl(this.myUrl);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Got results");
        System.out.println(pageLoadResults[0].toString());
        String contentType = pageLoadResults[0].toString();
        if (!contentType.contains("text/")) {
            //TODO: Handle this error
        }
        String html = pageLoadResults[1].toString();

        XMLParser pageParser = new XMLParser(html, this.myUrl);//TODO: fix rootUrl, probably not correct
        System.out.println("Parsing now");
        try {
            pageParser.parse();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        System.out.println("Finished parsing");
        final ArrayList<XMLParser.VerySimpleEntity> parseResults = pageParser.getParseList();


        System.out.println("Gathering buffered images");
        /*
        Now, need to get BufferedImages for each Image in parseResults
         */
        for (XMLParser.VerySimpleEntity entity : parseResults) {
            if (entity.textIsImageURL) {
                statusLabel.setText("Loading " + entity.text);
                boolean success = true;
                try {
                    System.out.println("Loading: " + entity.text);
                    entity.bufferedImage = loadImage(entity.text);
                } catch (Exception e) {
                    System.out.println("ERROR loading image: " + entity.text);
                    success = false;
                }
                if (!success) {
                    // do not attempt to display images that won't load
                    entity.textIsImageURL = false;
                    entity.text = "Unloadable image: " + entity.text;
                }
            }
        }

        System.out.println("Finished all! Going to update UI now...");
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
            System.out.println("called invokeAndwait");
        }
    }
}
