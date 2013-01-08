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
    private DoubleFFT_1D fft;
    private int fftSize;
    private List<Integer> fullSignal;

    private double[] testing;
    private double[] testing2;
    private ArrayList<Double> peaks;

    private List<List<Double>> spectrogram;
    int spectrogramWindowSize;
    int windowStepSize;
    private int windowNum;
    private DoubleFFT_1D stft;
    private int stftSize;
    private List<TranscribedNote> transcribedNotes;

    // These three need to be class variables because notes may span buffer reads from the mic
    private boolean withinNote;
    private int startWindow;
    private int endWindow;

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
        fullSignal = new ArrayList<Integer>();
        fft = new DoubleFFT_1D(dataSize);
        fftSize = dataSize;
        currentFrequency = 0.0;
        currentMags = new double[dataSize/2];
        // Default to A4 at 440Hz
        initializeNoteFrequencies(440);
        num_ffts = 0;

        // With a sample rate of 44100, if we do windows in increments of 256 steps, we get a
        // resolution of 1s / 44100 * 256 ~= 6 ms.  32nd notes at 240 beats per minute take about
        // 31 milliseconds, and humans can distinguish sounds up to about 10 milliseconds.  So this
        // resolution should be plenty adequate.
        windowStepSize = 256;
        // We want to spectrogram to have a good enough resolution to differentiate half steps;
        // this is right about on the border of that.
        spectrogramWindowSize = 2048;
        stft = new DoubleFFT_1D(spectrogramWindowSize);
        stftSize = spectrogramWindowSize;
        transcribedNotes = new ArrayList<TranscribedNote>();
        withinNote = false;
        startWindow = -1;
        endWindow = -1;
        spectrogram = new ArrayList<List<Double>>();
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

    public int getSpectrogramWindowSize() {
        return spectrogramWindowSize;
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

    public List<Integer> getRawSignal() {
        return fullSignal;
    }

    public List<List<Double>> getSpectrogram() {
        return spectrogram;
    }

    public List<TranscribedNote> getTranscribedNotes() {
        return transcribedNotes;
    }

    /**
     * Basic microphone sampling.  Reads bytes into this.buffer, performs some processing on the
     * audio input, then returns true if it was successful, false otherwise.
     */
    public boolean sampleMic() {
        int bytes = microphone.sample(buffer);
        copyBufferToData();
        if (bytes == bufferSize) {
            processSample();
            return true;
        }
        return false;
    }

    private void copyBufferToData() {
        if (bitRate == 8) {
            for (int i=0; i<bufferSize; i++) {
                fullSignal.add((int) buffer[i]);
            }
        } else {
            ByteBuffer b = ByteBuffer.wrap(buffer);
            for (int i=0; i<dataSize; i++) {
                if (bitRate == 16) {
                    fullSignal.add((int) b.getShort());
                } else {
                    throw new RuntimeException("Unsupported bit rate: " + bitRate);
                }
            }
        }
    }

    private void processSample() {
        int windowsPerSample = dataSize / windowStepSize;
        for (int i=0; i<windowsPerSample; i++) {
            int firstFrame = windowNum * windowStepSize;
            int lastFrame = firstFrame + spectrogramWindowSize - 1;
            if (lastFrame > fullSignal.size()) {
                // This should happen on the first call to processSample, when we don't have enough
                // data to do the full windowsPerSample number of spectrograms.
                break;
            }
            computeSpectrogram();
            findNoteOnsetsFromSpectrogram();
            windowNum++;
        }
    }

    private void computeSpectrogram() {
        int start = windowNum*windowStepSize;
        int end = start + spectrogramWindowSize;
        double[] fftArray = copyDataForFft(start, end);
        doFFT(fftArray);
        List<Double> column = new ArrayList<Double>();
        // Only the first half of the fft contains useful data
        for (int j=0; j<spectrogramWindowSize/2; j++) {
            double mag = Math.sqrt(fftArray[2*j]*fftArray[2*j]+fftArray[2*j+1]*fftArray[2*j+1]);
            column.add(mag);
        }
        spectrogram.add(column);
    }

    private void findNoteOnsetsFromSpectrogram() {
        // If a window corresponds to 6 milliseconds, then this means anything less than about 25
        // milliseconds will be ignored.
        int minNoteSize = 4; // PARAMTODO
        double total_mag = 0.0;
        for (int j=0; j<spectrogramWindowSize/2; j++) {
            total_mag += spectrogram.get(windowNum).get(j);
        }
        double seconds = windowNum * (double) windowStepSize / sampleRate;
        if (total_mag > 400) { // PARAMTODO
            if (!withinNote) {
                startWindow = windowNum;
            }
            withinNote = true;
        } else {
            // This fancy logic tries to account for temporary dips below the magnitude threshold
            // that are less than minNoteSize long.
            if (withinNote) {
                if (endWindow == -1) {
                    endWindow = windowNum;
                } else if (windowNum - endWindow > minNoteSize) {
                    if (endWindow - startWindow >= minNoteSize) {
                        for (TranscribedNote note : splitNotes(startWindow, windowNum)) {
                            transcribedNotes.add(note);
                        }
                    }
                    withinNote = false;
                    endWindow = -1;
                }
            }
        }
    }

    /**
     * Looks through the spectrogram, from startWindow to endWindow, and see if there's a likely
     * place to split the notes.  We start from the beginning, and if there is a place to split, we
     * call this recursively, moving startWindow to the place of the split.
     */
    private List<TranscribedNote> splitNotes(int startWindow, int endWindow) {
        int vectorSize = spectrogram.get(0).size();
        int averageCount = 0;
        int compareCount = 0;
        double[] averageVector = new double[vectorSize];
        double[] compareVector = new double[vectorSize];
        int window = startWindow;
        // First we get an initial average for the starting note.
        while (window < startWindow+5) { // PARAMTODO
            double[] vector = getNormalizedSpectrogramVector(window);
            for (int i=0; i<vectorSize; i++) {
                averageVector[i] += (vector[i] - averageVector[i]) / (averageCount + 1);
            }
            averageCount++;
            window++;
        }
        // Then we get an initial average for the (possible) second note
        int compareWindow = window;
        while (compareWindow < window+5 && compareWindow < endWindow - 5) { // PARAMTODO
            double[] vector = getNormalizedSpectrogramVector(compareWindow);
            for (int j=0; j<vectorSize; j++) {
                compareVector[j] += (vector[j] - compareVector[j]) / (compareCount + 1);
            }
            compareCount++;
            compareWindow++;
        }
        double splitThreshold = .8; // PARAMTODO
        // Now, compare the vectors, decide whether or not to split, and shift the compare point
        while (compareWindow < endWindow - 5) { // PARAMTODO
            /*
            System.out.println("\nAt window: " + window);
            System.out.println("averageVector norm: " + dotProduct(averageVector, averageVector));
            System.out.println("compareVector norm: " + dotProduct(compareVector, compareVector));
            System.out.println("dot product: " + dotProduct(averageVector, compareVector));
            */
            if (dotProduct(averageVector, compareVector) < splitThreshold) {
                List<TranscribedNote> notes = new ArrayList<TranscribedNote>();
                TranscribedNote note = getSingleNoteFromSpectrogramWindow(startWindow, window,
                        averageVector);
                if (note != null) {
                    notes.add(note);
                }
                for (TranscribedNote splitNote : splitNotes(window+1, endWindow)) {
                    notes.add(splitNote);
                }
                return notes;
            }
            // We didn't decide to split at this window, so move one window down.  That means add
            // the vector at window to averageVector, subtract it from compareVector, and add the
            // window at compareWindow to compareVector.
            double[] vector = getNormalizedSpectrogramVector(window);
            for (int j=0; j<vectorSize; j++) {
                averageVector[j] += (vector[j] - averageVector[j]) / (averageCount + 1);
                compareVector[j] -= (vector[j] - compareVector[j]) / (compareCount);
            }
            averageCount++;
            compareCount--;
            vector = getNormalizedSpectrogramVector(compareWindow);
            for (int j=0; j<vectorSize; j++) {
                compareVector[j] += (vector[j] - compareVector[j]) / (compareCount + 1);
            }
            compareCount++;
            window++;
            compareWindow++;
        }
        // We didn't ever split the window, so compute the pitch and return a list with a single
        // note
        List<TranscribedNote> notes = new ArrayList<TranscribedNote>();
        TranscribedNote note = getSingleNoteFromSpectrogramWindow(startWindow, window,
                averageVector);
        if (note != null) {
            notes.add(note);
        }
        return notes;
    }

    private double[] getNormalizedSpectrogramVector(int windowNum) {
        List<Double> v = spectrogram.get(windowNum);
        double[] vector = new double[v.size()];
        double norm = 0.0;
        for (int i=0; i<v.size(); i++) {
            vector[i] = v.get(i);
            norm += vector[i] * vector[i];
        }
        norm = Math.sqrt(norm);
        for (int j=0; j<v.size(); j++) {
            vector[j] /= norm;
        }
        return vector;
    }

    private double dotProduct(double[] v1, double[] v2) {
        if (v1.length != v2.length) {
            throw new RuntimeException("Vectors must be same length for dot product");
        }
        double sum = 0.0;
        for (int i=0; i<v1.length; i++) {
            sum += v1[i] * v2[i];
        }
        return sum;
    }

    private TranscribedNote getSingleNoteFromSpectrogramWindow(int startWindow, int endWindow,
            double[] averageVector) {
        double max = .1;
        int max_index = 0;
        for (int i=4; i<averageVector.length; i++) {
            double freq = i * sampleRate / spectrogramWindowSize;
            if (freq > noteFrequencies[noteFrequencies.length-1]) continue;
            if (averageVector[i] > max) {
                if (max_index > 0 && i - max_index > 2) break;
                max = averageVector[i];
                max_index = i;
            }
        }
        double likely_pitch = max_index * sampleRate / spectrogramWindowSize;
        if (likely_pitch == 0.0) {
            return null;
        }

        int startFrame = startWindow * windowStepSize + spectrogramWindowSize / 2;
        int endFrame = endWindow * windowStepSize + spectrogramWindowSize / 2;
        double pitch = getPitchFromFft(startFrame, endFrame, likely_pitch);
        if (pitch == 0.0) {
            return null;
        }

        double startTime = startFrame / (double) sampleRate;
        double endTime = endFrame / (double) sampleRate;
        return new TranscribedNote(startWindow, endWindow, startTime, endTime, pitch);
    }

    /////////////////////////////////////////////////////////////////////////
    // Basic FFT kinds of stuff, including getting frequencies and magnitudes
    /////////////////////////////////////////////////////////////////////////

    private double[] windowWeights;

    /**
     * Copy data from fullSignal into an array for performing an fft, starting at start and ending
     * at end.  The resultant array will be of size (end - start) * 2.
     */
    private double[] copyDataForFft(int start, int end) {
        double[] fftArray = new double[(end - start) * 2];
        for (int i=0; i<(end-start); i++) {
            int val = fullSignal.get(i+start);
            // Half-wave rectification and log compression
            if (val > 0) {
                if (windowWeights == null || windowWeights.length != (end-start)) {
                    computeWindowWeights(end-start);
                }
                fftArray[2*i] = windowWeights[i] * Math.log(val);
            } else {
                fftArray[2*i] = 0;
            }
            // Complex part of fftArray is zero
            fftArray[2*i+1] = 0;
        }
        return fftArray;
    }

    private void computeWindowWeights(int n) {
        // This uses a Hamming window.  I could make it pluggable for different windows, but I see
        // no reason to right now.
        windowWeights = new double[n];
        for (int i=0; i<n; i++) {
            double angle = 2 * Math.PI * i / (n - 1);
            windowWeights[i] = .54 - .46 * Math.cos(angle);
        }
    }

    /**
     * Perform an in place FFT on array.  We keep around two different JTransforms FFT objects; the
     * first (stft) is a fixed size which we perform frequently to generate the spectrogram.  The
     * second (fft) changes its size based on what transforms need to be done, and generally gets
     * used once per note played.  The condition (array.length = fftSize) is mostly just a chance
     * coincidence if it ever happens, but if it does will give us a small speed improvement.
     */
    private void doFFT(double[] array) {
        num_ffts++;
        int numSamples = array.length / 2;
        if (numSamples == stftSize) {
            stft.complexForward(array);
        } else if (numSamples == fftSize) {
            fft.complexForward(array);
        } else {
            fft = new DoubleFFT_1D(numSamples);
            fftSize = numSamples;
            fft.complexForward(array);
        }
    }

    /**
     * Find a peak frequency from fullSignal (frames startFrame to endFrame) that is close to
     * likely_pitch.
     */
    private double getPitchFromFft(int startFrame, int endFrame, double likely_pitch) {
        double[] fftArray = copyDataForFft(startFrame, endFrame);
        doFFT(fftArray);
        // TODOLATER: There are a couple of places where I compute magnitudes from a complex FFT
        // array - I should probably share the code.
        double[] magnitudes = new double[fftArray.length/4];
        // 0 < i < fftArray.length / 2, because the top half of the fft array is useless
        for (int i=0; i<fftArray.length/4; i++) {
            // the denominator when computing frequency is the number of samples; because this is a
            // complex fft, the number of samples is fftArray.length / 2
            double freq = sampleRate * i / (fftArray.length / 2);
            double mag = Math.sqrt(fftArray[2*i]*fftArray[2*i] + fftArray[2*i+1]*fftArray[2*i+1]);
            magnitudes[i] = mag;
        }
        List<Double> peaks = findFreqMagsAndPeaks(magnitudes, 20); // PARAMTODO
        double closest_peak = 0.0;
        double closest_dist = Double.MAX_VALUE;
        for (double peak : peaks) {
            double dist = Math.abs(likely_pitch - peak);
            if (dist < closest_dist) {
                closest_dist = dist;
                closest_peak = peak;
            }
        }
        return closest_peak;
    }

    private ArrayList<Double> findFreqMagsAndPeaks(double[] mag_array, int min_mag) {
        ArrayList<Double> peaks = new ArrayList<Double>();

        boolean in_peak = false;
        int first_bin = -1;
        boolean past_zero = false;

        ArrayList<Double> peak_mags = new ArrayList<Double>();
        ArrayList<Double> peak_freqs = new ArrayList<Double>();

        // Here we're assuming that magnitudes were computed for the first half of the transform
        // signal only.
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
