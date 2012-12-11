package com.gardner.musicapp;

import android.os.Handler;

import com.gardner.soundengine.SoundEngine;

public class SoundEngineRunner extends Thread {
    private Handler handler;
    private Runnable callback;
    private SoundEngine engine;

    public SoundEngineRunner(Handler handler, Runnable callback,
            SoundEngine engine) {
        this.engine = engine;
        this.handler = handler;
        this.callback = callback;
    }

    public void run() {
        engine.start();
        while (engine.sampleMic()) {
            handler.post(callback);
        }
    }

    public void end() {
        engine.stop();
    }
}
