package com.gardner.musicapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.model.XYSeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

public class MainActivity extends Activity {

    public static final String TAG = "musicapp.Main";

    private TextView freqText;
    private TextView noteText;
    private TextView diffText;
    private Microphone microphone;
    private Handler handler;
    private Runnable callback;

    private Button switchMic;
    private String micOn, micOff;

    private XYSeries freqData;
    private XYMultipleSeriesDataset dataset;
    private XYMultipleSeriesRenderer renderer;
    private XYSeriesRenderer seriesRenderer;
    private GraphicalView graphicalView;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.main);
        handler = new Handler();
        callback = new FrequencyUpdater();
        microphone = new Microphone(handler, callback);

        initializeNoteFrequencies(440);

        freqText = (TextView) findViewById(R.id.freq_text);
        freqText.setTextSize(40);
        freqText.setText("No audio");
        noteText = (TextView) findViewById(R.id.note_text);
        noteText.setTextSize(40);
        noteText.setText("No audio");
        diffText = (TextView) findViewById(R.id.diff_text);
        diffText.setTextSize(40);
        diffText.setText("No audio");


        micOn = getResources().getString(R.string.mic_on);
        micOff = getResources().getString(R.string.mic_off);
        switchMic = (Button) findViewById(R.id.switch_mic);
        switchMic.setOnClickListener(new View.OnClickListener() {
            boolean start = true;
            public void onClick(View view) {
                switchMicState(start);
                if (start) {
                    start = false;
                } else {
                    start = true;
                }
            }
        });

        renderer = new XYMultipleSeriesRenderer();
        renderer.setYAxisMin(0.0);
        renderer.setYAxisMax(25000.0);
        seriesRenderer = new XYSeriesRenderer();
        renderer.addSeriesRenderer(seriesRenderer);
        freqData = new XYSeries("Frequency Magnitudes");
        dataset = new XYMultipleSeriesDataset();
        dataset.addSeries(freqData);
        graphicalView = ChartFactory.getLineChartView(this, dataset, renderer);
        graphicalView.refreshDrawableState();
        graphicalView.repaint();
        LinearLayout layout = (LinearLayout) findViewById(R.id.freq_graph);
        layout.addView(graphicalView);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopMicrophone();
    }

    public void switchMicState(boolean start) {
        if (start) {
            switchMic.setText(micOff);
            startMicrophone();
        } else {
            switchMic.setText(micOn);
            stopMicrophone();
        }
    }

    public void startMicrophone() {
        microphone.start();
    }

    public void stopMicrophone() {
        microphone.end();
        microphone.interrupt();
        microphone = new Microphone(handler, callback);
    }

    private void updateMaxFrequency() {
        double freq = microphone.getCurrentFrequency();
        if (freq == -1) return;
        int note = findClosestNote(freq);
        freqText.setText(Double.toString(freq));
        if (note == -1) {
            noteText.setText("Out of range");
            diffText.setText("Out of range");
        } else {
            noteText.setText(noteNames[note]);
            diffText.setText(Double.toString(freq - noteFrequencies[note]));
        }
    }

    private void updateFreqGraph() {
        freqData.clear();
        double[] currentMags = microphone.getCurrentMags();
        int num_bins = currentMags.length / 2;
        for (int i=0; i<num_bins; i++) {
            freqData.add(currentMags[2*i], currentMags[2*i+1]);
        }
        graphicalView.repaint();
    }

    private class FrequencyUpdater implements Runnable {
        @Override
        public void run() {
            updateMaxFrequency();
            updateFreqGraph();
        }

    }

    private int findClosestNote(double frequency) {
        // Some bound checking first
        if (frequency > noteFrequencies[noteFrequencies.length - 1] ||
                frequency < noteFrequencies[0]) {
            return -1;
        }
        int min = 0;
        int max = noteFrequencies.length;
        int i = (max + min) / 2;
        while (true) {
            if (frequency > noteFrequencies[i]) {
                if (frequency - noteFrequencies[i] < noteFrequencies[i+1] - frequency) {
                    return i;
                } else {
                    min = i;
                    i = (max + min) / 2;
                }
            } else if (frequency < noteFrequencies[i]) {
                if (noteFrequencies[i] - frequency < frequency - noteFrequencies[i-1]) {
                    return i;
                } else {
                    max = i;
                    i = (max + min) / 2;
                }
            } else {
                return i;
            }
        }
    }

    private void initializeNoteFrequencies(int basePitch) {
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
            Log.d(TAG, noteNames[i] + " is at pitch " + noteFrequencies[i]);
            currentPitch *= multiplier;
            i++;
            currentNote++;
            if (currentNote == oneOctaveNames.length) {
                octave++;
                currentNote = 0;
            }
        }
    }

    private double[] noteFrequencies;
    private String[] noteNames;

    private String[] oneOctaveNames = new String[] {
        "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B",
    };
}
