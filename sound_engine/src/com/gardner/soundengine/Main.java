package com.gardner.soundengine;

import javax.swing.JFrame;
import javax.swing.JPanel;

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
            LinuxMicrophone mic = new LinuxMicrophone();
            SoundEngine engine = new SoundEngine(mic);
            panel = new SoundEnginePanel(engine, mic);
        }
        JFrame frame = new JFrame("Sound Engine");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(panel);
        frame.pack();
        frame.setVisible(true);
    }
}
