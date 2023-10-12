package com.repocket.androidsdk;

import android.os.StrictMode;
import android.util.Log;

import com.repocket.androidsdk.shared.DockerUtils;
import com.repocket.androidsdk.shared.EventHandler;
import com.repocket.androidsdk.shared.MyPlayerPrefs;

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
    }

    public static void CreatePeer()
    {
    }

    public static void StopPeer()
    {
    }
}