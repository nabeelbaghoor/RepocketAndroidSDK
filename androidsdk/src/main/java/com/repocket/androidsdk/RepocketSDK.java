package com.repocket.androidsdk;

import android.util.Log;

import com.repocket.androidsdk.shared.EventHandler;

public class RepocketSDK {

    private static String _sdkApiKey = "";

    public static void Initialize(String sdkApiKey)
    {
        Log.d("RepocketSDK", "RepocketSDK -> Initialize");
        // <Try>
        EventHandler eventHandler = new EventHandler();
        eventHandler.addListener(message -> Log.d("RepocketSDK" , "RepocketSDK1 -> Initialize -> listener added: " + message));
        eventHandler.addListener(message -> Log.d("RepocketSDK" , "RepocketSDK2 -> Initialize -> listener added: " + message));
        eventHandler.addListener(message -> Log.d("RepocketSDK" , "RepocketSDK3 -> Initialize -> listener added: " + message));
        eventHandler.broadcast("error");
        eventHandler.broadcast("error2");
        eventHandler.removeAllListeners();
        eventHandler.broadcast("error3");

        // </Try>














//        _sdkApiKey = sdkApiKey;
//        MyPlayerPrefs.SetString("sdk-api-key", sdkApiKey);
//        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
//        StrictMode.setThreadPolicy(policy);
//        DockerUtils.Initialize();




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