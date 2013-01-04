package com.gardner.soundengine;

public interface Microphone {

    /**
     * Perform whatever work is necessary to get the microphone ready to start recording, including
     * setting things like the sample rate and buffer read size, and all of that.
     */
    public void initialize();

    public int getSampleRate();

    public int getBufferSize();

    public int getBitRate();

    public int getBytesPerFrame();

    /**
     * Magnitude in Fourier space below which we ignore a frequency component.  Because this
     * depends heavily on microphone settings, we put it in here.
     */
    public int getMinimumMagnitude();

    /**
     * Start the microphone running.
     */
    public void start();

    /**
     * Attempt to sample bufferSize bytes from the microphone, putting the result into buffer, and
     * returning the actual number of bytes read.
     */
    public int sample(byte[] buffer);

    /**
     * Stop the microphone.
     */
    public void stop();
}
