package com.repocket.androidsdk.shared;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

public class NetworkCheck {
    private Context context;
    private static NetworkCheck instance;
    private Activity activity;
    private ConnectivityManager connectivityManager;

    private NetworkCheck() {
    }

    public static NetworkCheck instance() {
        if (instance == null) {
            instance = new NetworkCheck();
        }
        return instance;
    }

    public void setContext(Context context) {
        this.context = context;
        this.activity = (Activity) context;
    }

    public boolean CheckWifi() {
        if (connectivityManager == null) {
            connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        }
        Network network = connectivityManager.getActiveNetwork();
        if (network != null) {
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        }
        return false;
    }

    public boolean CheckMobile() {
        if (connectivityManager == null) {
            connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        }
        Network network = connectivityManager.getActiveNetwork();
        if (network != null) {
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
        }
        return false;
    }

    public boolean CheckInternet() {
        return CheckMobile() || CheckWifi();
    }
}

//
//import android.app.Activity;
//import android.content.Context;
//import android.net.ConnectivityManager;
//import android.net.NetworkInfo.State;
//
//public class NetworkCheck {
//    private Context context;
//    private static NetworkCheck instance;
//    private Activity activity;
//    private ConnectivityManager connectivityManager;
//
//    public NetworkCheck() {
//
//        instance = this;
//    }
//
//    public static NetworkCheck instance() {
//        if (instance == null) {
//            instance = new NetworkCheck();
//        }
//
//        return instance;
//    }
//
//    public void setContext(Context context) {
//        this.context = context;
//        this.activity = (Activity)context;
//    }
//
//    public boolean CheckWifi() {
//        if (this.connectivityManager == null) {
//            this.connectivityManager = (ConnectivityManager)this.activity.getSystemService(Context.CONNECTIVITY_SERVICE);
//        }
//
//        return this.connectivityManager.getNetworkInfo(1).getState() == State.CONNECTED;
//    }
//
//    public boolean CheckMobile() {
//        if (this.connectivityManager == null) {
//            this.connectivityManager = (ConnectivityManager)this.activity.getSystemService(Context.CONNECTIVITY_SERVICE);
//        }
//
//        return this.connectivityManager.getNetworkInfo(0).getState() == State.CONNECTED;
//    }
//
//    public boolean CheckInternet() {
//        return this.CheckMobile() || this.CheckWifi();
//    }
//}
//
