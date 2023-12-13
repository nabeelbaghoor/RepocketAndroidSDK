package com.repocket.androidsdk.classes;

import android.util.Log;

import java.util.Enumeration;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.net.NetworkInterface;
import java.net.SocketException;

public class VPNWatcher {
    private static final int DURATION = 10000;
    private boolean isRunning;
    private String peerId;
    private int second = 10;
    private Timer timer;
    private String userId;

    public void init(String peerId, String userId) {
        Log.d("RepocketSDK","VPNWatcher -> init");
        this.peerId = peerId;
        this.userId = userId;
    }

    public void start(Runnable onVpnActivated) {
        Log.d("RepocketSDK","VPNWatcher -> start");
        if (timer == null) {
            isRunning = true;

//            TimerTask timerTask = new TimerTask() {
//                @Override
//                public void run() {
//                    vpnIntervalHandler(() -> {
//                        onVpnActivated.run();
//                        return CompletableFuture.completedFuture(null);
//                    });
//                }
//            };

            timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    vpnIntervalHandler(onVpnActivated);
                }
            }, 0, DURATION);
        }
    }

    public void stop() {
        Log.d("RepocketSDK","VPNWatcher -> stop");
        if (timer != null) {
            isRunning = false;
            timer.cancel();
            timer = null;
        }
    }

    private void vpnIntervalHandler(Runnable onVpnActivated) {
        CompletableFuture<Boolean> isVpnConnect = isVpnActive();
        isVpnConnect.thenAccept(result -> {
            if (result) {
                onVpnActivated.run();
            }
        });
    }

    // TODO: Complete logic for checking if the user is connected via VPN
    private CompletableFuture<Boolean> isVpnActive() {
        boolean isVpnActive = false;
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

            // RPLogger.log("interfaces: ", interfaces);
        } catch (SocketException e) {
            Log.e("RepocketSDK", "VPNWatcher -> isVpnActive:" + e.getMessage());
        }

        return CompletableFuture.completedFuture(isVpnActive);
    }
}
