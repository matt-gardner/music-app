package com.gardner.soundengine;

import java.util.HashMap;
import java.util.Map;

import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;

public class SoundEngine {

    private Microphone microphone;

    private double currentFrequency;
    private double[] currentMags;

    private int sampleRate;
    private int bufferSize;
    private byte[] buffer;
    private double[] samples;
    private DoubleFFT_1D fft;

    public SoundEngine(Microphone microphone) {
        this.microphone = microphone;
        microphone.initialize();
        sampleRate = microphone.getSampleRate();
        bufferSize = microphone.getBufferSize();
        buffer = new byte[bufferSize];
        samples = new double[bufferSize*2];
        fft = new DoubleFFT_1D(bufferSize);
        currentFrequency = 0.0;
        currentMags = new double[bufferSize/2];
        // Default to A4 at 440Hz
        initializeNoteFrequencies(440);
    }

    /**
     * Basic microphone sampling.  Reads bytes into this.buffer, returns true if bytes were read,
     * false otherwise.
     */
    public boolean sampleMic() {
        int bytes = microphone.sample(buffer);
        if (bytes == bufferSize) {
            processSample();
            return true;
        }
        return false;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public void start() {
        microphone.start();
    }

    public void stop() {
        microphone.stop();
    }

    /////////////////////////////////////////////////////////////////////////
    // Basic FFT kinds of stuff, including getting frequencies and magnitudes
    /////////////////////////////////////////////////////////////////////////

    public double getCurrentFrequency() {
        return currentFrequency;
    }

    public double[] getCurrentMags() {
        return currentMags;
    }

    private double processSample() {
        for (int i=0; i<buffer.length; i++) {
            samples[2*i] = buffer[i];
            samples[2*i+1] = 0;
        }
        doFFT(samples);
        // If the magnitude is lower than 15,000, ignore it.
        double max_magnitude = 150;
        double max_freq = -1;
        // I would have thought I should only divide by two, but for some reason it looks like I
        // need to divide by 4.
        int max_n = buffer.length / 4;
        currentMags = new double[max_n*2];
        for (int i=0; i<max_n; i++) {
            double freq = sampleRate * i / buffer.length;
            double mag = Math.sqrt(samples[2*i]*samples[2*i] + samples[2*i+1]*samples[2*i+1]);
            currentMags[2*i] = freq;
            currentMags[2*i+1] = mag;
            if (mag > max_magnitude) {
                max_freq = freq;
                max_magnitude = mag;
            }
        }
        currentFrequency = max_freq;
        return max_freq;
    }

    private void doFFT(double[] samples) {
        fft.complexForward(samples);
    }

    /////////////////////////////////////////////////////////////////////////
    // Mapping of frequencies onto note names
    /////////////////////////////////////////////////////////////////////////

    private Map<String, Double> noteFrequencyMap;
    private String[] noteNames;
    private double[] noteFrequencies;

    private String[] oneOctaveNames = new String[] {
        "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B",
    };

    /**
     * Reset the base pitch for A4 to be freq.  Note that this is set in the constructor to be 440,
     * the standard concert pitch in the US and UK.
     */
    public void setBasePitch(int freq) {
        initializeNoteFrequencies(freq);
    }

    /**
     * Calls findClosestNote with currentFrequency
     */
    public String getCurrentClosestNote() {
        return findClosestNote(currentFrequency);
    }

    /**
     * Get the frequency of the given note (formatted as seen in oneOctaveNames, above, with the
     * octave appended - e.g., A#3), or -1 if the note is not recognized or out of range.
     */
    public double getNoteFrequency(String note) {
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
    public String findClosestNote(double frequency) {
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

    private void initializeNoteFrequencies(int basePitch) {
        noteFrequencyMap = new HashMap<String, Double>();
        // Start A0 - really unlikely I'll need lower than that
        int octave = 0;
        // A7 is about as high as you can handle when sampling at 8000Hz
        int maxOctave = 8;
        noteFrequencies = new double[maxOctave * oneOctaveNames.length - 9];
        noteNames = new String[maxOctave * oneOctaveNames.length - 9];
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
