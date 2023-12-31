package com.repocket.androidsdk.shared;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.gson.Gson;
import com.repocket.androidsdk.types.Types;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Utils {
    private static Gson gson = new Gson();

    public static void checkAndroidPermissions() {
        if(NetworkCheck.instance().checkAndRequestInternetPermission()){
            Log.d("RepocketSDK", "Utils -> checkAndroidPermissions -> Has internet permission");
        }
        else {
            Log.d("RepocketSDK", "Utils -> checkAndroidPermissions -> No Internet permission");
        }
    }

    private static String cyrb53(String str, int seed) {
        long h1 = 0xdeadbeef ^ seed;
        long h2 = 0x41c6ce57 ^ seed;

        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            h1 = (h1 ^ ch) * 2654435761L;
            h2 = (h2 ^ ch) * 1597334677L;
        }

        h1 = ((h1 ^ (h1 >> 16)) * 2246822507L) ^ ((h2 ^ (h2 >> 13)) * 3266489909L);
        h2 = ((h2 ^ (h2 >> 16)) * 2246822507L) ^ ((h1 ^ (h1 >> 13)) * 3266489909L);

        return String.format("%08x%08x", h2, h1);
    }

    public static Types.DeviceInfo getDeviceInfo() {
        try {
            Types.RuntimeInfo runtimeInfo = getRuntimeInfo();
            String deviceName = getDeviceName(runtimeInfo);
            String macAddress = getMacAddress();

            macAddress = cyrb53(macAddress,0);

            if (runtimeInfo.IsDocker) {
                macAddress = "docker-" + macAddress;
            }

            Log.d("RepocketSDK", "Utils -> getDeviceInfo -> Mac Address: " + macAddress);
            String version = runtimeInfo.AppVersion;
            String connectivityResult = checkConnectivityType();
            Log.d("RepocketSDK", "Utils -> getDeviceInfo -> Connectivity: " + connectivityResult);

            return new Types.DeviceInfo(
                    Runtime.getRuntime().availableProcessors(),
                    macAddress,
                    Build.FINGERPRINT,
                    true,
                    Build.MODEL,
                    version,
                    true,
                    0,
                    connectivityResult,
                    deviceName
            );
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private static String getDeviceName(Types.RuntimeInfo runtimeInfo) {
        if (runtimeInfo.IsDocker) {
            return "Docker Container";
        } else if (runtimeInfo.IsMac) {
            return "Mac Desktop";
        } else if (runtimeInfo.IsWindows) {
            return "Windows Desktop";
        } else if (runtimeInfo.IsLinux) {
            return "Linux Desktop";
        } else if (runtimeInfo.IsAndroid) {
            return "Android";
        } else if (runtimeInfo.IsIOS) {
            return "IOS";
        } else if (runtimeInfo.IsWebGL) {
            return "WebGL";
        }

        return "Desktop";
    }

    private static String checkConnectivityType() {
        NetworkCheck networking = NetworkCheck.instance();
        if (networking.CheckWifi())
            return "Wifi";
        else if (networking.CheckMobile())
            return "Mobile";
        else
            return "None";
    }

    private static String getMacAddress() {
        try {
            InetAddress localhost = InetAddress.getLocalHost();
            NetworkInterface networkInterface = NetworkInterface.getByInetAddress(localhost);

            byte[] macAddressBytes = networkInterface.getHardwareAddress();
            if (macAddressBytes != null) {
                StringBuilder macAddress = new StringBuilder();
                for (byte b : macAddressBytes) {
                    macAddress.append(String.format("%02X", b));
                }
                return macAddress.toString();
            } else {
                System.out.println("MAC Address not found for the specified network interface.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static Types.RuntimeInfo getRuntimeInfo() {
        String osName = "unknown-os";
        Types.RuntimeInfo runtimeInfo = Global.GetRuntimeInfo();

        if (runtimeInfo.IsDocker) {
            osName = "sdk-node";
        } else if (runtimeInfo.IsMac) {
            osName = "sdk-mac";
        } else if (runtimeInfo.IsWindows) {
            osName = "sdk-windows";
        } else if (runtimeInfo.IsLinux) {
            osName = "sdk-linux";
        } else {
            osName = "sdk";
        }

        runtimeInfo.OsName = osName;

        return runtimeInfo;
    }

    public static String getToken() {
        String accessToken = MyPlayerPrefs.GetString("loginToken");
        if (!accessToken.isEmpty()) {
            return accessToken;
        }

        String peerToken = MyPlayerPrefs.GetString("p-api-token");
        if (!peerToken.isEmpty()) {
            return peerToken;
        }

        String sdkApiKey = MyPlayerPrefs.GetString("sdk-api-key");
        if (!sdkApiKey.isEmpty()) {
            return sdkApiKey;
        }

        throw new RuntimeException("No valid token found.");
    }

    public static Map<String, Object> objectToDictionary(Object obj) {
        Map<String, Object> dictionary = new HashMap<>();

        Class<?> clazz = obj.getClass();
        java.lang.reflect.Field[] fields = clazz.getDeclaredFields();

        for (java.lang.reflect.Field field : fields) {
            field.setAccessible(true);
            try {
                Object fieldValue = field.get(obj);
                dictionary.put(field.getName(), fieldValue);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        return dictionary;
    }

    public static <T> T fromJson(String json, Class<T> classOfT) {
        return gson.fromJson(json, classOfT);
    }
}