package com.gardner.soundengine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a music note as found in sheet music, or MusicXML.
 */
public class MusicNote {
    // Takes values like C4, B5, etc.
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

    public static List<MusicNote> readNotesFromFile(File file) {
        List<MusicNote> notes = new ArrayList<MusicNote>();
        double beats = 0.0;
        String name = null;
        String line;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            while ((line = reader.readLine()) != null) {
                if (line.equals("")) {
                    notes.add(new MusicNote(name, beats));
                } else if (line.startsWith("Name")) {
                    name = line.split(": ")[1];
                } else if (line.startsWith("Beats")) {
                    beats = Double.parseDouble(line.split(": ")[1]);
                }
            }
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
        return notes;
    }
}
