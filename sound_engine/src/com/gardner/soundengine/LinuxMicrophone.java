package com.gardner.soundengine;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;

public class LinuxMicrophone implements Microphone {
    private TargetDataLine line = null;
    private int bufferSize;
    private int sampleRate;
    private int bitRate;
    private int bytesPerFrame;
    private int minMagnitude;
    private AudioFormat format;
    private ByteArrayOutputStream out;
    private boolean saveToFile = false;

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
        sampleRate = 44100;
        bitRate = 8;
        bytesPerFrame = bitRate / 8;
        bufferSize = 1024 * 4 * bytesPerFrame;
        minMagnitude = 100;
        format = new AudioFormat(sampleRate, bitRate, 1, true, true);
    }

    public void setSaveToFile(boolean saveToFile) {
        if (!saveToFile && out != null) {
            try {
                out.close();
            } catch(IOException e) { }
            out = null;
        }
        if (saveToFile) {
            out = new ByteArrayOutputStream();
        }
        this.saveToFile = saveToFile;
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
    public int getBitRate() {
        return bitRate;
    }

    @Override
    public int getBytesPerFrame() {
        return bytesPerFrame;
    }

    @Override
    public int getMinimumMagnitude() {
        return minMagnitude;
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
        int bytes = line.read(buffer, 0, bufferSize);
        if (saveToFile && bytes != -1) {
            out.write(buffer, 0, bytes);
        }
        return bytes;
    }

    @Override
    public void stop() {
        line.close();
    }

    public void saveAudio() {
        try {
            out.close();
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
        AudioInputStream stream = new AudioInputStream(
                new ByteArrayInputStream(out.toByteArray()),
                format,
                out.toByteArray().length / format.getFrameSize());
        File outfile = new File("audio_out.wav");
        try {
            AudioSystem.write(stream, AudioFileFormat.Type.WAVE, outfile);
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
        out = new ByteArrayOutputStream();
    }
}
