package com.gardner.soundengine.staves;

/**
 * A bitmap image (probably of some part of a staff).  See the note in StaffImages about why we
 * have this as an interface (really, it's because Android uses a Bitmap object, and Java uses a
 * BufferedImage, and they are incompatible).  So we just use this interface, and implement the
 * interface with a small amount of platform-specific code.
 *
 * Beacuse the drawing APIs for each platform will handle drawing an image onto a canvas, all we
 * really need here is to know the width and height, so our drawing library knows how much to scale
 * the image (if necessary).
 */
public interface StaticImage {
    public int getWidth();
    public int getHeight();
}
