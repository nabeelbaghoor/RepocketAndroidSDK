package com.repocket.androidsdk.services;

import com.repocket.androidsdk.shared.Global;

import java.util.HashMap;
import java.util.Map;

public class Services {
    public static final ApiService MonitorApiService = new ApiService("", getConfigMap(Global.GetConfig("production").MonitorApiUrl));
    public static final ApiService PeerManagerApiService = new ApiService("", getConfigMap(Global.GetConfig("production").PeerApiUrl));

    private static Map<String, Object> getConfigMap(String baseUrlKey) {
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("baseUrl", baseUrlKey);
        return configMap;
    }
}
