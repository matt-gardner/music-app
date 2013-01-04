package com.gardner.soundengine;

import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

public class LinuxFileMicrophone implements Microphone {
    private File file;
    private AudioInputStream stream;
    private int bufferSize;
    private int sampleRate;
    private int bitRate;
    private int bytesPerFrame;
    private int minMagnitude;

    public LinuxFileMicrophone(File file) {
        this.file = file;
    }

    @Override
    public void initialize() {
        try {
            stream = AudioSystem.getAudioInputStream(file);
        } catch(UnsupportedAudioFileException e) {
            throw new RuntimeException(e);
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
        bufferSize = 1024 * 4;
        sampleRate = (int) stream.getFormat().getSampleRate();
        bitRate = stream.getFormat().getSampleSizeInBits();;
        bytesPerFrame = bitRate / 8;
        minMagnitude = 100;
        System.out.println("Opened file " + file.getPath());
        System.out.println("Sample rate: " + sampleRate);
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
    }

    @Override
    public int sample(byte[] buffer) {
        try {
            return stream.read(buffer, 0, bufferSize);
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        try {
            stream.close();
        } catch(IOException e) {
        }
    }
}
