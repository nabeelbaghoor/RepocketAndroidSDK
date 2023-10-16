package com.repocket.androidsdk.shared;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class Debouncer {
    private Runnable action = null;
    private Supplier<CompletableFuture<Void>> asyncAction = null;
    private final int delayMilliseconds;
    private Timer timer;
    private CompletableFuture<Void> tcs;

    public Debouncer(Runnable action, int delayMilliseconds) {
        this.action = action;
        this.delayMilliseconds = delayMilliseconds;
        this.timer = new Timer();
        this.timer.schedule(new TimerTask() {
            @Override
            public void run() {
                timerElapsed();
            }
        }, delayMilliseconds, delayMilliseconds);
    }

    public Debouncer(Supplier<CompletableFuture<Void>> asyncAction, int delayMilliseconds) {
        this.asyncAction = asyncAction;
        this.delayMilliseconds = delayMilliseconds;
        this.timer = new Timer();
        this.timer.schedule(new TimerTask() {
            @Override
            public void run() {
                timerElapsed();
            }
        }, delayMilliseconds, delayMilliseconds);
    }

    public void call() {
        if (timer != null) {
            timer.cancel();
        }

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                timerElapsed();
            }
        }, delayMilliseconds);
    }

    private void timerElapsed() {
        if (timer != null) {
            timer.cancel();
        }
        tcs = new CompletableFuture<>();

        if (asyncAction != null) {
            asyncAction.get().thenRun(() -> {
                action.run();
                tcs.complete(null);
            });
        } else {
            action.run();
            tcs.complete(null);
        }

        tcs = null;
    }
}