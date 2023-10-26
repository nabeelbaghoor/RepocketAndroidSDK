package com.repocket.androidsdk.services;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.repocket.androidsdk.RepocketSDK;
import com.repocket.androidsdk.classes.ConnectionMonitor;
import com.repocket.androidsdk.classes.PeerMonitor;
import com.repocket.androidsdk.classes.VPNWatcher;
import com.repocket.androidsdk.shared.Callback;
import com.repocket.androidsdk.shared.Debouncer;
import com.repocket.androidsdk.shared.EventHandler;
import com.repocket.androidsdk.shared.MyPlayerPrefs;
import com.repocket.androidsdk.shared.Utils;
import com.repocket.androidsdk.types.Types;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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
    private List<Double> _responseTimes = new ArrayList<Double>() {{
        add(0.6);
        add(1.3);
        add(1.0);
        add(2.0);
        add(5.0);
        add(6.0);
        add(7.0);
    }};
    private boolean shouldResetConnection;
    private Types.TcpServerInfo tcpServerInfo;
    private boolean vpnStatus = false;
    private RemoteSettings _settings;

    public EventHandler onConnected = new EventHandler();
    public EventHandler onDisconnected = new EventHandler();
    public EventHandler onConnecting = new EventHandler();
    public EventHandler onRefreshTokenRequired = new EventHandler();

    public PeerService(String firebaseLoginToken, String peerApiToken, String sdkApiKey, String userId) {
        localId = new Random().nextInt(9000) + 1000;
        httpClient = new OkHttpClient();
        peerMonitor = new PeerMonitor();
        connectionMonitor = new ConnectionMonitor();
        vpnWatcher = new VPNWatcher();

        if (firebaseLoginToken == null && peerApiToken == null && sdkApiKey == null) {
            Log.d("RepocketSDK", "PeerService -> firebaseLoginToken or peerApiToken or sdkApiKey is required");
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

        resetPeerDebouncer = new Debouncer(o -> resetPeerDebounce(), 1000);
        handleConnectionClosedDebouncer = new Debouncer(o -> handleConnectionClosedDebounce(), 1000);
        deletePeerDebouncer = new Debouncer(o -> deletePeerDebounce(), 1000);
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

    public void createPeer() {
        if (isCreatingPeer || isPeerActive) {
            return;
        }

        isCreatingPeer = true;
        // Starting the local monitor before anything else, in case connection gets stuck and it never gets to peerMonitor or connectionMonitor init
        startLocalPeerMonitor();

        //  1. getting speed test
        //  2. getting device info
        Types.DeviceInfo deviceInfo = Utils.getDeviceInfo();

        //  3. getting ip info
        Types.IpInfo ipInfo = getIpInfo();

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
                put("ip", ipInfo != null ? ipInfo.query : ""); // TODO: Check behavior on the server
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
                String json = null;
                try {
                    json = response.body().string();
                } catch (IOException e) {
                    Log.d("RepocketSDK", "PeerService -> createPeer -> IOException: " + e);
                    throw new RuntimeException(e);
                }
                Types.CreatePeerResponse createPeer = Utils.fromJson(json, Types.CreatePeerResponse.class);

                peerId = createPeer != null ? createPeer.data._id : null;
                String token = createPeer != null ? createPeer.data.token : null;
                tcpServerInfo = createPeer != null ? createPeer.data.tcpServerInfo : null;

                Log.d("RepocketSDK","PeerService -> createPeer -> _tcpServerInfo?.ip: " + (tcpServerInfo != null ? tcpServerInfo.ip : ""));
                Log.d("RepocketSDK", "PeerService -> createPeer -> _tcpServerInfo?.port: " + (tcpServerInfo != null ? tcpServerInfo.port : ""));
                Log.d("RepocketSDK", "_PeerService -> createPeer -> tcpServerInfo?.socketReqHandlerPort: " + (tcpServerInfo != null ? tcpServerInfo.socketReqHandlerPort : ""));

                if (userId == null || userId.isEmpty()) {
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
                    Log.d("RepocketSDK", "PeerService -> createPeer: TcpServerInfo is null");
                }
            } else {
                Log.d("RepocketSDK", "PeerService -> createPeer -> Failed to create connection: HttpStatusCode(" + response.code() + ")");
                if (response.code() == 403) {
                    onRefreshTokenRequired.broadcast("PeerService -> createPeer -> Failed to create connection: HttpStatusCode(" + response.code() + ")");
                }
                isCreatingPeer = false;
            }
        }

        isCreatingPeer = false;
    }

    private Types.PeerConfigResponse getPeerConfig() {
        Response response = Services.PeerManagerApiService.Get(PeerConfigEndpoint, null);
        String responseData = null;
        try {
            responseData = response.body().string();
        } catch (IOException e) {
            Log.d("RepocketSDK", "PeerService -> getPeerConfig -> IOException: " + e);
            throw new RuntimeException(e);
        }

        if (response.isSuccessful()) {
            return Utils.fromJson(responseData, Types.PeerConfigResponse.class);
        } else {
            Log.d("RepocketSDK", "PeerService -> getPeerConfig -> Failed to get peer config: " + responseData);
            return null;
        }
    }

    private Types.IpInfo getIpInfo() {
        Request request = new Request.Builder()
                .url("http://ip-api.com/json")
                .get()
                .build();
        Response response = null;
        try {
            response = httpClient.newCall(request).execute();
            String responseData = response.body().string();

            if (response.isSuccessful()) {
                return Utils.fromJson(responseData, Types.IpInfo.class);
            } else {
                Log.d("RepocketSDK", "PeerService -> getIpInfo -> Something went wrong");
                return null;
            }
        } catch (IOException e) {
            Log.d("RepocketSDK", "PeerService -> getIpInfo -> IOException: " + e);
            throw new RuntimeException(e);
        }
    }

    private void _handleConnectionClosed() {
        handleConnectionClosedDebouncer.call("handleConnectionClosedDebouncer");
    }

    private void resetPeerDebounce() {
        Log.d("RepocketSDK", "PeerService(" + localId + ") -> resetPeer");
        stop(false);
        onConnecting.broadcast(null);

        new Handler(Looper.getMainLooper()).postDelayed(() -> createPeer(), 3000);
        new Handler(Looper.getMainLooper()).postDelayed(() -> isResettingPeer = false, 5000);
    }

    private void OnSocketConnectionFailedDebounce() {
        if (!isCreatingPeer && IsConnectivityChanged) {
            _handleConnectionClosed();
        }
    }

    private void onReceiveDataDebounce() {
        Log.d("RepocketSDK", "PeerService -> onReceiveDataDebounce -> onSocketConnectionFailed: " + isUserActivatedThePeer);
        if (!isPeerActive) {
            isPeerActive = true;
            onConnected.broadcast("onSocketConnectionFailed " + isUserActivatedThePeer);
        }
    }

    private void onConnectionToServerFailedDebounce() {
        Log.d("RepocketSDK", "PeerService -> onConnectionToServerFailedDebounce -> onConnectionToServerFailed " + isUserActivatedThePeer);
        onDisconnected.broadcast("onConnectionToServerFailed " + isUserActivatedThePeer);
        stop(false);
    }

    private void onSocketConnectionCloseDebounce() {
        Log.d("RepocketSDK", "PeerService -> onSocketConnectionCloseDebounce -> onSocketConnectionClose " + isUserActivatedThePeer);
        if (!isCreatingPeer) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                _handleConnectionClosed();
            }, 1000);
        }
    }

    private void onConnectionEstablishedDebounce() {
        Log.d("RepocketSDK", "PeerService -> onConnectionEstablishedDebounce -> PeerService(" + localId + ") -> Connection established");
        _markPeerAsAlive(); // TODO: Some part of it needs to be on the main thread?
    }

    private void registerEventListeners() {
        Debouncer onSocketConnectionFailedDebounce = new Debouncer(o -> OnSocketConnectionFailedDebounce(), 1000);
        Debouncer onReceiveDataDebounce = new Debouncer(o -> onReceiveDataDebounce(), 1000);
        Debouncer onConnectionToServerFailedDebounce = new Debouncer(o -> onConnectionToServerFailedDebounce(), 1000);
        Debouncer onSocketConnectionCloseDebounce = new Debouncer(o -> onSocketConnectionCloseDebounce(), 1000);
        Debouncer onConnectionEstablishedDebounce = new Debouncer(o -> onConnectionEstablishedDebounce(), 1000);

        p2PServiceInstance.BeforeStartSocketConnection.addListener(beforeStartSocketConnection -> {
            onConnecting.broadcast("");

            // Register all event listeners here
            p2PServiceInstance.SocketConnectionFailed.addListener(socketConnectionFailed -> {
                if (!isCreatingPeer && !IsConnectivityChanged) {
                    onSocketConnectionFailedDebounce.call("onSocketConnectionFailedDebounce");
                }
            });

            p2PServiceInstance.ReceiveData.addListener(receiveData -> onReceiveDataDebounce.call("onReceiveDataDebounce"));

            p2PServiceInstance.ConnectionToServerFailed.addListener(connectionToServerFailed -> onConnectionToServerFailedDebounce.call("onConnectionToServerFailedDebounce"));

            p2PServiceInstance.SocketConnectionClose.addListener(socketConnectionClose -> onSocketConnectionCloseDebounce.call("onSocketConnectionCloseDebounce"));

            p2PServiceInstance.ConnectionEstablished.addListener(connectionEstablished -> onConnectionEstablishedDebounce.call("onConnectionEstablishedDebounce"));
        });
    }

    private void _markPeerAsAlive() {
        Log.d("RepocketSDK", "PeerService -> markPeerAsAlive -> start - " + peerId);
        Response response = null;
        response = Services.PeerManagerApiService.Post("peer/markPeerAsAlive", new HashMap<String, Object>() {{
            put("peerId", peerId);
        }});
        boolean isAlive = response.code() == 200;
        if (isAlive) {
            Log.d("RepocketSDK", "PeerService -> _markPeerAsAlive -> markPeerAsAlive -> isAlive");
            isPeerActive = true;
            onConnected.broadcast("");
            enabledLocalMonitor = true;
            startPeerMonitor();
            startConnectionMonitor();
            startVpnWatcher();
        } else if (response.code() == 403) {
            Log.d("RepocketSDK", "PeerService -> _markPeerAsAlive -> Error happened: HttpStatusCode(" + response.code() + ")");
            onRefreshTokenRequired.broadcast("_markPeerAsAlive -> Error happened: HttpStatusCode(" + response.code() + ")");
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
        peerMonitor.start(this::_handleConnectionClosed, this::credentialsMissing, this::_verifyUIStatus, _settings.peerMonitorRate);
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
            Log.d("RepocketSDK", "PeerService -> localPeerMonitorTimerElapsed: checking for unhandled disconnects");
            String status = MyPlayerPrefs.GetString("connectionStatus");
            if (!"connected".equals(status)) {
                Log.d("RepocketSDK", "PeerService -> localPeerMonitorTimerElapsed: not connected, checking internet connection");
                ConnectionMonitor monitor = new ConnectionMonitor();
                monitor.setDuration(1000);
                monitor.start(() -> {
                    Log.d("RepocketSDK", "PeerService -> localPeerMonitorTimerElapsed: No Internet connection");
                    monitor.stop();
                }, () -> {
                    Log.d("RepocketSDK", "PeerService -> localPeerMonitorTimerElapsed: Internet connection present, resetting peer");
                    monitor.stop();
                    try {
                        _resetPeer();
                        Log.d("RepocketSDK", "PeerService -> localPeerMonitorTimerElapsed: Reset peer after an unhandled disconnect");
                    } catch (Exception exception) {
                        Log.d("RepocketSDK", "PeerService -> localPeerMonitorTimerElapsed -> Error resetting peer: " + exception.getMessage());
                        throw new RuntimeException(exception);
                    }
                });
            }
        }
    }

    private void _verifyUIStatus() {
        isPeerActive = true;
        onConnected.broadcast("");
    }

    private void _resetPeer() {
        if (isResettingPeer) {
            return;
        }

        isResettingPeer = true;
        resetPeerDebouncer.call("resetPeerDebouncer");
    }

    private void handleConnectionClosedDebounce() {
        Log.d("RepocketSDK", "PeerService(" + localId + ") -> handleConnectionClosedDebounce");
        onDisconnected.broadcast("PeerService(" + localId + ") -> handleConnectionClosedDebounce");
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
        Log.d("RepocketSDK", "PeerService -> deletePeerDebounce");
        stop(false);
    }

    private void deletePeer(boolean isForceStop) {
        if (isResettingPeer && !isForceStop) {
            return;
        }

        if (isForceStop) {
            stop(false);
        } else {
            deletePeerDebouncer.call("deletePeerDebouncer");
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
        Log.d("RepocketSDK", "PeerService(" + localId + ") -> stop");
        if (peerId != null) {
            peerMonitor.stop();
            if (!keepConnectionMonitor) {
                connectionMonitor.stop();
            }
            vpnWatcher.stop();
            onDisconnected.broadcast("");
            try {
                Services.PeerManagerApiService.Delete("peer/deletePeer", new HashMap<String, Object>() {{
                    put("peerId", peerId);
                }});
            } catch (Exception e) {
                Log.d("RepocketSDK", "PeerService(" + localId + ") -> stop -> exception: " + e.getMessage());
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
