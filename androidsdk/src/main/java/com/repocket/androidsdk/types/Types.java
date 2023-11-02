package com.repocket.androidsdk.types;

import androidx.annotation.Keep;

public abstract class Types {
    public String AppVersion;
    public boolean IsLinux;
    public boolean IsMac;
    public boolean IsWindows;

    public static class RuntimeInfo {
        public boolean IsDocker;
        public boolean IsMac;
        public boolean IsWindows;
        public boolean IsLinux;
        public boolean IsAndroid;
        public boolean IsIOS;
        public boolean IsWebGL;
        public String AppVersion;
        public String LibVersion;
        public String OsName;
    }

    public static class DeviceInfo {
        public int cpus;
        public String id;
        public String device;
        public boolean isPhysicalDevice;
        public String model;
        public String version;
        public boolean isDesktop;
        public int buildNumber;
        public String connectivityType;
        public String os;

        public DeviceInfo(int availableProcessors, String macAddress, String fingerprint, boolean b, String m, String v, boolean d, int i, String connectivityResult, String deviceName) {
            cpus = availableProcessors;
            id = macAddress;
            device = fingerprint;
            isPhysicalDevice = b;
            model = m;
            version = v;
            isDesktop = d;
            buildNumber = i;
            connectivityType = connectivityResult;
            os = deviceName;
        };
    }

    public static class Config {
        public String Env;
        public String ApiUrl;
        public String BaseUrl;
        public String PeerApiUrl;
        public String MonitorApiUrl;
        public String WebkitURL;
        public String DynamicLinkBaseUrl;
        public String PackageName;
    }

    public static class PeerTokenApiResponse {
        public String token;
        public String user_id;
        public String message;
    }

    public static class PeerMonitorApiResponse {
        public PeerMonitorApiDataField data;
    }

    public static class PeerMonitorApiDataField {
        public boolean active;
        public boolean isConfigurationUpdated;
    }

    public static class PeerConfigResponse {
        public PeerConfigDataField data;
    }

    public static class PeerConfigDataField {
        public String config_version_token;
    }

    public static class CreatePeerResponse {
        public CreatePeerDataField data;
    }

    public static class CreatePeerDataField {
        public String _id;
        public String token;
        public String userId;
        public TcpServerInfo tcpServerInfo;
    }

    public static class TcpServerInfo {
        public String ip;
        public int port;
        public int socketReqHandlerPort;
    }

    public class IpInfo{
        public String query;
    }
}

