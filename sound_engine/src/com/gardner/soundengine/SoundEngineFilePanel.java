package com.gardner.soundengine;

import java.awt.*;
import java.awt.event.*;
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

    private EngineRunner runner;

    private SoundEngine engine;
    private LinuxFileMicrophone microphone;

    public SoundEngineFilePanel() {

        setPreferredSize(new Dimension(1500, 750));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        topPanel = new JPanel();
        topPanel.setPreferredSize(new Dimension(1500, 100));
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        add(topPanel);

        fileChooser = new JFileChooser();
        pickFileButton = new JButton("Choose File");
        pickFileButton.addActionListener(new FileListener());
        topPanel.add(pickFileButton);
        fileLabel = new JLabel("No file loaded");
        topPanel.add(fileLabel);
    }

    private class FileListener implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            int returnVal = fileChooser.showOpenDialog(SoundEngineFilePanel.this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                fileLabel.setText(file.getName());
                microphone = new LinuxFileMicrophone(file);
                engine = new SoundEngine(microphone);
                runner = new EngineRunner();
                runner.start();
            }
        }
    }

    private class EngineRunner extends Thread {
        public void run() {
            engine.start();
            while (engine.sampleMic()) {
            }
        }

        public void end() {
            engine.stop();
        }
    }
}
