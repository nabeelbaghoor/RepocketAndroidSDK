package com.repocket.androidsdk.tests;

import android.util.Log;

import com.repocket.androidsdk.services.P2PService;

public class TestP2PService {
    public static void Init() {
        P2PService p2P = new P2PService(
                "127.0.0.1",
                7070,
                "test123",
                "user123",
                "testingToken",
                7072
        );

        p2P.ConnectionEstablished.addListener(x -> Log.d("RepocketSDK","TestP2PService -> Init -> onConnectionEstablished"));
        p2P.SocketConnectionFailed.addListener(x -> Log.d("RepocketSDK","TestP2PService -> Init -> onSocketConnectionFailed"));
        p2P.BeforeStartSocketConnection.addListener(x -> Log.d("RepocketSDK","TestP2PService -> Init -> onBeforeStartSocketConnection"));
        p2P.ServerCloseSocketConnection.addListener(x -> Log.d("RepocketSDK","TestP2PService -> Init -> onServerCloseSocketConnection"));
        p2P.SocketConnectionClose.addListener(x -> Log.d("RepocketSDK","TestP2PService -> Init -> onSocketConnectionClose"));
        p2P.ConnectionToServerFailed.addListener(x -> Log.d("RepocketSDK","TestP2PService -> Init -> onConnectionToServerFailed"));
        p2P.ReceiveData.addListener(x -> Log.d("RepocketSDK","TestP2PService -> Init -> onReceiveData"));
        p2P.SocketClose.addListener(x -> Log.d("RepocketSDK","TestP2PService -> Init -> onSocketClose"));

        p2P.startSocketConnection();
    }
}
