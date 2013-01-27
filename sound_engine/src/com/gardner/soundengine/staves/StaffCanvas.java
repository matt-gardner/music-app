package com.gardner.soundengine.staves;

/**
 * A canvas to draw on.  We'll be drawing bitmaps, mostly, and a few lines.  So we just need a few
 * draw methods, and that's it.
 */
public interface StaffCanvas {
    public int getWidth();
    public int getHeight();
    public int getPixel(int x, int y);
    public void setPixel(int x, int y, int rgb);
    public void setColor(int rgb);
    public void drawStaticImage(StaticImage image, int top, int left);
    public void drawLine(int startX, int startY, int endX, int endY);
    public void drawRect(int x, int y, int width, int height);
}
