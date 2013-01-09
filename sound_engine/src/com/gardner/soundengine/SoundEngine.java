package com.gardner.soundengine;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class SoundEngine {

    private Microphone microphone;
    private int sampleRate;
    private int bytesPerFrame;

    private int bufferSize;
    private byte[] buffer;
    private int dataSize;
    private List<Integer> fullSignal;

    int spectrogramWindowSize;
    int windowStepSize;
    private List<List<Double>> spectrogram;

    private FFT fft;
    private int num_ffts;

    private List<TranscribedNote> transcribedNotes;
    // A bunch of variables for transcribing notes, which pretty much always span buffer reads.
    private int windowNum;
    private boolean withinNote;
    private int startWindow;
    private int endWindow;
    private int vectorSize;
    private int averageCount;
    private int compareCount;
    private double[] averageVector;
    private double[] compareVector;
    private int minNoteSize;
    private int compareWindowSize;
    private double splitThreshold;


    public SoundEngine(Microphone microphone) {
        this.microphone = microphone;
        microphone.initialize();
        sampleRate = microphone.getSampleRate();
        bytesPerFrame = microphone.getBytesPerFrame();

        bufferSize = microphone.getBufferSize();
        buffer = new byte[bufferSize];
        dataSize = bufferSize / bytesPerFrame;
        fullSignal = new ArrayList<Integer>();

        // With a sample rate of 44100, if we do windows in increments of 256 steps, we get a
        // resolution of 1s / 44100 * 256 ~= 6 ms.  32nd notes at 240 beats per minute take about
        // 31 milliseconds, and humans can distinguish sounds up to about 10 milliseconds.  So this
        // resolution should be plenty adequate.
        windowStepSize = 256;
        // We want to spectrogram to have a good enough resolution to differentiate half steps;
        // this is right about on the border of that.
        spectrogramWindowSize = 2048;
        spectrogram = new ArrayList<List<Double>>();

        fft = new FFT(dataSize, spectrogramWindowSize);
        num_ffts = 0;

        transcribedNotes = new ArrayList<TranscribedNote>();
        windowNum = 0;
        withinNote = false;
        startWindow = -1;
        endWindow = -1;
        // This is the size of the magnitude vector in the spectrogram.  Because we throw out the
        // top half of the FFT, we have spectrogramWindowSize / 2 entries in the magnitude vector
        vectorSize = spectrogramWindowSize / 2;
        // If a window corresponds to 6 milliseconds, then this means anything less than about 25
        // milliseconds will be ignored.
        minNoteSize = 4; // PARAMTODO
        // Similarly, we use about 30 milliseconds to compare a change in sound to see if it's a
        // new note
        compareWindowSize = 5; // PARAMTODO
        splitThreshold = .8; // PARAMTODO
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
        return (int) Math.pow(2, bytesPerFrame*8-1);
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
        // TODO: what happens if I'm processing stuff and it takes longer than my buffer size?  Do
        // I lose data, or does the microphone keep the data around?  Do I need a separate thread
        // just to read and save the microphone data, in case I get behind?
        int bytes = microphone.sample(buffer);
        copyBufferToData();
        if (bytes == bufferSize) {
            processSample();
            return true;
        }
        return false;
    }

    private void copyBufferToData() {
        if (bytesPerFrame == 1) {
            for (int i=0; i<bufferSize; i++) {
                fullSignal.add((int) buffer[i]);
            }
        } else {
            ByteBuffer b = ByteBuffer.wrap(buffer);
            for (int i=0; i<dataSize; i++) {
                if (bytesPerFrame == 2) {
                    fullSignal.add((int) b.getShort());
                } else {
                    throw new RuntimeException("Unsupported bytes per frame: " + bytesPerFrame);
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
        double[] magnitudeArray = fft.doFft(start, end, fullSignal);
        num_ffts++;
        List<Double> column = new ArrayList<Double>();
        for (double mag : magnitudeArray) {
            column.add(mag);
        }
        spectrogram.add(column);
    }

    private void findNoteOnsetsFromSpectrogram() {
        double total_mag = 0.0;
        for (int j=0; j<spectrogramWindowSize/2; j++) {
            total_mag += spectrogram.get(windowNum).get(j);
        }
        double seconds = windowNum * (double) windowStepSize / sampleRate;
        checkForNoteChange(windowNum);
        if (total_mag > 400) { // PARAMTODO
            if (!withinNote) {
                startNote(windowNum);
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
                        TranscribedNote note = getNoteFromSpectrogramWindow(startWindow, endWindow);
                        if (note != null) {
                            transcribedNotes.add(note);
                        }
                    }
                    withinNote = false;
                    endWindow = -1;
                }
            }
        }
    }

    private void startNote(int window) {
        startWindow = window;
        averageCount = 0;
        compareCount = 0;
        averageVector = new double[vectorSize];
        compareVector = new double[vectorSize];
    }

    private void checkForNoteChange(int window) {
        if (!withinNote) return;
        // We want to wait a few windows to get a good idea that the note is actually changing.
        // currentWindow here represents the window that we are splitting on, looking at
        // startWindow to currentWindow as the current note, and currentWindow to window as the
        // potential new note.  The outline of this method is: add currentWindow to the
        // averageVector, subtract it from the compareVector, and add window to the
        // compareVector.  Then do a dot product between averageVector and compareVector, and if
        // it's below some threshold, we start a new note.

        int currentWindow = window - compareWindowSize;
        // We only add the currentWindow to the averageVector and subtract it from the
        // compareVector if we've seen compareWindowSize windows already.  This means we're filling
        // up compareVector first, even though there's nothing to compare it to at the beginning.
        if (compareCount == compareWindowSize) {
            double[] vector = getNormalizedSpectrogramVector(currentWindow);
            for (int i=0; i<vectorSize; i++) {
                averageVector[i] += (vector[i] - averageVector[i]) / (averageCount + 1);
                compareVector[i] -= (vector[i] - compareVector[i]) / (compareCount);
            }
            averageCount++;
            compareCount--;
        }
        // We always want to add window to the compareVector.
        double[] vector = getNormalizedSpectrogramVector(window);
        for (int j=0; j<vectorSize; j++) {
            compareVector[j] += (vector[j] - compareVector[j]) / (compareCount + 1);
        }
        compareCount++;
        if (averageCount > minNoteSize) {
            if (dotProduct(averageVector, compareVector) < splitThreshold) {
                // We found a significant change; start a new note
                TranscribedNote note = getNoteFromSpectrogramWindow(startWindow, currentWindow);
                if (note != null) {
                    transcribedNotes.add(note);
                }
                // To be sure to use all of the windows, we need to do some bookkeeping here,
                // switching what was compareVector to the beginnings of a new averageVector.
                // The next time checkForNoteChange is called, it will look at the window after
                // window, and because compareCount will be 0, things will work just fine.
                double[] tmpVector = new double[vectorSize];
                System.arraycopy(compareVector, 0, tmpVector, 0, vectorSize);
                startNote(currentWindow + 1);
                System.arraycopy(tmpVector, 0, averageVector, 0, vectorSize);
            }
        }
    }

    private double[] getNormalizedSpectrogramVector(int window) {
        List<Double> v = spectrogram.get(window);
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

    /**
     * Assuming there is a single note in the windows startWindow to endWindow, convert that
     * segment into a TranscribedNote.
     *
     * We first look at the spectrogram (averageVector, specifically - this needs to be called in
     * the context of checkForNoteChange) to get an idea of where the pitch probably is, then we do
     * an FFT on the whole signal to find the exact pitch.
     */
    private TranscribedNote getNoteFromSpectrogramWindow(int startWindow, int endWindow) {
        double max = .1;
        int max_index = 0;
        // We start at 4 here to ignore the peak that's normally at 0 - that gets us to a frequency
        // of something like 80Hz, which should be high enough for most instruments.
        for (int i=4; i<averageVector.length; i++) {
            double freq = i * sampleRate / spectrogramWindowSize;
            if (freq > NoteUtil.getMaxFrequency()) continue;
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

        // Use as the starting and ending frames the halfway points for their respective windows
        int startFrame = startWindow * windowStepSize + spectrogramWindowSize / 2;
        int endFrame = endWindow * windowStepSize + spectrogramWindowSize / 2;
        double[] magnitudeArray = fft.doFft(startFrame, endFrame, fullSignal);
        num_ffts++;
        double pitch = fft.getPeakCloseToPitch(magnitudeArray, likely_pitch, sampleRate);
        if (pitch == 0.0) {
            return null;
        }

        double startTime = startFrame / (double) sampleRate;
        double endTime = endFrame / (double) sampleRate;
        return new TranscribedNote(startWindow, endWindow, startTime, endTime, pitch);
    }
}
