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

        p2P.ConnectionEstablished.addListener(x -> Log.d("RepocketSDK","onConnectionEstablished"));
        p2P.SocketConnectionFailed.addListener(x -> Log.d("RepocketSDK","onSocketConnectionFailed"));
        p2P.BeforeStartSocketConnection.addListener(x -> Log.d("RepocketSDK","onBeforeStartSocketConnection"));
        p2P.ServerCloseSocketConnection.addListener(x -> Log.d("RepocketSDK","onServerCloseSocketConnection"));
        p2P.SocketConnectionClose.addListener(x -> Log.d("RepocketSDK","onSocketConnectionClose"));
        p2P.ConnectionToServerFailed.addListener(x -> Log.d("RepocketSDK","onConnectionToServerFailed"));
        p2P.ReceiveData.addListener(x -> Log.d("RepocketSDK","onReceiveData"));
        p2P.SocketClose.addListener(x -> Log.d("RepocketSDK","onSocketClose"));

        p2P.startSocketConnection();
    }
}
