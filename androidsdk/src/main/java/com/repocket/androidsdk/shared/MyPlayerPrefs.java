package com.repocket.androidsdk.shared;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class MyPlayerPrefs {
    private static final ConcurrentMap<String, Object> Preferences = new ConcurrentHashMap<>();

    public static int GetInt(String key) {
        Object value = Preferences.get(key);
        return value != null ? (int) value : 0;
    }

    public static void SetInt(String key, int value) {
        Preferences.put(key, value);
    }

    public static void SetString(String key, String value) {
        Preferences.put(key, value);
    }

    public static String GetString(String key) {
        Object value = Preferences.get(key);
        return value != null ? value.toString() : "";
    }
}

