package com.gardner.soundengine;

import java.util.ArrayList;
import java.util.List;

import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;

public class FFT {
    private DoubleFFT_1D fft;
    private int fftSize;
    private DoubleFFT_1D stft;
    private int stftSize;
    private double[] windowWeights;

    public FFT(int dataSize, int spectrogramWindowSize) {
        fft = new DoubleFFT_1D(dataSize);
        fftSize = dataSize;
        stft = new DoubleFFT_1D(spectrogramWindowSize);
        stftSize = spectrogramWindowSize;
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

    public double[] doFft(int start, int end, List<Integer> signal) {
        // First we copy the data from signal into fftArray, doing whatever windowing, compression
        // and rectification is desired
        double[] fftArray = new double[(end - start) * 2];
        for (int i=0; i<(end-start); i++) {
            int val = signal.get(i+start);
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

        // Now we do the FFT, in place, on fftArray.  We keep around two different JTransforms FFT
        // objects; the first (stft) is a fixed size which we perform frequently to generate the
        // spectrogram.  The second (fft) changes its size based on what transforms need to be
        // done, and generally gets used once per note played.  The condition (array.length =
        // fftSize) is mostly just a chance coincidence if it ever happens, but if it does will
        // give us a small speed improvement.
        int numSamples = fftArray.length / 2;
        if (numSamples == stftSize) {
            stft.complexForward(fftArray);
        } else if (numSamples == fftSize) {
            fft.complexForward(fftArray);
        } else {
            fft = new DoubleFFT_1D(numSamples);
            fftSize = numSamples;
            fft.complexForward(fftArray);
        }

        // Finally, compute the magnitudes from the complex fft.  Only the first half of the fft
        // contains useful data, so the array is of size numSamples / 2.
        double[] magnitudeArray = new double[numSamples / 2];
        for (int i=0; i<numSamples/2; i++) {
            double mag = Math.sqrt(fftArray[2*i]*fftArray[2*i] + fftArray[2*i+1]*fftArray[2*i+1]);
            magnitudeArray[i] = mag;
        }
        return magnitudeArray;
    }

    /**
     * Find a peak frequency from the FFT magnitudeArray that is close to likely_pitch.
     */
    public double getPeakCloseToPitch(double[] magnitudeArray, double likely_pitch, int sampleRate){
        List<Double> peaks = findPeaks(magnitudeArray, 20, sampleRate); // PARAMTODO
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

    /**
     * A simple peak finding algorithm that returns a list of peaks from magnitudes.  min_mag is a
     * threshold that creates a disjoint set of places to search for a peak, and we find the max in
     * each region (done by the helper method computePeakFrequency).  We also use NoteUtil's min
     * and max frequency to ignore regions of the space we don't care about.
     *
     * Another thing to note here is that we skip the first region above min_mag, assuming it is
     * still a drop off from the peak at zero.  For this to work right, min_mag has to be set
     * appropriately.
     */
    private List<Double> findPeaks(double[] magnitudes, int min_mag, int sampleRate) {
        ArrayList<Double> peaks = new ArrayList<Double>();

        boolean in_peak = false;
        int first_bin = -1;
        boolean past_zero = false;

        ArrayList<Double> peak_mags = new ArrayList<Double>();
        ArrayList<Double> peak_freqs = new ArrayList<Double>();

        // Here we're assuming that magnitudes were computed for the first half of the transform
        // signal only.
        int num_bins = magnitudes.length * 2;

        for (int i=0; i<magnitudes.length; i++) {
            double freq = sampleRate * i / num_bins;
            if (freq < NoteUtil.getMinFrequency() || freq > NoteUtil.getMaxFrequency()) {
                continue;
            }
            if (magnitudes[i] > min_mag && freq != 0) {
                if (past_zero) {
                    if (!in_peak) {
                        in_peak = true;
                        first_bin = i;
                    }
                    peak_mags.add(magnitudes[i]);
                    peak_freqs.add(freq);
                }
            } else {
                past_zero = true;
                if (in_peak) {
                    if (peak_mags.size() > 1) {
                        peaks.add(computePeakFrequency(peak_mags, peak_freqs, first_bin, num_bins,
                                    sampleRate));
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

    /**
     * Given a sequential list of magnitudes and frequencies, find the peak frequency, doing some
     * quadratic interpolation to refine the frequency estimate.
     *
     * This could probably just use indices into the magnitude array, actually, and it would be a
     * little more efficient.  Oh well.
     */
    private double computePeakFrequency(ArrayList<Double> mags, ArrayList<Double> freqs,
            int first_bin, int num_bins, int sampleRate) {
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
}
