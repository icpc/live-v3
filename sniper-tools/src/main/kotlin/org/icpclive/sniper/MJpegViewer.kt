package org.icpclive.sniper

import java.awt.image.BufferedImage

interface MJpegViewer {
    fun setBufferedImage(image: BufferedImage?)
    fun repaint()
    fun setFailedString(s: String?)
}