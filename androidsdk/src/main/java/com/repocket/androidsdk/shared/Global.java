package com.repocket.androidsdk.shared;

import com.repocket.androidsdk.types.Types;

public class Global {
    private static Types.RuntimeInfo _runtimeInfo;
    public static String SdkVersion = "android_sdk_1.1";
    public static Boolean IsSdk = true;
    public static String GrafanaLokiServerUrl = "http://51.159.149.175:3100"; // Address to local or remote Loki server


    private static final Types.Config Dev = new Types.Config() {{
        Env = "development";
        ApiUrl = "https://staging-server.repocket.co/api";
        BaseUrl = "localhost:3000";
        PeerApiUrl = "https://peer-staging.repocket.co/api";
        MonitorApiUrl = "https://monitor.repocket.co";
        WebkitURL = "https://staging-repocket.netlify.app";
        DynamicLinkBaseUrl = "https://link.repocket.co";
        PackageName = "com.app.repocket";
    }};

    private static final Types.Config Staging = new Types.Config() {{
        Env = "staging";
        ApiUrl = "https://staging-server.repocket.co/api";
        PeerApiUrl = "https://peer-staging.repocket.co/api";
        MonitorApiUrl = "https://monitor.repocket.co";
        BaseUrl = "https://staging-repocket-dashboard.netlify.app";
        WebkitURL = "https://staging-repocket.netlify.app";
        DynamicLinkBaseUrl = "https://staging-link.repocket.co";
        PackageName = "com.app.repocket";
    }};

    private static final Types.Config Prod = new Types.Config() {{
        Env = "production";
        ApiUrl = "https://api.repocket.co/api";
        PeerApiUrl = "https://peer.repocket.co/api";
        MonitorApiUrl = "https://monitor.repocket.co/api";
        BaseUrl = "https://app.repocket.co";
        WebkitURL = "https://repocket.co";
        DynamicLinkBaseUrl = "https://link.repocket.co";
        PackageName = "com.app.repocket";
    }};

    public static Types.RuntimeInfo GetRuntimeInfo() {
        if (_runtimeInfo == null) {
            _runtimeInfo = new Types.RuntimeInfo();
            _runtimeInfo.AppVersion = "1.0.0-default";
            _runtimeInfo.IsMac = isMac();
            _runtimeInfo.IsDocker = isDocker();
            _runtimeInfo.IsLinux = isLinux();
            _runtimeInfo.IsWindows = isWindows();
            _runtimeInfo.IsAndroid = isAndroid();
            _runtimeInfo.IsIOS = isIOS();
            _runtimeInfo.IsWebGL = isWebGL();
        }
        return _runtimeInfo;
    }

    public static Types.Config GetConfig(String env) {
        switch (env) {
            case "production":
                return Prod;
            case "development":
                return Dev;
            case "staging":
                return Staging;
            default:
                return Staging;
        }
    }

    private static boolean isMac() {
        return System.getProperty("os.name").toLowerCase().contains("mac");
    }

    private static boolean isDocker() {
        return System.getenv("RP_DOCKER") != null && !System.getenv("RP_DOCKER").isEmpty();
    }

    private static boolean isLinux() {
        return System.getProperty("os.name").toLowerCase().contains("linux");
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    private static boolean isAndroid() {
        return System.getProperty("java.vm.name").toLowerCase().contains("dalvik");
    }

    private static boolean isIOS() {
        // Add detection logic for iOS if needed
        return false;
    }

    private static boolean isWebGL() {
        // Add detection logic for WebGL if needed
        return false;
    }
}
