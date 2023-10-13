package com.repocket.androidsdk.classes;

import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;

public class ConnectionMonitor {

    private boolean isConnectionActive = false;
    private boolean isRunning;
    private Timer timer;
    private int duration = 60000;
    public void setDuration(int _duration){
        duration = _duration;
    }

    public void init() {
        Log.d("RepocketSDK", "onInit");
    }

    public void start(final Runnable onConnectionDeactivate, final Runnable onConnectionActive) {
        if (!isRunning) {
            isRunning = true;
            timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    monitorHandler(onConnectionDeactivate, onConnectionActive);
                }
            }, 0, duration);
        }
    }

    public void stop() {
        if (isRunning) {
            isRunning = false;
            timer.cancel();
            Log.d("RepocketSDK", "Monitor Stopped.");
        }
    }

    private void monitorHandler(Runnable onConnectionDeactivate, Runnable onConnectionActive) {
        Log.d("ConnectionMonitor", "Connection monitor is running");

        try {
            if (checkConnection()) {
                Log.d("RepocketSDK", "Internet connection is present");
                onConnectionActive.run();
            } else {
                Log.d("RepocketSDK", "Internet connection is absent");
                onConnectionDeactivate.run();
            }
        } catch (Exception e) {
            Log.d("RepocketSDK", "Error checking internet connection: " + e.getMessage());
        }
    }

    private boolean checkConnection() {
        try {
            return checkInternet();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkInternet() {
        try {
            InetAddress address = InetAddress.getByName("google.com");
            return !address.equals("");
        } catch (UnknownHostException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
    }
}
