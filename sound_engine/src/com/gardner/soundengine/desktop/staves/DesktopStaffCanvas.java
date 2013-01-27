package com.gardner.soundengine.desktop.staves;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import com.gardner.soundengine.staves.*;

public class DesktopStaffCanvas extends JPanel implements StaffCanvas {
    private BufferedImage image;
    private Color currentColor;

    public DesktopStaffCanvas(int width, int height) {
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    }

    @Override
    public int getWidth() {
        return image.getWidth();
    }

    @Override
    public int getHeight() {
        return image.getHeight();
    }

    @Override
    public void paintComponent(Graphics g) {
        g.drawImage(image, 0, 0, null);
    }

    @Override
    public void setColor(int rgb) {
        currentColor = new Color(rgb);
    }

    @Override
    public int getPixel(int x, int y) {
        return image.getRGB(x, y);
    }

    @Override
    public void setPixel(int x, int y, int rgb) {
        image.setRGB(x, y, rgb);
    }

    @Override
    public void drawStaticImage(StaticImage imageToDraw, int top, int left) {
        BufferedImage i = ((DesktopStaticImage) imageToDraw).getBufferedImage();
        System.out.println("drawing image with width " + i.getWidth() + " and height " +
                i.getHeight());
        Graphics g = image.getGraphics();
        g.setColor(currentColor);
        g.drawImage(i, top, left, i.getWidth(), i.getHeight(), null);
        g.dispose();
    }

    @Override
    public void drawLine(int startX, int startY, int endX, int endY) {
        Graphics g = image.getGraphics();
        g.setColor(currentColor);
        g.drawLine(startX, startY, endX, endY);
        g.dispose();
    }

    @Override
    public void drawRect(int x, int y, int width, int height) {
        Graphics g = image.getGraphics();
        g.setColor(currentColor);
        g.fillRect(x, y, width, height);
        g.dispose();
    }
}
