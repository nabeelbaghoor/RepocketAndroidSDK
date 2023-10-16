package com.repocket.androidsdk.shared;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Debouncer <T> {
    private final ScheduledExecutorService sched = Executors.newScheduledThreadPool(1);
    private final ConcurrentHashMap<T, TimerTask> delayedMap = new ConcurrentHashMap<T, TimerTask>();
    private final Callback<T> callback;
    private final int interval;

    public Debouncer(Callback<T> c, int interval) {
        this.callback = c;
        this.interval = interval;
    }

    public void call(T key) {
        TimerTask task = new TimerTask(key);

        TimerTask prev;
        do {
            prev = delayedMap.putIfAbsent(key, task);
            if (prev == null)
                sched.schedule(task, interval, TimeUnit.MILLISECONDS);
        } while (prev != null && !prev.extend()); // Exit only if new task was added to map, or existing task was extended successfully
    }

    public void terminate() {
        sched.shutdownNow();
    }

    // The task that wakes up when the wait time elapses
    private class TimerTask implements Runnable {
        private final T key;
        private long dueTime;
        private final Object lock = new Object();

        public TimerTask(T key) {
            this.key = key;
            extend();
        }

        public boolean extend() {
            synchronized (lock) {
                if (dueTime < 0) // Task has been shutdown
                    return false;
                dueTime = System.currentTimeMillis() + interval;
                return true;
            }
        }

        public void run() {
            synchronized (lock) {
                long remaining = dueTime - System.currentTimeMillis();
                if (remaining > 0) { // Re-schedule task
                    sched.schedule(this, remaining, TimeUnit.MILLISECONDS);
                } else { // Mark as terminated and invoke callback
                    dueTime = -1;
                    try {
                        callback.call(key);
                    } finally {
                        delayedMap.remove(key);
                    }
                }
            }
        }
    }
}



//
//import java.util.Timer;
//import java.util.TimerTask;
//import java.util.concurrent.CompletableFuture;
//import java.util.function.Supplier;
//
//public class Debouncer {
//    private Runnable action = null;
//    private Supplier<CompletableFuture<Void>> asyncAction = null;
//    private final int delayMilliseconds;
//    private Timer timer;
//    private CompletableFuture<Void> tcs;
//
//    public Debouncer(Runnable action, int delayMilliseconds) {
//        this.action = action;
//        this.delayMilliseconds = delayMilliseconds;
//        this.timer = new Timer();
//        this.timer.schedule(new TimerTask() {
//            @Override
//            public void run() {
//                timerElapsed();
//            }
//        }, delayMilliseconds, delayMilliseconds);
//    }
//
//    public Debouncer(Supplier<CompletableFuture<Void>> asyncAction, int delayMilliseconds) {
//        this.asyncAction = asyncAction;
//        this.delayMilliseconds = delayMilliseconds;
//        this.timer = new Timer();
//        this.timer.schedule(new TimerTask() {
//            @Override
//            public void run() {
//                timerElapsed();
//            }
//        }, delayMilliseconds, delayMilliseconds);
//    }
//
//    public void call() {
//        if (timer != null) {
//            timer.cancel();
//        }
//
//        timer = new Timer();
//        timer.schedule(new TimerTask() {
//            @Override
//            public void run() {
//                timerElapsed();
//            }
//        }, delayMilliseconds);
//    }
//
//    private void timerElapsed() {
//        if (timer != null) {
//            timer.cancel();
//        }
//        tcs = new CompletableFuture<>();
//
//        if (asyncAction != null) {
//            asyncAction.get().thenRun(() -> {
//                action.run();
//                tcs.complete(null);
//            });
//        } else {
//            action.run();
//            tcs.complete(null);
//        }
//
//        tcs = null;
//    }
//}