package com.repocket.androidsdk;

import android.os.StrictMode;
import android.util.Log;

import com.google.gson.Gson;
import com.repocket.androidsdk.services.Services;
import com.repocket.androidsdk.shared.DockerUtils;
import com.repocket.androidsdk.shared.MyPlayerPrefs;
import com.repocket.androidsdk.types.Types;

import org.json.JSONException;

import java.io.IOException;

import okhttp3.Response;

public class RepocketSDK {

    private static String _sdkApiKey = "";

    public static void Initialize(String sdkApiKey)
    {
        Log.d("RepocketSDK", "RepocketSDK -> Initialize");
        _sdkApiKey = sdkApiKey;
        MyPlayerPrefs.SetString("sdk-api-key", sdkApiKey);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        DockerUtils.Initialize();
//        try {
//            Response response = Services.PeerManagerApiService.Get("peer/config", null);
//            String responseData = response.body().string();
//            Gson gson = new Gson();
//            if (response.isSuccessful()) {
//                Types.PeerConfigResponse peerConfig = gson.fromJson(responseData, Types.PeerConfigResponse.class);
//                Log.d("RepocketSDK", "Received Peer Config: " + peerConfig.data.config_version_token);
//            }
//        } catch (JSONException e) {
//            throw new RuntimeException(e);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
    }

    public static void CreatePeer()
    {
    }

    public static void StopPeer()
    {
    }
}