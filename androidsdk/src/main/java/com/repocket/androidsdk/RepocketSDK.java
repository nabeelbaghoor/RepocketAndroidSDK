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
        _peerService.onConnected.addListener(x -> Log.d("RepocketSDK","RepocketSDK -> Initialize -> peer connected"));
        _peerService.onConnecting.addListener(x -> Log.d("RepocketSDK","RepocketSDK -> Initialize -> peer resetting"));
        _peerService.onDisconnected.addListener(x -> Log.d("RepocketSDK","RepocketSDK -> Initialize -> peer disconnected"));
        _peerService.onRefreshTokenRequired.addListener(x -> Log.d("RepocketSDK","RepocketSDK -> Initialize -> peer refresh_token_required")); ;
    }

    public static void CreatePeer()
    {
        try {
            _peerService.createPeer();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public static void StopPeer()
    {
        _peerService.stop(false);
    }
}