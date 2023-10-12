package com.repocket.androidsdk.shared.eventHandling;

import android.util.Log;

public class MyListener implements MyEventListener {
    @Override
    public void handleEvent(String message) {
        Log.d("RepocketSDK",  "MyListener -> handleEvent: " + message);
    }
}
