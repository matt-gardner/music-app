package com.gardner.soundengine;

import java.util.ArrayList;
import java.util.List;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.swing.*;

class SoundEngineFilePanel extends JPanel {
    private JFileChooser fileChooser;
    private JPanel topPanel;
    private JPanel bottomPanel;
    private JPanel chooserPanel;
    private JPanel textPanel;
    private JLabel fileLabel;
    private JButton pickFileButton;
    private JButton fromMicButton;

    private JLabel soundWaveLabel;
    private JLabel spectrogramLabel;
    private JScrollPane spectrogramScrollPane;

    // Some variables for the spectrogram
    private int max_y;
    private int pixels_per_y;
    private int width;
    private int height;
    private double freq_per_pixel;
    private double freq_per_y;


    public SoundEngineFilePanel() {

        setPreferredSize(new Dimension(1500, 750));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        topPanel = new JPanel();
        topPanel.setPreferredSize(new Dimension(1500, 100));
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        add(topPanel);

        fileChooser = new JFileChooser("data/flute/audio");
        pickFileButton = new JButton("Choose File");
        pickFileButton.addActionListener(new FileListener());
        topPanel.add(pickFileButton);
        fromMicButton = new JButton("Live from Microphone");
        fromMicButton.addActionListener(new MicListener());
        topPanel.add(fromMicButton);
        fileLabel = new JLabel("No audio loaded");
        topPanel.add(fileLabel);

        bottomPanel = new JPanel();
        bottomPanel.setPreferredSize(new Dimension(1500, 650));
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        add(bottomPanel);

        soundWaveLabel = new JLabel();
        bottomPanel.add(soundWaveLabel);
        spectrogramLabel = new JLabel();
        spectrogramScrollPane = new JScrollPane(spectrogramLabel);
        bottomPanel.add(spectrogramScrollPane);
    }

    private void runEngine(File file) {
        LinuxFileMicrophone microphone = new LinuxFileMicrophone(file);
        SoundEngine engine = new SoundEngine(microphone);

        // Set up some variables here for drawing the spectrogram
        // Cut off the spectrogram plot above 4000 Hz, as it's not interesting
        int max_freq = 4000;
        max_y = 0;
        int sampleRate = engine.getSampleRate();
        int windowSize = engine.getSpectrogramWindowSize();
        freq_per_y = sampleRate / (double) windowSize;
        for (int i=0; i<windowSize; i++) {
            double freq = sampleRate * i / (double) windowSize;
            if (freq > max_freq) {
                max_y = i;
                break;
            }
        }
        width = 1500;
        int target_height = 400;
        pixels_per_y = target_height / max_y;
        height = max_y * pixels_per_y;
        freq_per_pixel = freq_per_y / pixels_per_y;

        double normalizer = (double) engine.getDataNormalizer();
        engine.start();
        int count = 0;
        long start_time = System.currentTimeMillis();
        while (engine.sampleMic()) { }
        long end_time = System.currentTimeMillis();
        ArrayList<Double> data = new ArrayList<Double>();
        for (int i : engine.getRawSignal()) {
            data.add(i / normalizer);
        }
        double seconds = data.size() / (double) engine.getSampleRate();
        double proccessing_time = (end_time - start_time) / 1000.0;
        engine.stop();
        List<List<Double>> spectrogram = engine.getSpectrogram();
        showSpectrogram(spectrogram, engine);
        System.out.println("Audio file length: " + seconds);
        System.out.println("Time to process: " + proccessing_time);
        System.out.println("Number of FFTs performed: " + engine.getNumFfts());
    }

