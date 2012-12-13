package com.gardner.soundengine;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

class SoundEnginePanel extends JPanel {
    private JPanel topPanel;
    private JPanel bottomPanel;
    private JPanel buttonPanel;
    private JPanel textPanel;
    private JLabel frequencyLabel;
    private JLabel noteLabel;
    private JLabel differenceLabel;
    private JButton startEngineButton;
    private boolean engineRunning;

    private EngineRunner runner;

    private SoundEngine engine;

    private XYSeries series;
    private XYSeriesCollection data;
    private JFreeChart chart;
    private ChartPanel chartPanel;


    public SoundEnginePanel(SoundEngine engine) {
        this.engine = engine;

        setPreferredSize(new Dimension(500, 450));

        frequencyLabel = new JLabel("No audio");
        noteLabel = new JLabel("No audio");
        differenceLabel = new JLabel("No audio");

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));
        add(topPanel);

        textPanel = new JPanel();
        textPanel.setPreferredSize(new Dimension(300, 150));
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.add(frequencyLabel);
        textPanel.add(noteLabel);
        textPanel.add(differenceLabel);
        topPanel.add(textPanel);

        buttonPanel = new JPanel();
        buttonPanel.setPreferredSize(new Dimension(200, 150));
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        startEngineButton = new JButton("Start Microphone");
        startEngineButton.addActionListener(new EngineButtonListener());
        buttonPanel.add(startEngineButton);
        topPanel.add(buttonPanel);

        bottomPanel = new JPanel();
        add(bottomPanel);

        engineRunning = false;
        series = new XYSeries("FFT");
        data = new XYSeriesCollection(series);
        chart = ChartFactory.createXYLineChart(
                "FFT",
                "Frequency",
                "Magnitude",
                data,
                PlotOrientation.VERTICAL,
                true,
                true,
                false);
        chart.getXYPlot().getRangeAxis().setRange(0.0, 5000.0);
        chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(500, 300));
        bottomPanel.add(chartPanel);
    }

    private void update() {
        updateLabels();
        updateChart();
    }

    private void updateLabels() {
        double freq = engine.getCurrentFrequency();
        frequencyLabel.setText(Double.toString(freq));
        String note = engine.getCurrentClosestNote();
        noteLabel.setText(note);
        double noteFreq = engine.getNoteFrequency(note);
        differenceLabel.setText(Double.toString(freq - noteFreq));
    }

    private void updateChart() {
        series.clear();
        double[] currentMags = engine.getCurrentMags();
        int num_bins = currentMags.length / 2;
        for (int i=0; i<num_bins; i++) {
            series.add(currentMags[2*i], currentMags[2*i+1]);
        }
    }

    private class EngineButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            if (engineRunning) {
                runner.end();
                runner.interrupt();
                startEngineButton.setText("Start Microphone");
            } else {
                runner = new EngineRunner();
                runner.start();
                startEngineButton.setText("Stop Microphone");
            }
            engineRunning = !engineRunning;
        }
    }

    private class EngineRunner extends Thread {
        public void run() {
            engine.start();
            while (engine.sampleMic()) {
                update();
            }
        }

        public void end() {
            engine.stop();
        }
    }
}
