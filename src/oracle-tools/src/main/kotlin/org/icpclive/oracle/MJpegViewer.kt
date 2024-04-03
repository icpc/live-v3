package org.icpclive.oracle

import java.awt.image.BufferedImage

interface MJpegViewer {
    fun setBufferedImage(image: BufferedImage)
    fun repaint()
    fun setFailedString(s: String)
}
