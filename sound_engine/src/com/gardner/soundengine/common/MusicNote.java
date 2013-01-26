package com.gardner.soundengine.common;

/**
 * Represents a music note as found in sheet music, or MusicXML.
 */
public class MusicNote {
    // Takes values like C4, B5, etc.
    // TODO: figure out the best way to handle rests
    private final String name;
    // A value of 1 here means 1 beat (typically a quarter note), .5 means half a beat (typically
    // an eighth note), and so on.
    private final double beats;
    // Not currently in use, and maybe should be an enum, or a class.  Takes values like
    // "staccato", "accented", "legato", and so on.
    //private final String articulation;
    // As above, not currently in use, and perhaps should be an enum.  Takes values like "piano",
    // "mezzo piano", etc.
    //private final String volume;

    public MusicNote(String name, double beats) {
        this.name = name;
        this.beats = beats;
    }

    public String getName() {
        return name;
    }

    public double getBeats() {
        return beats;
    }
}
