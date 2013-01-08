package com.gardner.soundengine;

public class TranscribedNote {
    private final int startWindow;
    private final int endWindow;
    private final double startTime;
    private final double endTime;
    private final double pitch;

    public TranscribedNote(int startWindow, int endWindow, double startTime, double endTime,
            double pitch) {
        this.startWindow = startWindow;
        this.endWindow = endWindow;
        this.startTime = startTime;
        this.endTime = endTime;
        this.pitch = pitch;
    }

    public int getStartWindow() {
        return startWindow;
    }

    public int getEndWindow() {
        return endWindow;
    }

    public double getStartTime() {
        return startTime;
    }

    public double getEndTime() {
        return endTime;
    }

    public double getPitch() {
        return pitch;
    }
}
