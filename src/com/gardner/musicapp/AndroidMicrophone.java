package com.gardner.musicapp;

import android.os.Handler;
import android.util.Log;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import com.gardner.soundengine.Microphone;

public class AndroidMicrophone implements Microphone {
    public static final String TAG = "musicapp.AndroidMicrophone";

    private int bufferMultiplier;

    private int sampleRate;
    private int bufferSize;

    private AudioRecord mic;

    @Override
    public void initialize() {
        bufferMultiplier = 3;
        mic = findMicrophoneSettings();
    }

    @Override
    public void start() {
        mic.startRecording();
    }

    @Override
    public int getBufferSize() {
        return bufferSize;
    }

    @Override
    public int getSampleRate() {
        return sampleRate;
    }

    @Override
    public int sample(byte[] buffer) {
        return mic.read(buffer, 0, bufferSize);
    }

    @Override
    public void stop() {
        mic.stop();
        mic.release();
    }

    // The variables and method below were taken almost verbatim from StackOverflow:
    // http://stackoverflow.com/questions/4843739/audiorecord-object-not-initializing
    private int[] sampleRates = new int[] { 44100, 22050, 11025, 8000 };
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
                        bufferSize *= bufferMultiplier;
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
