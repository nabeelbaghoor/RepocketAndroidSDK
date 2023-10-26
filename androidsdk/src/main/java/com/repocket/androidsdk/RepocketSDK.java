package com.repocket.androidsdk;

import android.content.Context;
import android.os.StrictMode;
import android.util.Log;

import com.repocket.androidsdk.services.PeerService;
import com.repocket.androidsdk.shared.MyPlayerPrefs;
import com.repocket.androidsdk.shared.NetworkCheck;

import org.json.JSONException;

import java.io.IOException;

public class RepocketSDK {

    private static String _sdkApiKey = "";
    static PeerService _peerService;

    public static void Initialize(Context context, String sdkApiKey)
    {
        Log.d("RepocketSDK", "RepocketSDK -> Initialize");

        NetworkCheck.instance().setContext(context);

        _sdkApiKey = sdkApiKey;
        MyPlayerPrefs.SetString("sdk-api-key", sdkApiKey);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        _peerService = new PeerService(null,null,_sdkApiKey, null);
        _peerService.onConnected.addListener(x -> Log.d("RepocketSDK","RepocketSDK -> Initialize -> peer connected: " + x));
        _peerService.onConnecting.addListener(x -> Log.d("RepocketSDK","RepocketSDK -> Initialize -> peer resetting: " + x));
        _peerService.onDisconnected.addListener(x -> Log.d("RepocketSDK","RepocketSDK -> Initialize -> peer disconnected: " + x));
        _peerService.onRefreshTokenRequired.addListener(x -> Log.d("RepocketSDK","RepocketSDK -> Initialize -> peer refresh_token_required: " + x)); ;
    }

    public static void CreatePeer()
    {
        _peerService.createPeer();
    }

    public static void StopPeer()
    {
        _peerService.stop(false);
    }
}