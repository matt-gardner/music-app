package com.gardner.soundengine.staves;

/**
 * A canvas to draw on.  We'll be drawing bitmaps, mostly, and a few lines.  So we just need a few
 * draw methods, and that's it.
 */
public interface StaffCanvas {
    public void getWidth();
    public void getHeight();
    public void getPixel(int x, int y);
    public void setPixel(int x, int y, int r, int g, int b);
    public void drawStaticImage(StaticImage image, int top, int left);
    public void drawHorizontalLine(int y, int thickness, int length);
    public void drawVerticalLine(int x, int thickness, int height);
}
