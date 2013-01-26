package com.gardner.soundengine.staves;

/**
 * A bitmap image (probably of some part of a staff).  See the note in StaffImages about why we
 * have this as an interface (really, it's because Android uses a Bitmap object, and Java uses a
 * BufferedImage, and they are incompatible).  So we just use this interface, and implement the
 * interface with a small amount of platform-specific code.
 */
public interface StaticImage {
    public void getWidth();
    public void getHeight();
    public void getPixel(int x, int y);
}
