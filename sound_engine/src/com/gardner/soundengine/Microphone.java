package com.gardner.soundengine;

public interface Microphone {

    /**
     * Perform whatever work is necessary to get the microphone ready to start recording, including
     * setting things like the sample rate and buffer read size, and all of that.
     */
    public void initialize();

    public int getSampleRate();

    public int getBufferSize();

    public int getBytesPerFrame();

    /**
     * Start the microphone running.
     */
    public void start();

    /**
     * Attempt to sample bytes from the microphone, putting the result into buffer, and returning
     * the actual number of bytes read.  We will try to fill the entire buffer, so the length of
     * the buffer should be smaller than the buffer size of the microphone, probably by a factor of
     * 8 or more, just to be safe.
     */
    public int sample(byte[] buffer);

    /**
     * Stop the microphone.
     */
    public void stop();
}