    private void showSoundWave(ArrayList<Double> data, int start, int scaling) {
        int waveWidth = 2000;
        int waveHeight = 200;
        System.out.println("Number of sound samples: " + data.size());
        System.out.println("Building image of size " + waveWidth + " x " + waveHeight + " (" +
                (waveWidth * waveHeight * 4 / 1024) + " kb)");

        BufferedImage image = new BufferedImage(waveWidth, waveHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = (Graphics2D) image.getGraphics();
        for (int i=0; i<waveWidth; i++) {
            double value = data.get(start+i*scaling);
            boolean drawn = false;
            for (int j=0; j<waveHeight; j++) {
                double percent = ((double) j - waveHeight / 2) / (waveHeight / 2);
                if (!drawn && value < percent) {
                    g.setColor(Color.BLACK);
                    drawn = true;
                } else {
                    g.setColor(Color.WHITE);
                }
                g.fillRect(i, j, 1, 1);
            }
        }

        soundWaveLabel.setIcon(new ImageIcon(image));
    }

    private void showSpectrogram(List<List<Double>> spectrogram, SoundEngine engine) {
        // Here we're drawing from a complete file, so we just set the width correctly
        width = spectrogram.size();
        System.out.println("Number of sound samples: " + spectrogram.size());
        System.out.println("Building image of size " + width + " x " + height + " (" +
                (width * height * 4 / 1024) + " kb)");

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = (Graphics2D) image.getGraphics();
        for (int x=0; x<spectrogram.size(); x++) {
            drawSpectrogramColumn(spectrogram.get(x), x, g);
        }

        for (TranscribedNote note : engine.getTranscribedNotes()) {
            drawTranscribedNote(note, g);
        }
        spectrogramLabel.setIcon(new ImageIcon(image));
    }

    private void drawSpectrogramColumn(List<Double> column, int x, Graphics2D g) {
        float normalizer = 1500.0f;
        int current_y = height - 1;
        for (int y = 0; y < max_y; y++) {
            float c = column.get(y).floatValue() / normalizer;
            if (c > 1.0f) {
                c = 1.0f;
            }
            g.setColor(new Color(c, c, c));
            for (int k=0; k<pixels_per_y; k++) {
                g.fillRect(x, current_y--, 1, 1);
            }
        }
    }

    private void drawTranscribedNote(TranscribedNote note, Graphics2D g) {
        System.out.println("\nNote:");
        System.out.println("Pitch: " + note.getPitch());
        System.out.println("Onset: " + note.getStartTime());
        System.out.println("End: " + note.getEndTime());
        // TODOLATER: can I just fill line here, or fill rect for the line?
        g.setColor(Color.GREEN);
        for (int y=0; y<height; y++) {
            g.fillRect(note.getStartWindow(), y, 1, 1);
        }
        g.setColor(Color.RED);
        for (int y=0; y<height; y++) {
            g.fillRect(note.getEndWindow(), y, 1, 1);
        }
        double freq = note.getPitch();
        int y_pixel = (int) (freq / freq_per_pixel);
        g.setColor(Color.YELLOW);
        for (int x=note.getStartWindow(); x<note.getEndWindow(); x++) {
            g.fillRect(x, height - y_pixel, 1, 1);
        }
    }

    private class FileListener implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            int returnVal = fileChooser.showOpenDialog(SoundEngineFilePanel.this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                fileLabel.setText(file.getName());
                runEngine(file);
            }
        }
    }

    private class MicListener implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            if (running) {
                fromMicButton.setText("Live from Microphone");
                fileLabel.setText("No audio loaded");
                runner.end();
                runner.interrupt();
            } else {
                fromMicButton.setText("Stop Microphone");
                fileLabel.setText("Live audio");
                LinuxMicrophone mic = new LinuxMicrophone();
                liveEngine = new SoundEngine(mic);
                // Set up some variables here for drawing the spectrogram
                // Cut off the spectrogram plot above 4000 Hz, as it's not interesting
                int max_freq = 4000;
                max_y = 0;
                int sampleRate = liveEngine.getSampleRate();
                int windowSize = liveEngine.getSpectrogramWindowSize();
                freq_per_y = sampleRate / (double) windowSize;
                for (int i=0; i<windowSize; i++) {
                    double freq = sampleRate * i / (double) windowSize;
                    if (freq > max_freq) {
                        max_y = i;
                        break;
                    }
                }
                width = 1500;
                int target_height = 400;
                pixels_per_y = target_height / max_y;
                height = max_y * pixels_per_y;
                freq_per_pixel = freq_per_y / pixels_per_y;
                lastColumn = 0;
                lastNoteIndex = 0;

                spectrogramImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                spectrogramLabel.setIcon(new ImageIcon(spectrogramImage));
                runner = new EngineRunner();
                runner.start();
            }
            running = !running;
        }
    }

    private SoundEngine liveEngine;
    private EngineRunner runner;
    private boolean running = false;
    private int lastColumn;
    private int lastNoteIndex;
    private BufferedImage spectrogramImage;

    private class EngineRunner extends Thread {
        public void run() {
            liveEngine.start();
            while (liveEngine.sampleMic()) {
                update();
            }
        }

        public void end() {
            liveEngine.stop();
        }
    }

    private void update() {
        Graphics2D g = (Graphics2D) spectrogramImage.getGraphics();
        List<List<Double>> spectrogram = liveEngine.getSpectrogram();
        if (spectrogram.size() >= width) {
            growSpectrogramImage();
        }
        for (; lastColumn<spectrogram.size(); lastColumn++) {
            drawSpectrogramColumn(spectrogram.get(lastColumn), lastColumn, g);
        }
        List<TranscribedNote> notes = liveEngine.getTranscribedNotes();
        for (; lastNoteIndex<notes.size(); lastNoteIndex++) {
            drawTranscribedNote(notes.get(lastNoteIndex), g);
        }
        Rectangle r = new Rectangle();
        r.x = lastColumn;
        spectrogramLabel.scrollRectToVisible(r);
        // TODOLATER: override paintComponent to only paint the part that's visible, using
        // buffer.getSubimage
        repaint();
    }

    private void growSpectrogramImage() {
        int oldWidth = width;
        width *= 2;
        BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);;
        for (int x=0; x<oldWidth; x++) {
            for (int y=0; y<height; y++) {
                newImage.setRGB(x, y, spectrogramImage.getRGB(x, y));
            }
        }
        spectrogramImage = newImage;
        spectrogramLabel.setIcon(new ImageIcon(spectrogramImage));
    }
}
