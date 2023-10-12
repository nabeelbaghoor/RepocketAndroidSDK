package com.repocket.androidsdk.services;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.repocket.androidsdk.classes.ConnectionMonitor;
import com.repocket.androidsdk.classes.PeerMonitor;
import com.repocket.androidsdk.classes.VPNWatcher;
import com.repocket.androidsdk.shared.Debouncer;
import com.repocket.androidsdk.shared.MyPlayerPrefs;
import com.repocket.androidsdk.shared.Utils;
import com.repocket.androidsdk.types.Types;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PeerService {
    private static final boolean IsConnectivityChanged = false;

    private static final String CreatePeerEndpoint = "peer/createPeer";
    private static final String PeerConfigEndpoint = "peer/config";
    private final ConnectionMonitor connectionMonitor;
    private final Debouncer deletePeerDebouncer;
    private final Debouncer handleConnectionClosedDebouncer;
    private final OkHttpClient httpClient;
    private final int localId;
    private final Timer localPeerMonitorTimer;
    private final PeerMonitor peerMonitor;
    private final Debouncer resetPeerDebouncer;
    private final VPNWatcher vpnWatcher;
    private Object auth = new HashMap<String, String>() {{
        put("username", "test");
        put("password", "test");
    }};
    private Boolean cachedPeerActivatedEver = null;
    private Timer connectivityNoneTimer;
    private Object connectivitySubscription = null;
    private boolean enabledLocalMonitor;
    private boolean isCreatingPeer;
    private boolean isPeerActive;
    private boolean isResettingPeer;
    private boolean isUserActivatedThePeer = true;
    private P2PService p2PServiceInstance;
    private String peerId;
    private String peerQuality = "Good";
    private List<Double> responseTimes = List.of(0.6, 1.3, 1.0, 2.0, 5.0, 6.0, 7.0);
    private boolean shouldResetConnection;
    private Types.TcpServerInfo tcpServerInfo;
    private boolean vpnStatus = false;
    private RemoteSettings _settings;

    public interface ConnectionListener {
        void onConnected();

        void onDisconnected();

        void onConnecting();

        void onRefreshTokenRequired();
    }

    private ConnectionListener connectionListener;

    public void setConnectionListener(ConnectionListener listener) {
        connectionListener = listener;
    }

    public void removeConnectionListener() {
        connectionListener = null;
    }

    public PeerService(String firebaseLoginToken, String peerApiToken, String sdkApiKey, String userId) {
        localId = new Random().nextInt(9000) + 1000;
        httpClient = new OkHttpClient();
        peerMonitor = new PeerMonitor();
        connectionMonitor = new ConnectionMonitor();
        vpnWatcher = new VPNWatcher();

        if (firebaseLoginToken == null && peerApiToken == null && sdkApiKey == null) {
            throw new RuntimeException("firebaseLoginToken or peerApiToken or sdkApiKey is required");
        }

        if (firebaseLoginToken != null) {
            MyPlayerPrefs.SetString("loginToken", firebaseLoginToken);
        }
        if (sdkApiKey != null) {
            MyPlayerPrefs.SetString("sdk-api-key", sdkApiKey);
        }
        if (peerApiToken != null) {
            MyPlayerPrefs.SetString("p-api-token", peerApiToken);
        }
        if (userId != null) {
            MyPlayerPrefs.SetString("userId", userId);
        }

        resetPeerDebouncer = new Debouncer(this::_resetPeer, 1000);
        handleConnectionClosedDebouncer = new Debouncer(this::handleConnectionClosedDebounce, 1000);
        deletePeerDebouncer = new Debouncer(this::deletePeerDebounce, 1000);
        localPeerMonitorTimer = new Timer();
        localPeerMonitorTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                localPeerMonitorTimerElapsed();
            }
        }, 34300, 34300);
        _settings = new RemoteSettings();
        _settings.setPeerMonitorRate(120000);
    }

    public void createPeer() throws IOException, JSONException {
        if (isCreatingPeer || isPeerActive) {
            return;
        }

        isCreatingPeer = true;
        // Starting the local monitor before anything else, in case connection gets stuck and it never gets to peerMonitor or connectionMonitor init
        startLocalPeerMonitor();

        //  1. getting speed test
        //  2. getting device info
        Types.DeviceInfo deviceInfo = Utils.getDeviceInfo().join();

        //  3. getting ip info
        JSONObject ipInfo = getIpInfo();

        // 4. check connectivity
        boolean isConnectivityCheck = true; // ConnectivityData.connectivityCheck(); // TODO: Complete logic

        // 5. check VPN status
        boolean isVpnConnect = false; // isVpnActive(); // TODO: Complete logic

        // 6. establish connection - POST /api/peer/createPeer - payload: { ip, username, password, userId, userDeviceInfo, speedTestInfo }
        if (isConnectivityCheck && !isVpnConnect) {
            String userId = MyPlayerPrefs.GetString("userId");
            Types.PeerConfigResponse peerConfig = getPeerConfig();
            String versionToken = peerConfig != null ? peerConfig.data.config_version_token : null;
            MyPlayerPrefs.SetString("configVersionToken", versionToken != null ? versionToken : "");

            String finalUserId = userId;
            Response response = Services.PeerManagerApiService.Post(CreatePeerEndpoint, new HashMap<String, Object>() {{
                put("ip", ipInfo != null ? ipInfo.optString("query") : ""); // TODO: Check behavior on the server
                put("username", "test");
                put("password", "test");
                put("userId", finalUserId);
                put("userDeviceInfo", new HashMap<String, Object>() {{
                    put("cpus", deviceInfo.cpus);
                    put("id", deviceInfo.id);
                    put("device", deviceInfo.device);
                    put("isPhysicalDevice", deviceInfo.isPhysicalDevice);
                    put("model", deviceInfo.model);
                    put("version", deviceInfo.version);
                    put("isDesktop", deviceInfo.isDesktop);
                    put("buildNumber", deviceInfo.buildNumber);
                    put("connectivityType", deviceInfo.connectivityType);
                    put("os", deviceInfo.os);
                }});
            }});

            if (response.isSuccessful()) {
                String json = response.body().string();
                Types.CreatePeerResponse createPeer = Utils.fromJson(json, Types.CreatePeerResponse.class);

                peerId = createPeer != null ? createPeer.data._id : null;
                String token = createPeer != null ? createPeer.data.token : null;
                tcpServerInfo = createPeer != null ? createPeer.data.tcpServerInfo : null;

                Log.d("PeerService", "_tcpServerInfo?.ip: " + (tcpServerInfo != null ? tcpServerInfo.ip : ""));
                Log.d("PeerService", "_tcpServerInfo?.port: " + (tcpServerInfo != null ? tcpServerInfo.port : ""));
                Log.d("PeerService", "_tcpServerInfo?.socketReqHandlerPort: " + (tcpServerInfo != null ? tcpServerInfo.socketReqHandlerPort : ""));

                if (userId == null && createPeer != null) {
                    userId = createPeer.data.userId;
                    MyPlayerPrefs.SetString("userId", userId != null ? userId : "");
                }

                if (tcpServerInfo != null) {
                    p2PServiceInstance = new P2PService(tcpServerInfo.ip, tcpServerInfo.port, peerId, userId, token,
                            tcpServerInfo.socketReqHandlerPort);

                    registerEventListeners();

                    boolean isSocketConnected = p2PServiceInstance.startSocketConnection();
                    if (isSocketConnected) {
                        isUserActivatedThePeer = true;
                        isPeerActive = true;
                    }

                    isCreatingPeer = false;
                } else {
                    Log.d("PeerService", "TcpServerInfo is null");
                }
            } else {
                Log.d("PeerService", "Failed to create connection: HttpStatusCode(" + response.code() + ")");
                if (response.code() == 403) {
                    if (connectionListener != null) {
                        connectionListener.onRefreshTokenRequired();
                    }
                }
                isCreatingPeer = false;
            }
        }

        isCreatingPeer = false;
    }

    private Types.PeerConfigResponse getPeerConfig() throws IOException, JSONException {
        Response response = Services.PeerManagerApiService.Get(PeerConfigEndpoint, null);
        String responseData = response.body().string();

        if (response.isSuccessful()) {
            return Utils.fromJson(responseData, Types.PeerConfigResponse.class);
        } else {
            Log.d("PeerService", "GetPeerConfig: Failed to get peer config: " + responseData);
            return null;
        }
    }

    private JSONObject getIpInfo() throws IOException {
        Request request = new Request.Builder()
                .url("http://ip-api.com/json")
                .get()
                .build();
        Response response = httpClient.newCall(request).execute();
        String responseData = response.body().string();

        if (response.isSuccessful()) {
            try {
                return new JSONObject(responseData);
            } catch (JSONException e) {
                Log.e("PeerService", "Error parsing JSON: " + e.getMessage());
                return null;
            }
        } else {
            Log.d("PeerService", "GetIpInfo: Something went wrong");
            return null;
        }
    }

    private void _handleConnectionClosed() {
        handleConnectionClosedDebouncer.call();
    }

    private void resetPeerDebounce() {
        if (!isCreatingPeer && IsConnectivityChanged) {
            _handleConnectionClosed();
        }
    }

    private void onReceiveDataDebounce() {
        Log.d("PeerService", "onSocketConnectionFailed " + isUserActivatedThePeer);
        if (!isPeerActive) {
            isPeerActive = true;
            if (connectionListener != null) {
                connectionListener.onConnected();
            }
        }
    }

    private void onConnectionToServerFailedDebounce() {
        Log.d("PeerService", "onConnectionToServerFailed " + isUserActivatedThePeer);
        if (connectionListener != null) {
            connectionListener.onDisconnected();
        }
        stop(false);
    }

    private void onSocketConnectionCloseDebounce() {
        Log.d("PeerService", "onSocketConnectionClose " + isUserActivatedThePeer);
        if (!isCreatingPeer) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                _handleConnectionClosed();
            }, 1000);
        }
    }

    private void onConnectionEstablishedDebounce() {
        Log.d("PeerService", "PeerService(" + localId + ") -> Connection established");
        _markPeerAsAlive(); // TODO: Some part of it needs to be on the main thread?
    }

    private void registerEventListeners() {
        Debouncer onSocketConnectionFailedDebounce = new Debouncer(this::onSocketConnectionFailedDebounce, 1000);
        Debouncer onReceiveDataDebounce = new Debouncer(this::onReceiveDataDebounce, 1000);
        Debouncer onConnectionToServerFailedDebounce = new Debouncer(this::onConnectionToServerFailedDebounce, 1000);
        Debouncer onSocketConnectionCloseDebounce = new Debouncer(this::onSocketConnectionCloseDebounce, 1000);
        Debouncer onConnectionEstablishedDebounce = new Debouncer(this::onConnectionEstablishedDebounce, 1000);

        p2PServiceInstance.setBeforeStartSocketConnection((_, _) -> {
            if (connectionListener != null) {
                connectionListener.onConnecting();
            }

            // Register all event listeners here
            p2PServiceInstance.setSocketConnectionFailed((_, _) -> {
                if (!isCreatingPeer && !IsConnectivityChanged) {
                    onSocketConnectionFailedDebounce.call();
                }
            });

            p2PServiceInstance.setReceiveData((_, _) -> {
                onReceiveDataDebounce.call();
            });

            p2PServiceInstance.setConnectionToServerFailed((_, _) -> {
                onConnectionToServerFailedDebounce.call();
            });

            p2PServiceInstance.setSocketConnectionClose((_, _) -> {
                onSocketConnectionCloseDebounce.call();
            });

            p2PServiceInstance.setConnectionEstablished((_, _) -> {
                onConnectionEstablishedDebounce.call();
            });
        });
    }

    private void _markPeerAsAlive() {
        Log.d("PeerService", "markPeerAsAlive -> start - " + peerId);
        Response response = null;
        try {
            response = Services.PeerManagerApiService.Post("peer/markPeerAsAlive", new HashMap<String, Object>() {{
                put("peerId", peerId);
            }});
        } catch (JSONException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        boolean isAlive = response.code() == 200;
        if (isAlive) {
            Log.d("PeerService", "markPeerAsAlive -> isAlive");
            isPeerActive = true;
            if (connectionListener != null) {
                connectionListener.onConnected();
            }
            enabledLocalMonitor = true;
            startPeerMonitor();
            startConnectionMonitor();
            startVpnWatcher();
        } else if (response.code() == 403) {
            Log.d("PeerService", "_markPeerAsAlive -> Error happened: HttpStatusCode(" + response.code() + ")");
            if (connectionListener != null) {
                connectionListener.onRefreshTokenRequired();
            }
        }
    }

    private void startVpnWatcher() {
        String userId = MyPlayerPrefs.GetString("userId");
        vpnWatcher.init(peerId, userId);
        vpnWatcher.start(this::onVpnActivated);
    }

    private void onVpnActivated() {
        deletePeer(false);
    }

    private void startPeerMonitor() {
        String userId = MyPlayerPrefs.GetString("userId");
        String versionToken = MyPlayerPrefs.GetString("configVersionToken");
        peerMonitor.init(peerId, userId, versionToken);
        peerMonitor.start(this::_handleConnectionClosed, this::credentialsMissing, this::_verifyUIStatus, _settings.PeerMonitorRate);
    }

    private void credentialsMissing() {
        stop(false);
    }

    private void startConnectionMonitor() {
        connectionMonitor.init();
        connectionMonitor.start(this::onConnectionDeactivate, this::onConnectionActive);
    }

    private void onConnectionActive() {
        if (shouldResetConnection) {
            shouldResetConnection = false;
            _resetPeer();
        }
    }

    private void onConnectionDeactivate() {
        if (!shouldResetConnection) {
            shouldResetConnection = true;
            stop(true);
        }
    }

    private void startLocalPeerMonitor() {
        if (!enabledLocalMonitor) {
            enabledLocalMonitor = true;
            localPeerMonitorTimerElapsed();
        }
    }

    private void localPeerMonitorTimerElapsed() {
        if (isResettingPeer || isCreatingPeer) {
            return;
        }

        boolean isEnabled = MyPlayerPrefs.GetInt("shareInternet") == 1;
        if (isEnabled) {
            Log.d("PeerService", "startLocalPeerMonitor -> checking for unhandled disconnects");
            String status = MyPlayerPrefs.GetString("connectionStatus");
            if (!"connected".equals(status)) {
                Log.d("PeerService", "startLocalPeerMonitor -> not connected, checking internet connection");
                ConnectionMonitor monitor = new ConnectionMonitor();
                monitor.setDuration(1000);
                monitor.start(() -> {
                    Log.d("PeerService", "startLocalPeerMonitor -> No Internet connection");
                    monitor.stop();
                }, () -> {
                    Log.d("PeerService", "startLocalPeerMonitor -> Internet connection present, resetting peer");
                    monitor.stop();
                    try {
                        _resetPeer();
                        Log.d("PeerService", "startLocalPeerMonitor -> Reset peer after an unhandled disconnect");
                    } catch (Exception exception) {
                        Log.e("PeerService", "startLocalPeerMonitor -> Error resetting peer: " + exception.getMessage());
                        throw new RuntimeException(exception);
                    }
                });
            }
        }
    }

    private void _verifyUIStatus() {
        isPeerActive = true;
        if (connectionListener != null) {
            connectionListener.onConnected();
        }
    }

    private void _resetPeer() {
        if (isResettingPeer) {
            return;
        }

        isResettingPeer = true;
        resetPeerDebounce();
    }

    private void handleConnectionClosedDebounce() {
        Log.d("PeerService", "PeerService(" + localId + ") -> _handleConnectionClosed");
        if (connectionListener != null) {
            connectionListener.onDisconnected();
        }
        if (_shouldReconnectThePeer()) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                _resetPeer();
            }, 3000);
        } else {
            if (!isUserActivatedThePeer) {
                deletePeer(false);
            }
        }
    }

    private void deletePeerDebounce() {
        Log.d("PeerService", "deletePeerDebounce");
        stop(false);
    }

    private void deletePeer(boolean isForceStop) {
        if (isResettingPeer && !isForceStop) {
            return;
        }

        if (isForceStop) {
            stop(false);
        } else {
            deletePeerDebouncer.call();
        }
    }

    private boolean _shouldReconnectThePeer() {
        return isUserActivatedThePeer;
    }

    /**
     * Stops the created peer on repocket network
     *
     * @param keepConnectionMonitor Whether the connection monitor should keep running or not
     */
    public void stop(boolean keepConnectionMonitor) {
        Log.d("PeerService", "PeerService(" + localId + ") -> Stop");
        if (peerId != null) {
            peerMonitor.stop();
            if (!keepConnectionMonitor) {
                connectionMonitor.stop();
            }
            vpnWatcher.stop();
            if (connectionListener != null) {
                connectionListener.onDisconnected();
            }
            try {
                Services.PeerManagerApiService.Delete("peer/deletePeer", new HashMap<String, Object>() {{
                    put("peerId", peerId);
                }});
            } catch (Exception e) {
                Log.e("PeerService", "PeerService(" + localId + ") -> exception: " + e.getMessage());
            }

            p2PServiceInstance.end();
            p2PServiceInstance.removeAllListeners();
            isPeerActive = false;
            peerId = null;
        }
    }

    private class RemoteSettings {
        private int peerMonitorRate;

        public int getPeerMonitorRate() {
            return peerMonitorRate;
        }

        public void setPeerMonitorRate(int peerMonitorRate) {
            this.peerMonitorRate = peerMonitorRate;
        }
    }
}
