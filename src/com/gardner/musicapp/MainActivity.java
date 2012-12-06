package com.gardner.musicapp;

import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;


import android.app.Activity;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends Activity
{
    public static final String TAG = "MusicApp";

    private int SAMPLE_RATE = 8000;
    private int BUFFER_SIZE = 4 * 1024;;
    DoubleFFT_1D fft = new DoubleFFT_1D(BUFFER_SIZE);
    byte[] buffer;
    TextView textView;
    private AudioRecord mic;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mic = findMicrophoneSettings();
        textView = (TextView) findViewById(R.id.freq_text);
        textView.setTextSize(40);
        textView.setText("No audio");
    }

    /** Called when the user clicks the Send button */
    public void startMicrophone(View view) {
        textView.setText("Button pressed!");
        mic.startRecording();
        buffer = new byte[BUFFER_SIZE];
        int bytes;
        int i = 0;
        while ((bytes = mic.read(buffer, 0, BUFFER_SIZE)) > 0) {
            textView.setText(Double.toString(getFrequency(buffer)));
            i++;
            if (i > 100) break;
        }
    }

    private double getFrequency(byte[] bytes) {
        Log.d(TAG, "Getting frequency");
        double[] samples = new double[bytes.length];
        for (int i=0; i<bytes.length; i++) {
            samples[i] = bytes[i];
        }
        Log.d(TAG, "Performing FFT");
        doFFT(samples);
        Log.d(TAG, "Finding max");
        double max_amplitude = Double.NEGATIVE_INFINITY;
        double max_freq = Double.NEGATIVE_INFINITY;
        for (int i=0; i<samples.length; i++) {
            if (samples[i] > max_amplitude) {
                max_freq = SAMPLE_RATE * i / samples.length;
                max_amplitude = samples[i];
            }
        }
        Log.d(TAG, "Done: " + max_freq);
        return max_freq;
    }

    // The variables and method below were taken almost verbatim from StackOverflow:
    // http://stackoverflow.com/questions/4843739/audiorecord-object-not-initializing
    private int[] sampleRates = new int[] { 8000, 11025, 22050, 44100 };
    private short[] encodings =
        new short[] { AudioFormat.ENCODING_PCM_8BIT, AudioFormat.ENCODING_PCM_16BIT };
    private short[] channels =
        new short[] { AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_IN_STEREO };
    public AudioRecord findMicrophoneSettings() {
        for (int rate : sampleRates) {
            for (short encoding : encodings) {
                for (short channel : channels) {
                    try {
                        Log.d(TAG, "Attempting rate " + rate + "Hz, bits: " + encoding +
                                ", channel: " + channel);
                        int bufferSize = AudioRecord.getMinBufferSize(rate, channel, encoding);

                        if (bufferSize != AudioRecord.ERROR_BAD_VALUE) {
                            // check if we can instantiate and have a success
                            AudioRecord recorder = new AudioRecord(
                                    MediaRecorder.AudioSource.DEFAULT, rate, channel, encoding,
                                    bufferSize);
                            if (recorder.getState() == AudioRecord.STATE_INITIALIZED)
                                return recorder;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, rate + "Exception, keep trying.",e);
                    }
                }
            }
        }
        return null;
    }

    private void doFFT(double[] samples) {
        fft.realForward(samples);
    }

    @Override
    public void onPause() {
        mic.release();
    }
}
