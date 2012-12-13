package com.gardner.soundengine;

import javax.swing.JFrame;

class Main {
    public static void main (String[] args) throws InterruptedException {
        JFrame frame = new JFrame("Sound Engine");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        SoundEngine engine = new SoundEngine(new LinuxMicrophone());
        frame.getContentPane().add(new SoundEnginePanel(engine));
        frame.pack();
        frame.setVisible(true);
    }
}
