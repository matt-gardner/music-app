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

    private TextView textView;
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

        textView = (TextView) findViewById(R.id.freq_text);
        textView.setTextSize(40);
        textView.setText("No audio");

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
        renderer.setYAxisMax(10000.0);
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
        textView.setText(Double.toString(microphone.getCurrentFrequency()));
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
}
