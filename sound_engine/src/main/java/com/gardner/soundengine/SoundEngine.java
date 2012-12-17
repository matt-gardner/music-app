package com.gardner.soundengine;

import java.util.ArrayList;
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

    private void processSample() {
        for (int i=0; i<bufferSize; i++) {
            // Half-wave rectification and log compression
            if (buffer[i] > 0) {
                samples[2*i] = Math.log(buffer[i]);
            } else {
                samples[2*i] = 0;
            }
            samples[2*i+1] = 0;
        }
        doFFT(samples);
        ArrayList<Double> peaks = new ArrayList<Double>();

        double min_magnitude = 100;
        int max_n = bufferSize / 2;

        currentMags = new double[max_n*2];
        boolean in_peak = false;
        int first_bin = -1;
        boolean past_zero = false;
        ArrayList<Double> peak_mags = new ArrayList<Double>();
        ArrayList<Double> peak_freqs = new ArrayList<Double>();

        for (int i=0; i<max_n; i++) {
            double freq = sampleRate * i / bufferSize;
            double mag = Math.sqrt(samples[2*i]*samples[2*i] + samples[2*i+1]*samples[2*i+1]);
            currentMags[2*i] = freq;
            currentMags[2*i+1] = mag;
            if (freq < noteFrequencies[0] || freq > noteFrequencies[noteFrequencies.length - 1]) {
                continue;
            }
            if (mag > min_magnitude && freq != 0) {
                if (past_zero) {
                    if (!in_peak) {
                        in_peak = true;
                        first_bin = i;
                    }
                    peak_mags.add(mag);
                    peak_freqs.add(freq);
                }
            } else {
                past_zero = true;
                if (in_peak) {
                    if (peak_mags.size() > 1) {
                        peaks.add(computePeakFrequency(peak_mags, peak_freqs, first_bin));
                    }
                    peak_mags.clear();
                    peak_freqs.clear();
                    in_peak = false;
                    first_bin = -1;
                }
            }
        }
        if (peaks.size() == 0) {
            currentFrequency = 0.0;
        } else {
            if (peaks.size() > 1 && peaks.get(1) / peaks.get(0) > 4) {
                // Looks like a spurious low frequency peak
                peaks.remove(0);
            }
            int max_harmonic = 15;
            double[] harmonics = new double[max_harmonic];
            double[] off_values = new double[max_harmonic];
            double base = peaks.get(0);
            for (int i=0; i<peaks.size(); i++) {
                double peak = peaks.get(i);
                double multiple = peak / base;
                int mult = (int) (multiple + .5);
                double off = Math.abs(multiple - mult);
                if (mult >= max_harmonic) {
                    // Greater than max allowable harmonic; moving on
                    continue;
                }
                if (harmonics[mult] == 0.0) {
                    harmonics[mult] = peak;
                    off_values[mult] = off;
                } else {
                    if (off_values[mult] > off) {
                        // This might be a better estimate; replace previous peak
                        harmonics[mult] = peak;
                        off_values[mult] = off;
                    }
                }
            }
            double sum_x = 0.0;
            double sum_y = 0.0;
            double sum_xy = 0.0;
            double sum_x2 = 0.0;
            int count = 0;
            for (int i=0; i<max_harmonic; i++) {
                if (harmonics[i] == 0.0) {
                    continue;
                }
                count += 1;
                sum_x += i;
                sum_x2 += i*i;
                sum_y += harmonics[i];
                sum_xy += i*harmonics[i];
            }
            double calc_freq;
            if (count == 1) {
                calc_freq = sum_y;
            } else {
                calc_freq = (count * sum_xy - sum_x * sum_y) / (count * sum_x2 - sum_x * sum_x);
            }
            currentFrequency = calc_freq;
        }
    }

    private double computePeakFrequency(ArrayList<Double> mags, ArrayList<Double> freqs,
            int first_bin) {
        int max_index = -1;
        double max_mag = -1;
        for (int i=0; i<mags.size(); i++) {
            if (mags.get(i) > max_mag) {
                max_mag = mags.get(i);
                max_index = i;
            }
        }
        int i = max_index;
        if (i == 0 || i == mags.size() - 1) {
            return freqs.get(i);
        }
        double peak_bin = .5 * (mags.get(i-1) - mags.get(i+1)) /
            (mags.get(i-1) - 2 * mags.get(i) + mags.get(i+1)) + (first_bin + i);
        double peak_freq = sampleRate * peak_bin / bufferSize;
        return peak_freq;
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
