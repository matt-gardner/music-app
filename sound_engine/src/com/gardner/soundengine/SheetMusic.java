package com.gardner.soundengine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a piece of SheetMusic, corresponding to a MusicXML file or some other similar
 * notation.
 *
 * Currently pretty deficient, as I don't need anything more than a list of notes.  But it will get
 * better.
 */
public class SheetMusic {
    private final List<MusicNote> notes;

    public SheetMusic(List<MusicNote> notes) {
        this.notes = notes;
    }

    public List<MusicNote> getNotes() {
        return notes;
    }

    // TODOLATER: make a SheetMusicFactory class, subclass it to read from a text file
    public static SheetMusic readFromFile(File file) {
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
        return new SheetMusic(notes);
    }
}
