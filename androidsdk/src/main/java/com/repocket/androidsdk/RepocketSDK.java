package com.repocket.androidsdk;

import android.os.StrictMode;
import android.util.Log;

import com.repocket.androidsdk.services.PeerService;
import com.repocket.androidsdk.shared.DockerUtils;
import com.repocket.androidsdk.shared.EventHandler;
import com.repocket.androidsdk.shared.MyPlayerPrefs;

import org.json.JSONException;

import java.io.IOException;

public class RepocketSDK {

    private static String _sdkApiKey = "";

    public static void Initialize(String sdkApiKey)
    {
        Log.d("RepocketSDK", "RepocketSDK -> Initialize");
        _sdkApiKey = sdkApiKey;
        MyPlayerPrefs.SetString("sdk-api-key", sdkApiKey);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
//        DockerUtils.Initialize();
        PeerService _peerService = new PeerService(null,null,_sdkApiKey, null);
        _peerService.onConnected.addListener(x -> Log.d("RepocketSDK","peer connected"));
        _peerService.onConnecting.addListener(x -> Log.d("RepocketSDK","peer resetting"));
        _peerService.onDisconnected.addListener(x -> Log.d("RepocketSDK","peer disconnected"));
        _peerService.onRefreshTokenRequired.addListener(x -> Log.d("RepocketSDK","peer refresh_token_required")); ;

        try {
            _peerService.createPeer();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public static void CreatePeer()
    {
    }

    public static void StopPeer()
    {
    }
}