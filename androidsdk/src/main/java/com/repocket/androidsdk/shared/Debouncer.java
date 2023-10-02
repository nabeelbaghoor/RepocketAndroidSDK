package com.repocket.androidsdk.shared;

import android.os.Handler;
import android.os.Looper;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class Debouncer {
    private final Runnable action;
    private final Runnable asyncAction;
    private final int delayMilliseconds;
    private final Timer timer;
    private CompletableFuture<Void> tcs;

    public Debouncer(Runnable action, int delayMilliseconds) {
        this.action = action;
        this.asyncAction = null;
        this.delayMilliseconds = delayMilliseconds;
        this.timer = new Timer();
    }

    public Debouncer(Runnable action, int delayMilliseconds, Looper looper) {
        this.action = action;
        this.asyncAction = null;
        this.delayMilliseconds = delayMilliseconds;
        this.timer = new Timer();
        this.timer.schedule(new TimerTask() {
            @Override
            public void run() {
                new Handler(looper).post(() -> {
                    timer.cancel();
                    tcs = null;
                    action.run();
                });
            }
        }, delayMilliseconds);
    }

    public Debouncer(CompletableFuture<Void> asyncAction, int delayMilliseconds) {
        this.action = null;
        this.asyncAction = () -> {
            try {
                asyncAction.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        };
        this.delayMilliseconds = delayMilliseconds;
        this.timer = new Timer();
    }

    public void call() {
        if (timer != null) {
            timer.cancel();
        }

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (asyncAction != null) {
                    asyncAction.run();
                } else {
                    action.run();
                }
            }
        }, delayMilliseconds);
    }
}
