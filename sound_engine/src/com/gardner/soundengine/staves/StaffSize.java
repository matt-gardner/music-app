package com.gardner.soundengine.staves;

/**
 * An enumeration of possible staff sizes.  Mostly this is for handling different pixel densities.
 */
public abstract class StaffSize {

    public static final StaffSize SMALL = new SmallStaffSize();
    public static final StaffSize MEDIUM = new MediumStaffSize();
    public static final StaffSize LARGE = new LargeStaffSize();
    public static final StaffSize XLARGE = new ExtraLargeStaffSize();

    protected int staffHeight;
    protected int lineThickness;
    protected int trebleClefHeight;
    protected int trebleClefOffset;
    protected int noteHeight;
    protected int stemlessNoteHeight;
    protected int quarterNoteYOffset;

    public int getStaffHeight() {
        return staffHeight;
    }

    public int getLineThickness() {
        return lineThickness;
    }

    public int getLineSpacing() {
        // Five lines per staff
        return staffHeight / 4;
    }

    public int getTrebleClefHeight() {
        return trebleClefHeight;
    }

    public int getTrebleClefOffset() {
        return trebleClefOffset;
    }

    public int getQuarterNoteHeight() {
        return noteHeight;
    }

    public int getWholeNotHeight() {
        return stemlessNoteHeight;
    }

    public int getQuarterNoteYOffset() {
        return quarterNoteYOffset;
    }
}
