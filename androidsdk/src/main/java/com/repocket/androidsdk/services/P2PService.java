//package com.repocket.androidsdk.services;
//
//import android.os.Handler;
//import android.os.Looper;
//import android.util.Log;
//
//import com.repocket.androidsdk.P2P.MainSocket;
//
//import java.util.Timer;
//import java.util.TimerTask;
//
//public class P2PService {
//    private static final int MaxRetryPerHour = 30;
//    private final String ip;
//    private final String peerId;
//    private final int port;
//    private final Timer retriesInterval;
//    private final int socketReqHandlerPort;
//    private final String token;
//    private final String userId;
//    private MainSocket mainSocket;
//    private int retriesConnectionCounter;
//
//    public P2PService(String ip, int port, String peerId, String userId, String token, int socketReqHandlerPort) {
//        this.ip = ip;
//        this.port = port;
//        this.socketReqHandlerPort = socketReqHandlerPort;
//        this.peerId = peerId;
//        this.userId = userId;
//        this.token = token;
//
//        retriesInterval = new Timer();
//        retriesInterval.schedule(new TimerTask() {
//            @Override
//            public void run() {
//                retriesConnectionCounter = 0;
//            }
//        }, 1000 * 60 * 60, 1000 * 60 * 60);
//    }
//
//    public interface ConnectionListener {
//        void onConnectionEstablished();
//
//        void onSocketConnectionFailed();
//
//        void onBeforeStartSocketConnection();
//
//        void onServerCloseSocketConnection();
//
//        void onSocketConnectionClose();
//
//        void onConnectionToServerFailed();
//
//        void onReceiveData();
//
//        void onSocketClose();
//
//        void onCreateSocketRequestHandler();
//
//        void onTargetWebsiteError();
//    }
//
//    private ConnectionListener connectionListener;
//
//    public void setConnectionListener(ConnectionListener listener) {
//        connectionListener = listener;
//    }
//
//    public void removeAllListeners() {
//        // Implement logic to remove event listeners.
//    }
//
//    public void removeConnectionListener() {
//        connectionListener = null;
//    }
//
//    public boolean startSocketConnection() {
//        Log.d("P2PService", "start connection");
//
//        if (retriesConnectionCounter >= MaxRetryPerHour) {
//            retriesConnectionCounter = 0;
//            Log.d("P2PService", "P2PService -> onConnectionToServerFailed");
//            if (connectionListener != null) {
//                connectionListener.onConnectionToServerFailed();
//            }
//            return false;
//        }
//
//        retriesConnectionCounter++;
//        if (connectionListener != null) {
//            connectionListener.onBeforeStartSocketConnection();
//        }
//
//        mainSocket = new MainSocket(port, ip, peerId, token, userId, socketReqHandlerPort);
//        mainSocket.setConnectionListener(new MainSocket.ConnectionListener() {
//            @Override
//            public void onConnectionEstablished() {
//                if (connectionListener != null) {
//                    connectionListener.onConnectionEstablished();
//                }
//            }
//
//            @Override
//            public void onSocketConnectionFailed() {
//                if (connectionListener != null) {
//                    connectionListener.onSocketConnectionFailed();
//                }
//            }
//
//            @Override
//            public void onSocketConnectionClose() {
//                if (connectionListener != null) {
//                    connectionListener.onSocketConnectionClose();
//                }
//            }
//        });
//
//        boolean isConnected = mainSocket.connect();
//
//        if (isConnected) {
//            retriesConnectionCounter = 0;
//            return true;
//        }
//
//        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                startSocketConnection();
//            }
//        }, 1000 * 10);
//
//        return false;
//    }
//
//    public void end() {
//        Log.d("P2PService", "P2PService -> CLOSE SOCKET CONNECTION");
//        if (mainSocket != null) {
//            mainSocket.end();
//        }
//    }
//}
