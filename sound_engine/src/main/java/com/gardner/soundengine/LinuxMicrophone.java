package com.gardner.soundengine;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;

public class LinuxMicrophone implements Microphone {
    TargetDataLine line = null;
    private int bufferSize = 1024 * 4;
    private int sampleRate = 44100;
    AudioFormat format;

    @Override
    public void initialize() {
        Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
        int i=0;
        for (Mixer.Info info: mixerInfos){
            System.out.println(info);
            Mixer m = AudioSystem.getMixer(info);
            Line.Info lineInfo = m.getTargetLineInfo()[0];
            System.out.println(lineInfo);
            try {
                line = (TargetDataLine) m.getLine(lineInfo);
            } catch(LineUnavailableException e) {
                throw new RuntimeException(e);
            }
            if (i == 1) break;
            i++;
        }
        format = new AudioFormat(sampleRate, 8, 1, true, false);
    }

    @Override
    public int getSampleRate() {
        return sampleRate;
    }

    @Override
    public int getBufferSize() {
        return bufferSize;
    }

    @Override
    public void start() {
        try {
            line.open(format, bufferSize);
        } catch (LineUnavailableException e) {
            throw new RuntimeException(e);
        }
        line.start();
    }

    @Override
    public int sample(byte[] buffer) {
        return line.read(buffer, 0, bufferSize);
    }

    @Override
    public void stop() {
        line.close();
    }
}
