package com.repocket.androidsdk.classes;

import static com.repocket.androidsdk.services.Services.MonitorApiService;

import android.util.Log;

import com.android.volley.Response;
import com.android.volley.toolbox.HttpResponse;
import com.repocket.androidsdk.shared.Utils;
import com.repocket.androidsdk.types.Types;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class PeerMonitor {

    private String configVersionToken;
    private Timer interval;
    private boolean isPeerActive;
    private boolean isRunning;
    private Runnable onPeerDeactivate;
    private String peerId;
    private int second = 7;
    private String userId;

    public void init(String peerId, String userId, String configVersionToken) {
        Log.d("PeerMonitor", "onInit");
        this.peerId = peerId;
        this.userId = userId;
        this.configVersionToken = configVersionToken;
    }

    public void start(final Runnable peerDeactivate, final Runnable credentialsMissing, final Runnable peerActive, int rate) {
        // runs every 5 seconds
        if (interval == null) {
            isRunning = true;
            interval = new Timer();
            interval.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    monitorHandler(peerDeactivate, credentialsMissing, peerActive);
                }
            }, 0, TimeUnit.SECONDS.toMillis(rate));
        }
    }

    public void stop() {
        if (interval != null) {
            isRunning = false;
            interval.cancel();
            interval = null;
            Log.d("PeerMonitor", "Monitor Stopped.");
        }
    }

    private void monitorHandler(Runnable onPeerDeactivate, Runnable onCredentialsMissing, Runnable onPeerActive) {
        Log.d("PeerMonitor", "peer monitor is running " + userId + " / " + peerId);

        String token = Utils.getToken();
        if (token == null || token.isEmpty()) {
            Log.d("PeerMonitor", "User credentials missing, disconnecting");
            onCredentialsMissing.run();
            return;
        }

        try {
            String url = "peer/monitor/" + userId + "/" + peerId;
            if (configVersionToken != null && !configVersionToken.isEmpty()) {
                url += "/" + configVersionToken;
            }

            HttpResponse response = MonitorApiService.Get(url, null).join();
            if (response != null && response.getStatusCode() == 200 && response != null) {
                String json = response.getContent().
                Types.PeerMonitorApiResponse peerMonitor = Utils.fromJson(json, Types.PeerMonitorApiResponse.class);

                isPeerActive = peerMonitor.data.active && !peerMonitor.data.isConfigurationUpdated;

                if (!isPeerActive) {
                    onPeerDeactivate.run();
                    stop();
                } else {
                    onPeerActive.run();
                }
            } else {
                onPeerDeactivate.run();
                stop();
            }
        } catch (IOException error) {
            Log.e("PeerMonitor", "Peer monitor error: " + error.getMessage());
        }
    }
}
