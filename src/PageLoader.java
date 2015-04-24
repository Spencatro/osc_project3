import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Array;
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
        if (!url.startsWith("http")) {
            url = "http://" + url;
        }
        URL urlObj = new URL(url);
        String getString = urlObj.getPath();
        if (getString.equals("")) {
            getString = "/";
        }
        String urlRoot = urlObj.getHost();
        int portNum = urlObj.getPort();
        if (portNum == -1) {
            portNum = 80;
        }

        Socket socket = new Socket(InetAddress.getByName(urlRoot), portNum);

        DataOutputStream socketOut = null;
        try {
            socketOut = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        socketOut.writeBytes("GET " + getString + " HTTP/1.1\r\n");
        socketOut.writeBytes("Host: " + urlRoot + ":" + portNum + "\r\n");
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
                        // make sure to take the first one! break now
                        break;
                    }
                }
                if (indexOfEOH != -1) {
                    // the endOfHeader was reached
                    // pull header out first
                    headerBuffer = Arrays.copyOfRange(contentBuffer, 0, indexOfEOH);
                    headerIdx = indexOfEOH;

                    // now cut off the header from the rest of the content
                    contentBuffer = Arrays.copyOfRange(contentBuffer, indexOfEOH, contentIdx);
                    contentIdx = contentIdx - indexOfEOH;
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
//            String test = new String(contentBuffer, 0, contentIdx);
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
            System.out.println(url);
            System.out.println(header);
        }

        InputStream imageStream = new ByteArrayInputStream(boutArr[1].toByteArray());
        return ImageIO.read(imageStream);
    }

    public static String getContentType(String header) {
        String[] lines = header.split("\n");
        for (String line : lines) {
            if (line.toLowerCase().contains("content-type")) {
                //if it contains a charset, cut it out of the result
                int endIdx = (line.indexOf(";") == -1 ? line.length() : line.indexOf(";"));
                String ctype = line.substring(line.indexOf(":") + 1, endIdx);
                return ctype;
            }
        }

        return "";
    }

    public static int getHTTPCode(String header) {
        String[] lines = header.split("\n");
        for (String line : lines) {
            if (line.toLowerCase().contains("http/")) {
                String[] lineSplit = line.split(" ");
                int code = Integer.parseInt(lineSplit[1]);
                return code;
            }
            break;
        }
        return 200;
    }

    public static String getHTTPCodeDescription(String header) {
        String[] lines = header.split("\n");
        String returnVal = "";
        for (String line : lines) {
            if (line.toLowerCase().contains("http/")) {
                String[] lineSplit = line.split(" ");
                for(int i = 2; i < lineSplit.length; i++) {
                    returnVal += " "+lineSplit[i];
                }
                return returnVal;
            }
            break;
        }
        return "Unknown error!";
    }

    @Override
    public void run() {
        if (this.myRenderPanel == null) {
            System.out.println("PageLoader ERROR: If not using this class to modify a JPanel, only use it as a static class!!");
            return;
        }
        // load the page here
        ByteArrayOutputStream[] pageLoadResults = new ByteArrayOutputStream[2];
        try {
            pageLoadResults = loadUrl(this.myUrl);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String contentType = getContentType(pageLoadResults[0].toString());
        int httpCode = getHTTPCode(pageLoadResults[0].toString());

        if (httpCode > 299 || httpCode < 200) {
            //report the error
            final VerySimpleEntity singleEntity = new VerySimpleEntity("HTTP Error code: " + httpCode, false, false, false);


            if (myRenderPanel != null) {
                statusLabel.setText("Error loading " + this.myUrl);
                // do GUI specific stuff here
                Runnable updateUI = new Runnable() {
                    @Override
                    public void run() {
                        Component labelToInsert = singleEntity.toJLabel();
                        if (labelToInsert != null) {
                            myRenderPanel.add(labelToInsert);

                        }
                        myRenderPanel.revalidate();
                        myRenderPanel.repaint();
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
        } else if (contentType.contains("image/")) {
            //TODO: This is an image, render it that way

            final ArrayList<VerySimpleEntity> parseResults = new ArrayList<>();
            VerySimpleEntity singleEntity = new VerySimpleEntity(this.myUrl, true, false, false);

            parseResults.add(singleEntity);

            statusLabel.setText("Loading " + this.myUrl);
            try {
                singleEntity.bufferedImage = loadImage(this.myUrl);
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("PageLoader ERROR loading image: " + singleEntity.text);
                singleEntity.textIsImageURL = false;
                singleEntity.text = "Unloadable image: " + singleEntity.text;
            }

            if (myRenderPanel != null) {
                // do GUI specific stuff here
                Runnable updateUI = new Runnable() {
                    @Override
                    public void run() {
                        for (VerySimpleEntity entity : parseResults) {
                            Component labelToInsert = entity.toJLabel();
                            if (labelToInsert != null) {
                                myRenderPanel.add(labelToInsert);
                            }
                        }
                        statusLabel.setText("Idle");
                        myRenderPanel.revalidate();
                        myRenderPanel.repaint();
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
        } else if (contentType.contains("text/")) {
            // this is text, do normal render process
            String html = pageLoadResults[1].toString();

            XMLParser pageParser = new XMLParser(html, this.myUrl);//TODO: fix rootUrl, probably not correct
            try {
                pageParser.parse();
            } catch (ParseException e) {
                e.printStackTrace();
            }
            final ArrayList<VerySimpleEntity> parseResults = pageParser.getParseList();


            /*
            Now, need to get BufferedImages for each Image in parseResults
             */
            for (VerySimpleEntity entity : parseResults) {
                if (entity.textIsImageURL) {
                    statusLabel.setText("Loading " + entity.text);
                    boolean success = true;
                    try {
                        if (entity.text.contains("\n")) {
                            String[] parts = entity.text.split("\n");
                            if (parts.length == 3) {
                                entity.text = parts[0].trim() + parts[2].trim();
                            }
                        }
                        entity.bufferedImage = loadImage(entity.text);
                    } catch (Exception e) {
                        System.out.println("PageLoader ERROR loading image: " + entity.text);
                        e.printStackTrace();
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
                        for (VerySimpleEntity entity : parseResults) {
                            Component labelToInsert = entity.toJLabel();
                            if (labelToInsert != null) {
                                myRenderPanel.add(labelToInsert);
                            }
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
        } else {
            //TODO: display an error
        }
    }
}
