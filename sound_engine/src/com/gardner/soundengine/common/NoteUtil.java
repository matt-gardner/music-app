package com.gardner.soundengine.common;

import java.util.HashMap;
import java.util.Map;

public class NoteUtil {

    private static Map<String, Double> noteFrequencyMap;
    private static String[] noteNames;
    private static double[] noteFrequencies;
    private static String[] oneOctaveNames = new String[] {
        "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B",
    };
    private static double basePitch = 440;

    /**
     * Reset the base pitch for A4 to be freq.  Note that this is set initially to be 440,
     * the standard concert pitch in the US and UK.
     */
    public static void setBasePitch(double freq) {
        basePitch = freq;
        initializeNoteFrequencies(freq);
    }

    public static double getMinFrequency() {
        if (noteFrequencyMap == null) {
            initializeNoteFrequencies(basePitch);
        }
        return noteFrequencies[0];
    }

    public static double getMaxFrequency() {
        if (noteFrequencyMap == null) {
            initializeNoteFrequencies(basePitch);
        }
        return noteFrequencies[noteFrequencies.length-1];
    }

    /**
     * Get the frequency of the given note (formatted as seen in oneOctaveNames, above, with the
     * octave appended - e.g., A#3), or -1 if the note is not recognized or out of range.
     */
    public static double getNoteFrequency(String note) {
        if (noteFrequencyMap == null) {
            initializeNoteFrequencies(basePitch);
        }
        Double freq = noteFrequencyMap.get(note);
        if (freq == null) {
            return -1;
        }
        return freq;
    }

    /**
     * Find the named note that is closest to the given frequency, returning null if the frequency
     * is outside of the range we can handle.
     */
    public static String findClosestNote(double frequency) {
        if (noteFrequencyMap == null) {
            initializeNoteFrequencies(basePitch);
        }
        // Some bound checking first
        if (frequency > noteFrequencies[noteFrequencies.length-1] ||
                frequency < noteFrequencies[0]) {
            return null;
        }
        int min = 0;
        int max = noteFrequencies.length;
        int i = (max + min) / 2;
        while (true) {
            if (frequency > noteFrequencies[i]) {
                if (frequency - noteFrequencies[i] < noteFrequencies[i+1] - frequency) {
                    return noteNames[i];
                } else {
                    min = i;
                    i = (max + min) / 2;
                }
            } else if (frequency < noteFrequencies[i]) {
                if (noteFrequencies[i] - frequency < frequency - noteFrequencies[i-1]) {
                    return noteNames[i];
                } else {
                    max = i;
                    i = (max + min) / 2;
                }
            } else {
                return noteNames[i];
            }
        }
    }

    private static void initializeNoteFrequencies(double basePitch) {
        noteFrequencyMap = new HashMap<String, Double>();
        // Start A0 - really unlikely I'll need lower than that
        int octave = 0;
        // A7 is about as high as we are likely to need
        int maxOctave = 8;
        // The -9 here is because we're starting with A, not C
        noteFrequencies = new double[maxOctave * oneOctaveNames.length - 9];
        noteNames = new String[maxOctave * oneOctaveNames.length - 9];
        // This is 2^(1/12)
        double multiplier = 1.0594630943592953;
        double currentPitch = basePitch;
        // Take the base pitch (A4), and divide by 16 to get A0
        currentPitch /= 16;
        int currentNote = 9;
        int i = 0;
        while (octave < 8) {
            noteFrequencies[i] = currentPitch;
            noteNames[i] = oneOctaveNames[currentNote] + octave;
            noteFrequencyMap.put(noteNames[i], currentPitch);
            currentPitch *= multiplier;
            i++;
            currentNote++;
            if (currentNote == oneOctaveNames.length) {
                octave++;
                currentNote = 0;
            }
        }
    }
}
