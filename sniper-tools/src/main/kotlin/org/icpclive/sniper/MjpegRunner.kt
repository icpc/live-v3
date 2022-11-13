package org.icpclive.sniper;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;

import javax.imageio.ImageIO;

/**
 * Given an extended JPanel and URL read and create BufferedImages to be displayed from a MJPEG stream
 *
 * @author shrub34 Copyright 2012
 * Free for reuse, just please give me a credit if it is for a redistributed package
 */
public class MjpegRunner implements Runnable {
    private static final String CONTENT_LENGTH = "Content-Length: ";
    private static final String CONTENT_TYPE = "Content-Type: image/jpeg";
    private MJpegViewer viewer;
    private InputStream urlStream;
    private StringWriter stringWriter;
    private boolean processing = true;
    private long last;

    public MjpegRunner(MJpegViewer viewer, URL url) throws IOException {
        this.viewer = viewer;
        URLConnection urlConn = url.openConnection();
        // change the timeout to taste, I like 1 second
        urlConn.setReadTimeout(10000);
        urlConn.connect();
        urlStream = urlConn.getInputStream();
        stringWriter = new StringWriter(128);
    }

    /**
     * Stop the loop, and allow it to clean up
     */
    public synchronized void stop() {
        processing = false;
    }

    /**
     * Keeps running while process() returns true
     * <p>
     * Each loop asks for the next JPEG image and then sends it to our JPanel to draw
     *
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        while (processing) {
            try {
//                System.out.println("try");
                byte[] imageBytes = retrieveNextImage();
                ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
//
//                ImageReader reader = ImageIO.getImageReadersByMIMEType("image/jpeg").next();
//                ImageReadParam param = reader.getDefaultReadParam();
//                ImageInputStream stream = ImageIO.createImageInputStream(bais);
//                reader.setInput(stream, true, true);
//                BufferedImage image;
//                try {
//                    image = reader.read(0, param);
//                } finally {
//                    reader.dispose();
//                    stream.close();
//                }
                if (System.currentTimeMillis() > last + 1000) {
                    BufferedImage image = ImageIO.read(bais);
//                System.out.println(image);
                    if (image != null) {
                        viewer.setBufferedImage(image);
                        viewer.repaint();
                    }
                    last = System.currentTimeMillis();
                }
            } catch (SocketTimeoutException ste) {
                System.err.println("failed stream read: " + ste);
                viewer.setFailedString("Lost Camera connection: " + ste);
                viewer.repaint();
                stop();
            } catch (IOException e) {
                System.err.println("failed stream read: " + e);
                stop();
            }
        }

        System.err.println("Stopping");
        // close streams
        try {
            urlStream.close();
        } catch (IOException ioe) {
            System.err.println("Failed to close the stream: " + ioe);
        }
    }

    /**
     * Using the urlStream get the next JPEG image as a byte[]
     *
     * @return byte[] of the JPEG
     * @throws IOException
     */
    private byte[] retrieveNextImage() throws IOException {
        boolean haveHeader = false;
        int currByte = -1;

        String header = null;
        // build headers
        // the DCS-930L stops it's headers

        while ((currByte = urlStream.read()) > -1 && !haveHeader) {
//            System.out.println(Integer.toHexString(currByte & 255));
            stringWriter.write(currByte);

            String tempString = stringWriter.toString();

            if (tempString.endsWith("\r\n\r\n")) {
                haveHeader = true;
                header = tempString;
            }
        }

        // 255 indicates the start of the jpeg image
        int lastByte = -1;
        while (true) {
            if (lastByte == 255 && currByte == 0xd8) {
                break;
            }
            lastByte = currByte;
            currByte = urlStream.read();
//            System.out.println(Integer.toHexString(currByte & 255));
            // just skip extras
        }

        // rest is the buffer
        int contentLength = contentLength(header);
        byte[] imageBytes = new byte[contentLength + 1];
        // since we ate the original 255 , shove it back in
        imageBytes[0] = (byte) 255;
        imageBytes[1] = (byte) 0xd8;
        int offset = 2;
        int numRead = 0;
        while (offset < imageBytes.length
                && (numRead = urlStream.read(imageBytes, offset, imageBytes.length - offset)) >= 0) {
            offset += numRead;
        }

//        for (int i = 0; i < 10; i++) {
//            System.out.println(Integer.toHexString(imageBytes[i] & 255));
//        }

        stringWriter = new StringWriter(128);

        return imageBytes;
    }

    // dirty but it works content-length parsing
    private static int contentLength(String header) {
        int indexOfContentLength = header.indexOf(CONTENT_LENGTH);
        int valueStartPos = indexOfContentLength + CONTENT_LENGTH.length();
        int indexOfEOL = header.indexOf('\n', indexOfContentLength);

//        System.out.println(header);
//        System.out.println(valueStartPos + " " + indexOfEOL);

        String lengthValStr = header.substring(valueStartPos, indexOfEOL).trim();

        int retValue = Integer.parseInt(lengthValStr);

        return retValue;
    }
}