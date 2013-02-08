package com.gardner.soundengine.staves;

public class StaffDrawer {

    public void drawStaff(StaffCanvas canvas, StaffSize size, StaffImageUtil util) {
        canvas.setColor(0); // 0 should be black
        int staffHeight = size.getStaffHeight();
        int staffWidth = (int) (canvas.getWidth() * .95);
        int staffStartX = (canvas.getWidth() - staffWidth) / 2;
        int staffTop = (canvas.getHeight() - staffHeight) / 2;
        // Five lines per staff
        int lineSpacing = staffHeight / 4;
        int lineThickness = size.getLineThickness();
        for (int i=0; i<5; i++) {
            canvas.drawRect(staffStartX, staffTop + i*lineSpacing, staffWidth, lineThickness);
        }
        canvas.drawRect(staffStartX, staffTop, lineThickness, staffHeight);
    }

    public void drawTrebleClefStaff(StaffCanvas canvas, StaffSize size, StaffImageUtil util) {
        drawStaff(canvas, size, util);
        int staffHeight = size.getStaffHeight();
        int staffWidth = (int) (canvas.getWidth() * .95);
        int staffStartX = (canvas.getWidth() - staffWidth) / 2;
        int clefTop = (canvas.getHeight() - staffHeight) / 2 - size.getTrebleClefOffset();
        canvas.drawStaticImage(util.getTrebleClef(size), clefTop, staffStartX);
    }
}
