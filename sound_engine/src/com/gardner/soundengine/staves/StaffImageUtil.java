package com.gardner.soundengine.staves;

/**
 * A provider of bitmap images for creating a music staff.  This is an interface so I can minimize
 * the amount of code that has to be written in the various platforms.  Each platform (such as
 * Android or desktop Java) will have to implement this interface (and StaticImage and
 * StaffCanvas), and then the StaffDrawer will just use the interface.
 */
public interface StaffImageUtil {
    public StaticImage getTrebleClef();
    public StaticImage getBaseClef();

    public StaticImage getQuarterNoteHead();
    public StaticImage getHalfNoteHead();
    public StaticImage getWholeNoteHead();

    public StaticImage getQuarterRest();
    public StaticImage getEighthRest();
    public StaticImage getHalfRest();

    public StaticImage getQuarterNoteStem();
    public StaticImage getEighthNoteStem();
    public StaticImage getSixteenthNoteStem();
    public StaticImage getThirtySecondNoteStem();

    public StaticImage loadImageFile(String filename);
    public StaticImage scaleImage(StaticImage image, double scaleFactor);
}
