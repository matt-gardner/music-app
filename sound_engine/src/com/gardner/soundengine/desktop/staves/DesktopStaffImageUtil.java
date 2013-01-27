package com.gardner.soundengine.desktop.staves;

import java.io.File;
import java.io.IOException;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import javax.imageio.ImageIO;

import com.gardner.soundengine.staves.*;

/**
 * An implementation of StaffImageUtil for use on a desktop machine (as opposed to Android, iOS, or
 * GWT).
 */
public class DesktopStaffImageUtil implements StaffImageUtil {
    @Override
    public StaticImage getTrebleClef() {
        return loadImageFile("images/large/treble_clef.png");
    }

    @Override
    public StaticImage getBaseClef() {
        return loadImageFile("images/large/base_clef.png");
    }

    @Override
    public StaticImage getQuarterNoteHead() {
        throw new UnsupportedOperationException();
    }

    @Override
    public StaticImage getHalfNoteHead() {
        throw new UnsupportedOperationException();
    }

    @Override
    public StaticImage getWholeNoteHead() {
        throw new UnsupportedOperationException();
    }

    @Override
    public StaticImage getQuarterRest() {
        throw new UnsupportedOperationException();
    }

    @Override
    public StaticImage getEighthRest() {
        throw new UnsupportedOperationException();
    }

    @Override
    public StaticImage getHalfRest() {
        throw new UnsupportedOperationException();
    }

    @Override
    public StaticImage getQuarterNoteStem() {
        throw new UnsupportedOperationException();
    }

    @Override
    public StaticImage getEighthNoteStem() {
        throw new UnsupportedOperationException();
    }

    @Override
    public StaticImage getSixteenthNoteStem() {
        throw new UnsupportedOperationException();
    }

    @Override
    public StaticImage getThirtySecondNoteStem() {
        throw new UnsupportedOperationException();
    }

    @Override
    public StaticImage loadImageFile(String filename) {
        try {
            BufferedImage in = ImageIO.read(new File(filename));
            BufferedImage newImage = new BufferedImage(in.getWidth(), in.getHeight(),
                    BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = newImage.createGraphics();
            g.drawImage(in, 0, 0, null);
            g.dispose();
            return new DesktopStaticImage(in);
        } catch(IOException e) {
            return null;
        }
    }

    @Override
    public StaticImage scaleImage(StaticImage image, double scaleFactor) {
        int w = (int) (image.getWidth() * scaleFactor);
        int h = (int) (image.getHeight() * scaleFactor);
        BufferedImage scaledImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaledImage.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(((DesktopStaticImage) image).getBufferedImage(), 0, 0, w, h, null);
        g.dispose();
        return new DesktopStaticImage(scaledImage);
    }
}
