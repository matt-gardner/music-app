package com.gardner.soundengine.microphone;

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
        System.out.println("Opened file " + file.getPath());
        System.out.println("Sample rate: " + sampleRate);
        System.out.println("Bit rate: " + bitRate);
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
    public int getBytesPerFrame() {
        return bytesPerFrame;
    }

    @Override
    public void start() {
    }

    @Override
    public int sample(byte[] buffer) {
        try {
            int bytes = stream.read(buffer, 0, buffer.length);
            // These are unsigned bytes, but java is treating them as signed.  So we shift the
            // values.  TODOLATER: this is quite specific to some particular files I'm currently
            // working with.  If this gets used more broadly later, this bit of code should be
            // fixed.
            if (bitRate == 8) {
                for (int i=0; i<buffer.length; i++) {
                    if (buffer[i] > 0) {
                        buffer[i] = (byte) (buffer[i] - 128);
                    } else {
                        buffer[i] = (byte) (buffer[i] + 128);
                    }
                }
            }
            return bytes;
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
