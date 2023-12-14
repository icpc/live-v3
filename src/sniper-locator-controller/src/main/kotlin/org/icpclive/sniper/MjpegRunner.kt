package org.icpclive.sniper

import java.io.*
import java.net.SocketTimeoutException
import java.net.URL
import javax.imageio.ImageIO

/**
 * Given an extended JPanel and URL read and create BufferedImages to be displayed from a MJPEG stream
 *
 * @author shrub34 Copyright 2012
 * Free for reuse, just please give me a credit if it is for a redistributed package
 */
class MjpegRunner(private val viewer: MJpegViewer, url: URL) : Runnable {
    private val urlStream: InputStream
    private var stringWriter: StringWriter
    private var processing = true
    private var last: Long = 0

    init {
        val urlConn = url.openConnection()
        // change the timeout to taste, I like 1 second
        urlConn.readTimeout = 10000
        urlConn.connect()
        urlStream = urlConn.getInputStream()
        stringWriter = StringWriter(128)
    }

    /**
     * Stop the loop, and allow it to clean up
     */
    @Synchronized
    fun stop() {
        processing = false
    }

    /**
     * Keeps running while process() returns true
     *
     *
     * Each loop asks for the next JPEG image and then sends it to our JPanel to draw
     *
     * @see java.lang.Runnable.run
     */
    override fun run() {
        while (processing) {
            try {
                val imageBytes = retrieveNextImage()
                val bais = ByteArrayInputStream(imageBytes)
                if (System.currentTimeMillis() > last + 1000) {
                    val image = ImageIO.read(bais)
                    if (image != null) {
                        viewer.setBufferedImage(image)
                        viewer.repaint()
                    }
                    last = System.currentTimeMillis()
                }
            } catch (ste: SocketTimeoutException) {
                System.err.println("failed stream read: $ste")
                viewer.setFailedString("Lost Camera connection: $ste")
                viewer.repaint()
                stop()
            } catch (e: IOException) {
                System.err.println("failed stream read: $e")
                stop()
            }
        }
        System.err.println("Stopping")
        // close streams
        try {
            urlStream.close()
        } catch (ioe: IOException) {
            System.err.println("Failed to close the stream: $ioe")
        }
    }

    /**
     * Using the urlStream get the next JPEG image as a byte[]
     *
     * @return byte[] of the JPEG
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun retrieveNextImage(): ByteArray {
        var haveHeader = false
        var currByte: Int
        var header: String? = null
        // build headers
        // the DCS-930L stops it's headers
        while (urlStream.read().also { currByte = it } > -1 && !haveHeader) {
            stringWriter.write(currByte)
            val tempString = stringWriter.toString()
            if (tempString.endsWith("\r\n\r\n")) {
                haveHeader = true
                header = tempString
            }
        }

        // 255 indicates the start of the jpeg image
        var lastByte = -1
        while (true) {
            if (lastByte == 255 && currByte == 0xd8) {
                break
            }
            lastByte = currByte
            currByte = urlStream.read()
            // just skip extras
        }

        // rest is the buffer
        val contentLength = contentLength(header)
        val imageBytes = ByteArray(contentLength + 1)
        // since we ate the original 255 , shove it back in
        imageBytes[0] = 255.toByte()
        imageBytes[1] = 0xd8.toByte()
        var offset = 2
        var numRead = 0
        while (offset < imageBytes.size
            && urlStream.read(imageBytes, offset, imageBytes.size - offset).also { numRead = it } >= 0
        ) {
            offset += numRead
        }

        stringWriter = StringWriter(128)
        return imageBytes
    }

    companion object {
        private const val CONTENT_LENGTH = "Content-Length: "
        private const val CONTENT_TYPE = "Content-Type: image/jpeg"

        // dirty but it works content-length parsing
        private fun contentLength(header: String?): Int {
            val indexOfContentLength = header!!.indexOf(CONTENT_LENGTH)
            val valueStartPos = indexOfContentLength + CONTENT_LENGTH.length
            val indexOfEOL = header.indexOf('\n', indexOfContentLength)

            val lengthValStr = header.substring(valueStartPos, indexOfEOL).trim { it <= ' ' }
            return lengthValStr.toInt()
        }
    }
}