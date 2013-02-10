package com.gardner.soundengine.staves;

public class StaffDrawer {

    public void drawStaff(StaffCanvas canvas, StaffSize size, StaffImageUtil util) {
        canvas.setColor(0); // 0 should be black
        int staffHeight = size.getStaffHeight();
        int staffWidth = getStaffWidth(canvas, size);
        int staffStartX = getStaffStartX(canvas, size);
        int staffTop = getStaffTop(canvas, size);
        int lineSpacing = size.getLineSpacing();
        int lineThickness = size.getLineThickness();
        for (int i=0; i<5; i++) {
            canvas.drawRect(staffStartX, staffTop + i*lineSpacing, staffWidth, lineThickness);
        }
        canvas.drawRect(staffStartX, staffTop, lineThickness, staffHeight);
    }

    public void drawTrebleClefStaff(StaffCanvas canvas, StaffSize size, StaffImageUtil util) {
        drawStaff(canvas, size, util);
        int staffStartX = getStaffStartX(canvas, size);
        int clefTop = getStaffTop(canvas, size) - size.getTrebleClefOffset();
        canvas.drawStaticImage(util.getTrebleClef(size), clefTop, staffStartX);
    }

    public void drawQuarterNoteOnTrebleClef(StaffCanvas canvas, StaffSize size,
            StaffImageUtil util, String noteName, int xOffset) {
        drawNote(canvas, size, util.getQuarterNote(size), getLineOrSpace(noteName, "treble"),
                xOffset, size.getQuarterNoteYOffset());
    }

    public void drawNote(StaffCanvas canvas, StaffSize size, StaticImage note, int lineOrSpace,
            int xOffset, int noteYOffset) {
        int staffTop = getStaffTop(canvas, size);
        int staffHeight = size.getStaffHeight();
        int middle = staffTop + staffHeight / 2;
        int stepSize = size.getLineSpacing() / 2;
        int y = middle - stepSize * lineOrSpace - noteYOffset;
        canvas.drawStaticImage(note, y, getStaffStartX(canvas, size) + xOffset);
    }

    private int getStaffWidth(StaffCanvas canvas, StaffSize size) {
        return (int) (canvas.getWidth() * .95);
    }

    private int getStaffStartX(StaffCanvas canvas, StaffSize size) {
        return (canvas.getWidth() - getStaffWidth(canvas, size)) / 2;
    }

    private int getStaffTop(StaffCanvas canvas, StaffSize size) {
        return (canvas.getHeight() - size.getStaffHeight()) / 2;
    }

    /**
     * Return the number of note positions (lines or spaces) up or down from the middle line on the
     * staff.
     *
     * Note names must be uppercase.  We look at the first character (the base note name, minus
     * accidentals) and the last character (the octave number).
     */
    private int getLineOrSpace(String noteName, String clefName) {
        // I don't really like using clefName as a string here, it should probably be an enum, but
        // that's ok for now. TODOLATER
        if (clefName.equals("treble")) {
            // On a treble clef, B4 is the middle line.
            int noteOffset = noteName.charAt(0) - 'B';
            int octaveOffset = noteName.charAt(noteName.length()-1) - '4';
            if (noteOffset > 0) {
                octaveOffset -= 1;
            }
            System.out.println("Note: " + noteName + "; noteOffset: " + noteOffset +
                    "; octaveOffset: " + octaveOffset);
            return noteOffset + octaveOffset * 7;
        } else {
            throw new UnsupportedOperationException();
        }
    }
}
