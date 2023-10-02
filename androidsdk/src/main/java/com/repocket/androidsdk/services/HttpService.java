package com.repocket.androidsdk.services;

import com.android.volley.toolbox.HttpResponse;
import com.repocket.androidsdk.shared.Global;
import com.repocket.androidsdk.shared.MyPlayerPrefs;
import com.repocket.androidsdk.types.Types;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.json.JSONException;
import org.json.JSONObject;

public class HttpService {
    private String _accessToken;
    private String _peerToken;
    private Types.RuntimeInfo _runtimeInfo;
    private String _sdkApiKey;

    public HttpService() {
        _accessToken = MyPlayerPrefs.GetString("loginToken");
        _peerToken = MyPlayerPrefs.GetString("p-api-token");
        _sdkApiKey = MyPlayerPrefs.GetString("sdk-api-key");
        _runtimeInfo = Global.GetRuntimeInfo();
    }

    public CompletableFuture<HttpResponse> GetAsync(String url, Map<String, Object> parameters) {
        if (parameters != null && !parameters.isEmpty()) {
            url += "?" + parameters.entrySet().stream()
                    .map(p -> p.getKey() + "=" + p.getValue())
                    .reduce((p1, p2) -> p1 + "&" + p2)
                    .orElse("");
        }

        return SendHttpRequest(url, "GET", null);
    }

    public CompletableFuture<HttpResponse> PostAsync(String url, Object payload) {
        return SendHttpRequest(url, "POST", payload);
    }

    public CompletableFuture<HttpResponse> PutAsync(String url, Object payload) {
        return SendHttpRequest(url, "PUT", payload);
    }

    public CompletableFuture<HttpResponse> DeleteAsync(String url, Object data) {
        return SendHttpRequest(url, "DELETE", data);
    }

    private CompletableFuture<HttpResponse> SendHttpRequest(String url, String method, Object payload) {
        CompletableFuture<HttpResponse> responseFuture = new CompletableFuture<>();

        try {
            URL requestUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) requestUrl.openConnection();
            connection.setRequestMethod(method);
            connection.setRequestProperty("Accept", "application/json");

            _accessToken = MyPlayerPrefs.GetString("loginToken");
            _peerToken = MyPlayerPrefs.GetString("p-api-token");
            _sdkApiKey = MyPlayerPrefs.GetString("sdk-api-key");
            _runtimeInfo = Global.GetRuntimeInfo();

            if (!_accessToken.isEmpty() || !_peerToken.isEmpty() || !_sdkApiKey.isEmpty()) {
                if (!method.equals("OPTIONS")) {
                    if (!_accessToken.isEmpty()) {
                        connection.setRequestProperty("auth-token", _accessToken);
                    }
                    if (!_peerToken.isEmpty()) {
                        connection.setRequestProperty("p-auth-token", _peerToken);
                    }
                    if (!_sdkApiKey.isEmpty()) {
                        connection.setRequestProperty("api-key", _sdkApiKey);
                        connection.setRequestProperty("x-sdk-version", _runtimeInfo.AppVersion);
                    }
                }
            }

            connection.setRequestProperty("x-app-version", _runtimeInfo.AppVersion);

            if (_runtimeInfo.IsDocker) {
                connection.setRequestProperty("device-os", "node");
            } else if (_runtimeInfo.IsMac) {
                connection.setRequestProperty("device-os", "mac");
            } else if (_runtimeInfo.IsWindows) {
                connection.setRequestProperty("device-os", "windows");
            } else if (_runtimeInfo.IsLinux) {
                connection.setRequestProperty("device-os", "linux");
            }

            if (payload != null) {
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");

                String payloadJson = new JSONObject((Map) payload).toString();
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = payloadJson.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
            }

            int responseCode = connection.getResponseCode();

            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line);
                }
            }

            responseFuture.complete(content.toString());
            connection.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
            responseFuture.completeExceptionally(e);
        }

        return responseFuture;
    }

    private void HandleResponseAsync(JSONObject data) throws JSONException {
        if (data != null && data.has("message") && data.has("showNotification")) {
            String title = data.getJSONObject("message").optString("title", null);
            String body = data.getJSONObject("message").optString("body", null);
            String variant = data.getJSONObject("message").optString("variant", "success");

            // notificationService.openNotification({
            //   title: title,
            //   html: body,
            //   variant: variant || 'success',
            // });
        }
    }

    private void HandleErrorAsync(int statusCode, JSONObject data) {
        if (statusCode == 401) {
            if (data != null && data.has("message") && data.has("showNotification")) {
                // Handle unauthorized user
            }
        }

        if (data != null && data.has("err") && !data.optString("status", "").equals("402")) {
            if (data.has("showNotification")) {
                // Handle error
            }
        }

        // if (data != null) throw new Exception(data.toString());
    }
}
