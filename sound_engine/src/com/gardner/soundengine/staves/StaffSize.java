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

    public int getStaffHeight() {
        return staffHeight;
    }

    public int getLineThickness() {
        return lineThickness;
    }

    public int getTrebleClefHeight() {
        return trebleClefHeight;
    }

    public int getTrebleClefOffset() {
        return trebleClefOffset;
    }
}
