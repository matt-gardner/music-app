package com.gardner.soundengine.staves;

/**
 * A provider of bitmap images for creating a music staff.  This is an interface so I can minimize
 * the amount of code that has to be written in the various platforms.  Each platform (such as
 * Android or desktop Java) will have to implement this interface (and StaticImage and
 * StaffCanvas), and then the StaffDrawer will just use the interface.
 */
public interface StaffImageUtil {
    public StaticImage getTrebleClef(StaffSize size);
    public StaticImage getBaseClef(StaffSize size);

    public StaticImage getWholeNote(StaffSize size);
    public StaticImage getHalfNote(StaffSize size);
    public StaticImage getQuarterNote(StaffSize size);
    public StaticImage getEighthNote(StaffSize size);
    public StaticImage getSixteenthNote(StaffSize size);
    public StaticImage getThirtySecondNote(StaffSize size);

    public StaticImage getQuarterRest(StaffSize size);
    public StaticImage getEighthRest(StaffSize size);
    public StaticImage getHalfRest(StaffSize size);


    public StaticImage loadImageFile(String filename);
    public StaticImage scaleImage(StaticImage image, double scaleFactor);
}
