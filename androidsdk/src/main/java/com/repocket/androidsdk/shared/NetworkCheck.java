package com.repocket.androidsdk.shared;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class NetworkCheck {
    private Context context;
    private static NetworkCheck instance;
    private Activity activity;
    private ConnectivityManager connectivityManager;

    private static final int INTERNET_PERMISSION_REQUEST_CODE = 1;

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

    public boolean checkAndRequestInternetPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int permissionStatus = ContextCompat.checkSelfPermission(activity, Manifest.permission.INTERNET);

            if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
                // Request the INTERNET permission
                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.INTERNET},
                        INTERNET_PERMISSION_REQUEST_CODE);
                return false;
            } else {
                // Permission has already been granted
                return true;
            }
        } else {
            // Versions prior to Marshmallow don't require runtime permission checks
            return true;
        }
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
