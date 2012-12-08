package com.gardner.musicapp;

import android.os.Handler;
import android.util.Log;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;

public class Microphone extends Thread {

    public static final String TAG = "musicapp.Microphone";

    private double currentFrequency;
    private double[] currentMags;

    private int sampleRate;
    private int bufferSize;
    private byte[] buffer;
    private DoubleFFT_1D fft;
    private AudioRecord mic;

    private Handler handler;
    private Runnable callback;

    public Microphone(Handler handler, Runnable callback) {
        this.handler = handler;
        this.callback = callback;
        mic = findMicrophoneSettings();
        fft = new DoubleFFT_1D(bufferSize);
        currentFrequency = 0.0;
        currentMags = new double[bufferSize/2];
    }

    public double getCurrentFrequency() {
        return currentFrequency;
    }

    public double[] getCurrentMags() {
        return currentMags;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public void run() {
        mic.startRecording();
        buffer = new byte[bufferSize];
        int bytes;
        while ((bytes = mic.read(buffer, 0, bufferSize)) > 0) {
            getFrequency(buffer);
            handler.post(callback);
        }
    }

    public void end() {
        mic.stop();
        mic.release();
    }

    private double getFrequency(byte[] bytes) {
        double[] samples = new double[2*bytes.length];
        for (int i=0; i<bytes.length; i++) {
            samples[2*i] = bytes[i];
        }
        doFFT(samples);
        // If the magnitude is lower than 10,000, ignore it.
        double max_magnitude = 10000;
        double max_freq = -1;
        // I would have thought I should only divide by two, but for some reason it looks like I
        // need to divide by 4.
        int max_n = bytes.length / 4;
        currentMags = new double[max_n*2];
        for (int i=0; i<max_n; i++) {
            double freq = sampleRate * i / bytes.length;
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

    // The variables and method below were taken almost verbatim from StackOverflow:
    // http://stackoverflow.com/questions/4843739/audiorecord-object-not-initializing
    private int[] sampleRates = new int[] { 8000, 11025, 22050, 44100 };
    private short[] encodings =
        new short[] { AudioFormat.ENCODING_PCM_8BIT, AudioFormat.ENCODING_PCM_16BIT };
    private short[] channels =
        new short[] { AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_IN_STEREO };

    private AudioRecord findMicrophoneSettings() {
        int i=0;
        for (int rate : sampleRates) {
            for (short encoding : encodings) {
                for (short channel : channels) {
                    try {
                        Log.d(TAG, "Attempting rate " + rate + "Hz, bits: " + encoding +
                                ", channel: " + channel);
                        bufferSize = AudioRecord.getMinBufferSize(rate, channel, encoding);
                        bufferSize *= 4;
                        if (bufferSize != AudioRecord.ERROR_BAD_VALUE) {
                            // check if we can instantiate and have a success
                            AudioRecord recorder = new AudioRecord(
                                    MediaRecorder.AudioSource.DEFAULT, rate, channel, encoding,
                                    bufferSize);
                            if (recorder.getState() == AudioRecord.STATE_INITIALIZED) {
                                Log.d(TAG, "Success!");
                                sampleRate = rate;
                                Log.d(TAG, "Sample rate: " + sampleRate);
                                Log.d(TAG, "Buffer size: " + bufferSize);
                                return recorder;
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, rate + "Exception, keep trying.",e);
                    }
                }
            }
            i++;
        }
        return null;
    }
}
