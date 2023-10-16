package com.repocket.androidsdk.shared;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo.State;

public class NetworkCheck {
    private Context context;
    private static NetworkCheck instance;
    private Activity activity;
    private ConnectivityManager connectivityManager;

    public NetworkCheck() {

        instance = this;
    }

    public static NetworkCheck instance() {
        if (instance == null) {
            instance = new NetworkCheck();
        }

        return instance;
    }

    public void setContext(Context context) {
        this.context = context;
        this.activity = (Activity)context;
    }

    public boolean CheckWifi() {
        if (this.connectivityManager == null) {
            this.connectivityManager = (ConnectivityManager)this.activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        }

        return this.connectivityManager.getNetworkInfo(1).getState() == State.CONNECTED;
    }

    public boolean CheckMobile() {
        if (this.connectivityManager == null) {
            this.connectivityManager = (ConnectivityManager)this.activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        }

        return this.connectivityManager.getNetworkInfo(0).getState() == State.CONNECTED;
    }

    public boolean CheckInternet() {
        return this.CheckMobile() || this.CheckWifi();
    }
}

