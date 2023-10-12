package com.repocket.androidsdk;

import android.os.StrictMode;
import android.util.Log;

import com.google.gson.Gson;
import com.repocket.androidsdk.services.Services;
import com.repocket.androidsdk.shared.DockerUtils;
import com.repocket.androidsdk.shared.MyPlayerPrefs;
import com.repocket.androidsdk.shared.eventHandling.Event;
import com.repocket.androidsdk.shared.eventHandling.MyEvent;
import com.repocket.androidsdk.shared.eventHandling.MyEventArgs;
import com.repocket.androidsdk.shared.eventHandling.MyEventHandler;
import com.repocket.androidsdk.shared.eventHandling.MyEventListener;
import com.repocket.androidsdk.shared.eventHandling.MyListener;
import com.repocket.androidsdk.types.Types;

import org.json.JSONException;

import java.io.IOException;
import java.util.EventListener;

import okhttp3.Response;

public class RepocketSDK {

    private static String _sdkApiKey = "";

    public static void Initialize(String sdkApiKey)
    {
        Log.d("RepocketSDK", "RepocketSDK -> Initialize");
        // <Try>
//        MyEvent myEvent = new MyEvent();
//        myEvent.addListener(message -> Log.d("RepocketSDK" , "RepocketSDK -> Initialize -> listener added"));
////        myEvent.removeAllListeners();
//        myEvent.invoke();

        Event event = new Event();
        event.addListener(message -> Log.d("RepocketSDK" , "RepocketSDK -> Initialize -> listener added"));
        event.addListener(message -> Log.d("RepocketSDK" , "RepocketSDK2 -> Initialize -> listener added"));
        java.util.function.Consumer<MyEventArgs> a = event.addListener(message -> Log.d("RepocketSDK" , "RepocketSDK3 -> Initialize -> listener added"));
        a.andThen(b->{
            Log.d("RepocketSDK", b.toString());
        });
//        event.broadcast(new MyEventArgs());
        event.broadcast(message -> {
            Log.d("RepocketSDK", "RepocketSDK -> chal gya");
        });
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