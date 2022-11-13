package org.icpclive.sniper;

import java.awt.image.BufferedImage;

public interface MJpegViewer {
    void setBufferedImage(BufferedImage image);

    void repaint();

    void setFailedString(String s);

}
