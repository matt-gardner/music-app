package com.gardner.soundengine.desktop.staves;

import java.awt.image.BufferedImage;

import com.gardner.soundengine.staves.*;

/**
 * A StaticImage implementation that uses BufferedImage from AWT.
 */
public class DesktopStaticImage implements StaticImage {
    private BufferedImage image;

    public DesktopStaticImage(BufferedImage image) {
        this.image = image;
    }

    public BufferedImage getBufferedImage() {
        return image;
    }

    @Override
    public int getWidth() {
        return image.getWidth();
    }

    @Override
    public int getHeight() {
        return image.getHeight();
    }
}
