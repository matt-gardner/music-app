package com.gardner.soundengine;

import java.nio.ByteBuffer;
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
    private int bitRate;
    private int bytesPerFrame;
    private int dataSize;
    private int minMagnitude;

    private byte[] buffer;
    private int[] data;
    private int[] prevData;
    private double[] averages;
    private double[] prevAverages;
    private double[] samples;
    private DoubleFFT_1D fft;

    private double[] testing;
    private double[] testing2;
    private ArrayList<Double> peaks;

    // With a sample rate of 44100, if we do windows in increments of 256 steps, we get a
    // resolution of 1s / 44100 * 256 ~= 6 ms.  32nd notes at 240 beats per minute take about 31
    // milliseconds, and humans can distinguish sounds up to about 10 milliseconds.  So this
    // resolution should be plenty adequate.
    int stftBufferSize;
    int windowStepSize;
    int numWindows;
    private DoubleFFT_1D stft;

    private int timeStep;

    public SoundEngine(Microphone microphone) {
        this.microphone = microphone;
        microphone.initialize();
        sampleRate = microphone.getSampleRate();
        bufferSize = microphone.getBufferSize();
        bitRate = microphone.getBitRate();
        bytesPerFrame = microphone.getBytesPerFrame();
        minMagnitude = microphone.getMinimumMagnitude();
        dataSize = bufferSize / bytesPerFrame;
        buffer = new byte[bufferSize];
        data = new int[dataSize];
        prevData = new int[dataSize];
        averages = new double[dataSize];
        prevAverages = new double[dataSize];
        samples = new double[dataSize*2];
        fft = new DoubleFFT_1D(dataSize);
        currentFrequency = 0.0;
        currentMags = new double[dataSize/2];
        // Default to A4 at 440Hz
        initializeNoteFrequencies(440);
        timeStep = 0;

        stftBufferSize = 512;
        windowStepSize = stftBufferSize / 2;
        numWindows = dataSize / windowStepSize - 1;
        stft = new DoubleFFT_1D(stftBufferSize);
    }

    /**
     * Basic microphone sampling.  Reads bytes into this.buffer, performs some processing on the
     * audio input, then returns true if it was successful, false otherwise.
     */
    public boolean sampleMic() {
        System.arraycopy(data, 0, prevData, 0, dataSize);
        System.arraycopy(averages, 0, prevAverages, 0, dataSize);
        int bytes = microphone.sample(buffer);
        copyBufferToData();
        if (bytes == bufferSize) {
            timeStep += 1;
            processSample();
            double seconds = timeStep * 1.0 / sampleRate * dataSize;
            System.out.println("Frequency at " + seconds + ": "
                    + currentFrequency);
            return true;
        }
        return false;
    }

    private void copyBufferToData() {
        if (bitRate == 8) {
            for (int i=0; i<bufferSize; i++) {
                data[i] = buffer[i];
            }
        } else {
            ByteBuffer b = ByteBuffer.wrap(buffer);
            for (int i=0; i<dataSize; i++) {
                if (bitRate == 16) {
                    data[i] = b.getShort();
                } else {
                    throw new RuntimeException("Unsupported bit rate: " + bitRate);
                }
            }
        }
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public int getDataSize() {
        return dataSize;
    }

    public int getBytesPerFrame() {
        return bytesPerFrame;
    }

    public void start() {
        microphone.start();
    }

    public void stop() {
        microphone.stop();
    }

    public int[] getRawSignal() {
        return data;
    }

    // For outputing whatever I want to view for testing purposes
    public double[] getTestingSignal() {
        return testing;
    }

    // For outputing whatever I want to view for testing purposes (second line)
    public double[] getTestingSignal2() {
        return testing2;
    }

    private void processSample() {
        analyzePitch();
        findNoteOnsets();
        //findNoteOnsetsWithSTFT();
    }

    private void findNoteOnsetsWithSTFT() {
        System.out.println("Finding note onsets");
        testing = new double[numWindows];
        double[] tmpData;
        double[][] matrix = new double[numWindows][stftBufferSize/2];
        for (int i=0; i<numWindows; i++) {
            // Copy the relevant bytes from data into tmpData
            tmpData = new double[stftBufferSize*2];
            for (int j=0; j<stftBufferSize; j++) {
                tmpData[2*j] = data[i*windowStepSize+j];
                tmpData[2*j+1] = 0;
            }
            doFFT(tmpData, true);
            double energy_est = 0.0;
            double diff = 0.0;
            for (int j=0; j<stftBufferSize/4; j++) {
                double freq = sampleRate * i / stftBufferSize;
                double mag = Math.sqrt(tmpData[2*i]*tmpData[2*i] + tmpData[2*i+1]*tmpData[2*i+1]);
                matrix[i][j] = mag;
                energy_est += matrix[i][j];
                if (i > 0) {
                    double d = matrix[i][j] - matrix[i-1][j];
                    diff += d * d;
                }
            }
            testing[i] = diff / (stftBufferSize / 2) / 10000;
            System.out.println("Energy at " + i + ": " + testing[i]);
        }
    }

    private double adjustSignal(double s) {
        if (s == 0.0) return s;
        return Math.log(Math.abs(s));
    }

    private void findNoteOnsets() {
        double[] derivs = new double[dataSize];
        averages = new double[dataSize];
        testing = new double[dataSize];
        testing2 = new double[dataSize];
        int windowSize = 200;
        double sum = 0.0;
        int compareTo = 1500;
        for (int i=dataSize-windowSize; i<dataSize; i++) {
            sum += adjustSignal(prevData[i]);
        }
        double prev = 0;
        double maxDeriv = 1.5;
        double minDeriv = -1.5;
        double maxIndex = -1;
        double minIndex = -1;
        for (int i=0; i<dataSize; i++) {
            sum += adjustSignal(data[i]);
            int j = i - windowSize;
            if (j >= 0) {
                sum -= adjustSignal(data[j]);
            } else {
                sum -= adjustSignal(prevData[dataSize + j]);
            }
            averages[i] = sum / windowSize;
            int l = i - compareTo;
            if (l >= 0) {
                prev = averages[l];
            } else {
                prev = prevAverages[dataSize+l];
            }
            derivs[i] = (averages[i] - prev);
            testing2[i] = derivs[i]*10;
            testing[i] = averages[i];
            if (derivs[i] > maxDeriv) {
                maxDeriv = derivs[i];
                maxIndex = i;
            }
            if (derivs[i] < minDeriv) {
                minDeriv = derivs[i];
                minIndex = i;
            }
        }
        if (maxIndex != -1) {
            double seconds = (timeStep * dataSize + maxIndex) * 1.0 / sampleRate;
            System.out.println("Possible note begin at " + seconds);
        }
        if (minIndex != -1) {
            double seconds = (timeStep * dataSize + minIndex) * 1.0 / sampleRate;
            System.out.println("Possible note end at " + seconds);
        }
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

    private void analyzePitch() {
        // These methods mostly all work on the object's state, so we don't need to pass too many
        // variables around
        copyDataToSamples();
        doFFT(samples);
        findFreqMagsAndPeaks();
        computeFrequencyFromPeaks();
    }

    private void copyDataToSamples() {
        for (int i=0; i<dataSize; i++) {
            // Half-wave rectification and log compression
            if (data[i] > 0) {
                samples[2*i] = Math.log(data[i]);
            } else {
                samples[2*i] = 0;
            }
            samples[2*i+1] = 0;
        }
    }

    private void doFFT(double[] array) {
        // Assume you want a full FFT if you don't pass in the sftf parameter
        doFFT(array, false);
    }

    private void doFFT(double[] array, boolean do_stft) {
        if (do_stft) {
            stft.complexForward(array);
        } else {
            fft.complexForward(array);
        }
    }

    private void findFreqMagsAndPeaks() {
        peaks = new ArrayList<Double>();

        int max_n = dataSize / 2;

        currentMags = new double[max_n*2];
        boolean in_peak = false;
        int first_bin = -1;
        boolean past_zero = false;
        ArrayList<Double> peak_mags = new ArrayList<Double>();
        ArrayList<Double> peak_freqs = new ArrayList<Double>();

        for (int i=0; i<max_n; i++) {
            double freq = sampleRate * i / dataSize;
            double mag = Math.sqrt(samples[2*i]*samples[2*i] + samples[2*i+1]*samples[2*i+1]);
            currentMags[2*i] = freq;
            currentMags[2*i+1] = mag;
            if (freq < noteFrequencies[0] || freq > noteFrequencies[noteFrequencies.length - 1]) {
                continue;
            }
            if (mag > minMagnitude && freq != 0) {
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
        double peak_freq = sampleRate * peak_bin / dataSize;
        return peak_freq;
    }

    private void computeFrequencyFromPeaks() {
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
