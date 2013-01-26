package com.gardner.soundengine;

import javax.swing.JFrame;
import javax.swing.JPanel;

import com.gardner.soundengine.java_ui.*;
import com.gardner.soundengine.microphone.*;
import com.gardner.soundengine.transcription.*;

class Main {
    public static void main (String[] args) throws InterruptedException {
        boolean file = false;
        if (args.length > 0) {
            if (args[0].equals("file")) {
                file = true;
            }
        }
        JPanel panel;
        if (file) {
            panel = new SoundEngineFilePanel();
        } else {
            System.out.println("Currently broken, sorry...");
            LinuxMicrophone mic = new LinuxMicrophone();
            TranscriptionEngine engine = new TranscriptionEngine(mic);
            panel = null;
            //panel = new SoundEnginePanel(engine, mic);
        }
        JFrame frame = new JFrame("Sound Engine");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(panel);
        frame.pack();
        frame.setVisible(true);
    }
}
