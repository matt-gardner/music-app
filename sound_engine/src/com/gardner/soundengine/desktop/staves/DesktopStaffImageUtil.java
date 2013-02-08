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
    public StaticImage getTrebleClef(StaffSize size) {
        // Really, this should load separate images depending on size.  But for now we'll just
        // scale stuff.
        StaticImage clef = loadImageFile("images/large/treble_clef.png");
        clef = scaleImage(clef, size.getTrebleClefHeight() / (double) clef.getHeight());
        return clef;
    }

    @Override
    public StaticImage getBaseClef(StaffSize size) {
        StaticImage clef = loadImageFile("images/large/base_clef.png");
        clef = scaleImage(clef, size.getStaffHeight() / (double) clef.getHeight());
        return clef;
    }

    @Override
    public StaticImage getQuarterNoteHead(StaffSize size) {
        throw new UnsupportedOperationException();
    }

    @Override
    public StaticImage getHalfNoteHead(StaffSize size) {
        throw new UnsupportedOperationException();
    }

    @Override
    public StaticImage getWholeNoteHead(StaffSize size) {
        throw new UnsupportedOperationException();
    }

    @Override
    public StaticImage getQuarterRest(StaffSize size) {
        throw new UnsupportedOperationException();
    }

    @Override
    public StaticImage getEighthRest(StaffSize size) {
        throw new UnsupportedOperationException();
    }

    @Override
    public StaticImage getHalfRest(StaffSize size) {
        throw new UnsupportedOperationException();
    }

    @Override
    public StaticImage getQuarterNoteStem(StaffSize size) {
        throw new UnsupportedOperationException();
    }

    @Override
    public StaticImage getEighthNoteStem(StaffSize size) {
        throw new UnsupportedOperationException();
    }

    @Override
    public StaticImage getSixteenthNoteStem(StaffSize size) {
        throw new UnsupportedOperationException();
    }

    @Override
    public StaticImage getThirtySecondNoteStem(StaffSize size) {
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
