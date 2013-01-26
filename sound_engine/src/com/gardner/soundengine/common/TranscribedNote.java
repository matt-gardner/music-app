package com.gardner.soundengine.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a note as found in an audio signal.  This is what SoundEngine produces.
 */
public class TranscribedNote {
    private final int startWindow;
    private final int endWindow;
    private final double startTime; // in seconds from beginning of piece
    private final double endTime; // in seconds from beginning of piece
    private final double pitch;

    public TranscribedNote(int startWindow, int endWindow, double startTime, double endTime,
            double pitch) {
        this.startWindow = startWindow;
        this.endWindow = endWindow;
        this.startTime = startTime;
        this.endTime = endTime;
        this.pitch = pitch;
    }

    public int getStartWindow() {
        return startWindow;
    }

    public int getEndWindow() {
        return endWindow;
    }

    public double getStartTime() {
        return startTime;
    }

    public double getEndTime() {
        return endTime;
    }

    public double getDuration() {
        return endTime - startTime;
    }

    public double getPitch() {
        return pitch;
    }

    public static List<TranscribedNote> readNotesFromFile(File file) {
        List<TranscribedNote> notes = new ArrayList<TranscribedNote>();
        double pitch = 0.0;
        double onset = 0.0;
        double end = 0.0;
        String line;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            while ((line = reader.readLine()) != null) {
                if (line.equals("")) {
                    notes.add(new TranscribedNote(-1, -1, onset, end, pitch));
                } else if (line.startsWith("Pitch")) {
                    pitch = Double.parseDouble(line.split(": ")[1]);
                } else if (line.startsWith("Onset")) {
                    onset = Double.parseDouble(line.split(": ")[1]);
                } else if (line.startsWith("End")) {
                    end = Double.parseDouble(line.split(": ")[1]);
                }
            }
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
        return notes;
    }
}
