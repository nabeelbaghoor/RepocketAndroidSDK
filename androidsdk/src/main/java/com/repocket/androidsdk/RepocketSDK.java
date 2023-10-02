package com.repocket.androidsdk;

import android.os.StrictMode;
import android.util.Log;

//import com.repocket.androidsdk.services.HttpService;
//import com.repocket.androidsdk.services.Services;
import com.android.volley.toolbox.HttpResponse;
import com.repocket.androidsdk.services.HttpService;
import com.repocket.androidsdk.services.Services;
import com.repocket.androidsdk.shared.MyPlayerPrefs;
import com.repocket.androidsdk.types.Types;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class RepocketSDK {

    private static String _sdkApiKey = "";

    public static void Initialize(String sdkApiKey)
    {
        Log.d("RepocketSDK", "RepocketSDK -> Initialize");
        _sdkApiKey = sdkApiKey;
        MyPlayerPrefs.SetString("sdk-api-key", sdkApiKey);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        CompletableFuture<HttpResponse> peerConfigFuture = Services.PeerManagerApiService.Get("peer/config", null);
        peerConfigFuture.thenAccept(peerConfig -> {
            if (peerConfig != null) {
                // Process the peerConfig here
                Log.d("RepocketSDK", "Received Peer Config: " + peerConfig.getStatusCode());
            } {
                Log.d("RepocketSDK", "GetPeerConfig: Failed to get peer config: " + peerConfig.getStatusCode());
            }
        });

//        if (response.isSuccessStatusCode()) {
//            return new Gson().fromJson(responseData, Types.PeerConfigResponse.class);
//        } else {
//            Log.d("RepocketSDK", "GetPeerConfig: Failed to get peer config: " + peerConfigFuture.);
//            return null;
//        }
    }

    public static void CreatePeer()
    {
    }

    public static void StopPeer()
    {
    }
}