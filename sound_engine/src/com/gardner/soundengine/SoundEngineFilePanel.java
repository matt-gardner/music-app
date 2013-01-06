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

    private JLabel soundWaveLabel;
    private JLabel spectrogramLabel;
    private JScrollPane spectrogramScrollPane;

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
        fileLabel = new JLabel("No file loaded");
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

    private void runEngine(File file) {
        LinuxFileMicrophone microphone = new LinuxFileMicrophone(file);
        ArrayList<Double> data = new ArrayList<Double>();
        SoundEngine engine = new SoundEngine(microphone);
        double normalizer = (double) engine.getDataNormalizer();
        engine.start();
        int count = 0;
        long start_time = System.currentTimeMillis();
        while (engine.sampleMic()) {
            for (int i : engine.getRawSignal()) {
                data.add(i / normalizer);
            }
        }
        long end_time = System.currentTimeMillis();
        double seconds = data.size() / (double) engine.getSampleRate();
        double proccessing_time = (end_time - start_time) / 1000.0;
        engine.stop();
        List<List<Double>> spectrogram = engine.getSpectrogram();
        int start = 60000;
        int scaling = 10;
        showSoundWave(data, start, scaling);
        // Cut off the spectrogram plot above 4000 Hz, as it's not interesting
        int max_freq = 4000;
        int max_j = 0;
        int sampleRate = engine.getSampleRate();
        int windowSize = spectrogram.get(0).size();
        for (int i=0; i<windowSize; i++) {
            double freq = sampleRate * i / (windowSize * 2);
            if (freq > max_freq) {
                max_j = i;
                break;
            }
        }
        showSpectrogram(spectrogram, engine, max_j);
        System.out.println("Audio file length: " + seconds);
        System.out.println("Time to process: " + proccessing_time);
        System.out.println("Number of FFTs performed: " + engine.getNumFfts());
    }

    private void showSoundWave(ArrayList<Double> data, int start, int scaling) {
        int width = 2000;
        int height = 200;
        System.out.println("Number of sound samples: " + data.size());
        System.out.println("Building image of size " + width + " x " + height + " (" +
                (width * height * 4 / 1024) + " kb)");

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = (Graphics2D) image.getGraphics();
        for (int i=0; i<width; i++) {
            double value = data.get(start+i*scaling);
            boolean drawn = false;
            for (int j=0; j<height; j++) {
                double percent = ((double) j - height / 2) / (height / 2);
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

    private void showSpectrogram(List<List<Double>> spectrogram, SoundEngine engine, int max_j) {
        int width = spectrogram.size();
        int pixels_per_j = 400 / max_j;
        int height = max_j * pixels_per_j;
        System.out.println("Number of sound samples: " + spectrogram.size());
        System.out.println("Building image of size " + width + " x " + height + " (" +
                (width * height * 4 / 1024) + " kb)");

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = (Graphics2D) image.getGraphics();
        float max = Float.MIN_VALUE;
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < max_j; j++) {
                float c = spectrogram.get(i).get(j).floatValue();
                if (c > max) max = c;
            }
        }
        for (int i = 0; i < width; i++) {
            int current_j = height - 1;
            for (int j = 0; j < max_j; j++) {
                float c = spectrogram.get(i).get(j).floatValue() / max;
                g.setColor(new Color(c, c, c));
                for (int k=0; k<pixels_per_j; k++) {
                    g.fillRect(i, current_j--, 1, 1);
                }
            }
        }

        for (List<Integer> note : engine.getNoteBoundaries()) {
            // TODO: can I just fill line here, or fill rect for the line?
            g.setColor(Color.GREEN);
            for (int j=0; j<height; j++) {
                g.fillRect(note.get(0), j, 1, 1);
            }
            if (note.size() == 1) continue;
            g.setColor(Color.RED);
            for (int j=0; j<height; j++) {
                g.fillRect(note.get(1), j, 1, 1);
            }
        }
        spectrogramLabel.setIcon(new ImageIcon(image));
    }
}
