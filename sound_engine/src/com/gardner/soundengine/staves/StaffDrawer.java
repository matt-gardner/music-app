package com.gardner.soundengine.staves;

public class StaffDrawer {

    public void drawStaff(StaffCanvas canvas, StaticImage clef, StaffImageUtil util) {
        canvas.setColor(0); // 0 should be black
        int height = canvas.getHeight();
        int width = canvas.getWidth();
        // We assume that the staff lines are going to take up half of the canvas height, and 95%
        // of the canvas width.
        int staffHeight = height / 2;
        int staffWidth = (int) (width * .95);
        int staffStartX = (width - staffWidth) / 2;
        int staffTop = (height - staffHeight) / 2;
        // Five lines per staff
        int lineSpacing = staffHeight / 4;
        int lineThickness = lineSpacing / 6;
        for (int i=0; i<5; i++) {
            canvas.drawRect(staffStartX, staffTop + i*lineSpacing, staffWidth, lineThickness);
        }
        canvas.drawRect(staffStartX, staffTop, lineThickness, staffHeight);
        clef = util.scaleImage(clef, staffHeight / (double) clef.getHeight());
        canvas.drawStaticImage(clef, staffTop, staffStartX);
    }
}
