package com.gardner.soundengine;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;

public class SoundEngine {

    private Microphone microphone;

    private double currentFrequency;
    private double[] currentMags;
    private int num_ffts;

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
    private List<Integer> fullSignal;

    private double[] testing;
    private double[] testing2;
    private ArrayList<Double> peaks;

    private List<List<Double>> spectrogram;
    int spectrogramWindowSize;
    int windowStepSize;
    int numWindows;
    private DoubleFFT_1D stft;
    private List<List<Integer>> noteBoundaries;
    private boolean withinNote;


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
        fullSignal = new ArrayList<Integer>();
        fft = new DoubleFFT_1D(dataSize);
        currentFrequency = 0.0;
        currentMags = new double[dataSize/2];
        // Default to A4 at 440Hz
        initializeNoteFrequencies(440);
        timeStep = 0;
        num_ffts = 0;

        // With a sample rate of 44100, if we do windows in increments of 256 steps, we get a
        // resolution of 1s / 44100 * 256 ~= 6 ms.  32nd notes at 240 beats per minute take about
        // 31 milliseconds, and humans can distinguish sounds up to about 10 milliseconds.  So this
        // resolution should be plenty adequate.
        spectrogramWindowSize = 512;
        windowStepSize = spectrogramWindowSize / 2;
        numWindows = dataSize / windowStepSize - 1;
        stft = new DoubleFFT_1D(spectrogramWindowSize);
        noteBoundaries = new ArrayList<List<Integer>>();
        withinNote = false;
        spectrogram = new ArrayList<List<Double>>();
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
            //System.out.println("Frequency at " + seconds + ": "
                    //+ currentFrequency);
            return true;
        }
        return false;
    }

    private void copyBufferToData() {
        if (bitRate == 8) {
            for (int i=0; i<bufferSize; i++) {
                data[i] = buffer[i];
                fullSignal.add(data[i]);
            }
        } else {
            ByteBuffer b = ByteBuffer.wrap(buffer);
            for (int i=0; i<dataSize; i++) {
                if (bitRate == 16) {
                    data[i] = b.getShort();
                } else {
                    throw new RuntimeException("Unsupported bit rate: " + bitRate);
                }
                fullSignal.add(data[i]);
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

    public int getDataNormalizer() {
        return (int) Math.pow(2, bitRate-1);
    }

    public int getBytesPerFrame() {
        return bytesPerFrame;
    }

    public int getNumFfts() {
        return num_ffts;
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

    public List<List<Double>> getSpectrogram() {
        return spectrogram;
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
        //analyzePitch();
        //findNoteOnsets();
        computeSpectrogram();
        findNoteOnsetsFromSpectrogram();
    }

    private void computeSpectrogram() {
        double[] tmpData = new double[spectrogramWindowSize*2];
        for (int i=0; i<numWindows; i++) {
            List<Double> column = new ArrayList<Double>();
            int start = i*windowStepSize;
            int end = start + spectrogramWindowSize;
            copyDataForFft(data, tmpData, start, end);
            doFFT(tmpData, true);
            for (int j=0; j<spectrogramWindowSize/2; j++) {
                double freq = sampleRate * j / (spectrogramWindowSize / 2);
                double mag = Math.sqrt(tmpData[2*j]*tmpData[2*j] + tmpData[2*j+1]*tmpData[2*j+1]);
                column.add(mag);
            }
            spectrogram.add(column);
        }
    }

    private int windowNum = 0;

    private void findNoteOnsetsFromSpectrogram() {
        // If a window corresponds to 6 milliseconds, then this means anything less than about 25
        // milliseconds will be ignored.
        int minNoteSize = 4;
        int start = spectrogram.size() - numWindows;
        for (int i=start; i<spectrogram.size(); i++) {
            windowNum++;
            double total_mag = 0.0;
            for (int j=0; j<spectrogramWindowSize/2; j++) {
                total_mag += spectrogram.get(i).get(j);
            }
            double seconds = windowNum * (double) windowStepSize / sampleRate;
            System.out.println("i: " + windowNum + "; time: " + seconds + "; total_mag: "
                    + total_mag);
            /*
            ArrayList<Double> peaks = findFreqMagsAndPeaks(spectrogram.get(i), 1);
            for (double peak : peaks) {
                System.out.println("   peak at: " + peak);
            }
            */
            // TODO: make this a parameter, and find a good way to pick it
            if (total_mag > 400) {
                if (!withinNote) {
                    // TODO: check for min note size here, too, to disallow very short pauses.
                    // That needs some care, though.
                    withinNote = true;
                    ArrayList<Integer> note = new ArrayList<Integer>();
                    note.add(windowNum);
                    noteBoundaries.add(note);
                }
            } else {
                if (withinNote) {
                    withinNote = false;
                    List<Integer> note = noteBoundaries.get(noteBoundaries.size()-1);
                    int noteStart = note.get(0);
                    if (windowNum - noteStart < minNoteSize) {
                        noteBoundaries.remove(noteBoundaries.size()-1);
                    } else {
                        // TODO: try to split the note by pitch, as this just finds gaps in
                        // articulation and rests
                        note.add(windowNum);
                    }
                }
            }
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

    public List<List<Integer>> getNoteBoundaries() {
        return noteBoundaries;
    }

    private void analyzePitch() {
        // These methods mostly all work on the object's state, so we don't need to pass too many
        // variables around
        copyDataForFft(data, samples, 0, dataSize);
        doFFT(samples);
        // TODO: I'm taking a different approach, and so this is currently broken
        //findFreqMagsAndPeaks();
        //computeFrequencyFromPeaks();
    }

    private double[] windowWeights;

    /**
     * start and end are indices for the source array - the fft_array must be of size
     * (end - start) * 2
     */
    private void copyDataForFft(int[] source_array, double[] fft_array, int start, int end) {
        for (int i=0; i<(end-start); i++) {
            // Half-wave rectification and log compression
            if (source_array[i+start] > 0) {
                if (windowWeights == null || windowWeights.length != (end-start)) {
                    computeWindowWeights(end-start);
                }
                fft_array[2*i] = windowWeights[i] * Math.log(source_array[i+start]);
            } else {
                fft_array[2*i] = 0;
            }
            // Complex part of fft_array is zero
            fft_array[2*i+1] = 0;
        }
    }

    private void computeWindowWeights(int n) {
        windowWeights = new double[n];
        for (int i=0; i<n; i++) {
            double angle = 2 * Math.PI * i / (n - 1);
            windowWeights[i] = .54 - .46 * Math.cos(angle);
        }
    }

    private void doFFT(double[] array) {
        // Assume you want a full FFT if you don't pass in the sftf parameter
        doFFT(array, false);
    }

    private void doFFT(double[] array, boolean do_stft) {
        num_ffts++;
        if (do_stft) {
            stft.complexForward(array);
        } else {
            fft.complexForward(array);
        }
    }

    private ArrayList<Double> findFreqMagsAndPeaks(double[] mag_array, int min_mag) {
        ArrayList<Double> peaks = new ArrayList<Double>();

        boolean in_peak = false;
        int first_bin = -1;
        boolean past_zero = false;

        ArrayList<Double> peak_mags = new ArrayList<Double>();
        ArrayList<Double> peak_freqs = new ArrayList<Double>();

        int num_bins = mag_array.length * 2;

        for (int i=0; i<mag_array.length; i++) {
            double freq = sampleRate * i / num_bins;
            if (freq < noteFrequencies[0] || freq > noteFrequencies[noteFrequencies.length - 1]) {
                continue;
            }
            if (mag_array[i] > min_mag && freq != 0) {
                if (past_zero) {
                    if (!in_peak) {
                        in_peak = true;
                        first_bin = i;
                    }
                    peak_mags.add(mag_array[i]);
                    peak_freqs.add(freq);
                }
            } else {
                past_zero = true;
                if (in_peak) {
                    if (peak_mags.size() > 1) {
                        peaks.add(computePeakFrequency(peak_mags, peak_freqs, first_bin, num_bins));
                    }
                    peak_mags.clear();
                    peak_freqs.clear();
                    in_peak = false;
                    first_bin = -1;
                }
            }
        }
        return peaks;
    }

    private double computePeakFrequency(ArrayList<Double> mags, ArrayList<Double> freqs,
            int first_bin, int num_bins) {
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
        double peak_freq = sampleRate * peak_bin / num_bins;
        return peak_freq;
    }

    private double computeFrequencyFromPeaks(ArrayList<Double> peaks) {
        if (peaks.size() == 0) {
            return 0.0;
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
            double freq;
            if (count == 1) {
                freq = sum_y;
            } else {
                freq = (count * sum_xy - sum_x * sum_y) / (count * sum_x2 - sum_x * sum_x);
            }
            return freq;
        }
    }

    /////////////////////////////////////////////////////////////////////////////////
    // Mapping of frequencies onto note names - maybe should go into a separate class
    /////////////////////////////////////////////////////////////////////////////////

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

    /////////////////////////////////////////////////////////////////////////
    // Old stuff that I will probably delete soon
    /////////////////////////////////////////////////////////////////////////

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

}
