package com.repocket.androidsdk.services;

import android.util.Log;

import com.repocket.androidsdk.shared.Global;
import com.repocket.androidsdk.shared.MyPlayerPrefs;
import com.repocket.androidsdk.types.Types;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HttpService {

    private final OkHttpClient httpClient;
    private String accessToken;
    private String peerToken;
    private Types.RuntimeInfo runtimeInfo;
    private String sdkApiKey;

    public HttpService() {
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    Request.Builder requestBuilder = original.newBuilder()
                            .header("Accept", "application/json");
                    addHeaders(requestBuilder);
                    Request request = requestBuilder.build();
                    return chain.proceed(request);
                })
                .build();

        accessToken = MyPlayerPrefs.GetString("loginToken");
        peerToken = MyPlayerPrefs.GetString("p-api-token");
        sdkApiKey = MyPlayerPrefs.GetString("sdk-api-key");

        runtimeInfo = Global.GetRuntimeInfo();
    }

    public Response GetAsync(String url, Map<String, Object> parameters) {
        if (parameters != null && !parameters.isEmpty()) {
            StringBuilder query = new StringBuilder();
            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                if (query.length() > 0) {
                    query.append("&");
                }
                query.append(entry.getKey()).append("=").append(entry.getValue());
            }
            url += "?" + query.toString();
        }
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        return handleResponse(request);
    }

    public Response PostAsync(String url, Object payload) {
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), new JSONObject((Map) payload).toString());
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();
        return handleResponse(request);
    }

    public Response PutAsync(String url, Object payload) {
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), new JSONObject((Map) payload).toString());
        Request request = new Request.Builder()
                .url(url)
                .put(requestBody)
                .build();
        return handleResponse(request);
    }

    public Response DeleteAsync(String url, Object data) {
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), new JSONObject((Map) data).toString());
        Request request = new Request.Builder()
                .url(url)
                .delete(requestBody)
                .build();
        return handleResponse(request);
    }

    private void addHeaders(Request.Builder requestBuilder) {
        accessToken = MyPlayerPrefs.GetString("loginToken");
        peerToken = MyPlayerPrefs.GetString("p-api-token");
        sdkApiKey = MyPlayerPrefs.GetString("sdk-api-key");

        runtimeInfo = Global.GetRuntimeInfo();

        if (!isEmpty(accessToken) || !isEmpty(peerToken) || !isEmpty(sdkApiKey)) {
            if (!isEmpty(accessToken)) requestBuilder.header("auth-token", accessToken);
            if (!isEmpty(peerToken)) requestBuilder.header("p-auth-token", peerToken);
            if (!isEmpty(sdkApiKey)) {
                requestBuilder.header("api-key", sdkApiKey);
                requestBuilder.header("x-sdk-version", runtimeInfo.AppVersion);
            }
        }

        requestBuilder.header("x-app-version", runtimeInfo.AppVersion);

        if (runtimeInfo.IsDocker) {
            requestBuilder.header("device-os", "node");
        } else if (runtimeInfo.IsMac) {
            requestBuilder.header("device-os", "mac");
        } else if (runtimeInfo.IsWindows) {
            requestBuilder.header("device-os", "windows");
        } else if (runtimeInfo.IsLinux) {
            requestBuilder.header("device-os", "linux");
        }
    }

    private Response handleResponse(Request request) {
        Response response = null;
        try {
            response = httpClient.newCall(request).execute();
        } catch (IOException e) {
            Log.d("RepocketSDK", "HttpService -> handleResponse -> IOException: " + e);
            throw new RuntimeException(e);
        }
//        String responseBody = response.body().string();
//
//        JSONObject data = null;
//        if (response.isSuccessful()) {
//            try {
//                data = new JSONObject(responseBody);
//            } catch (JSONException e) {
//                throw new RuntimeException(e);
//            }
//        }
//
//        handleResponseAsync(data);
//        handleErrorAsync(response.code(), data);

        return response;
    }

    private void handleResponseAsync(JSONObject data) {
        if (data != null && data.has("message") && data.has("showNotification")) {
            try {
                String title = data.getJSONObject("message").optString("title");
                String body = data.getJSONObject("message").optString("body");
                String variant = data.getJSONObject("message").optString("variant", "success");
            } catch (JSONException e) {
                Log.d("RepocketSDK", "HttpService -> handleResponseAsync -> JSONException: " + e);
            }

            // notificationService.openNotification({
            //   title: title,
            //   html: body,
            //   variant: variant || 'success',
            // });
        }
    }

    private void handleErrorAsync(int statusCode, JSONObject data) {
        if (statusCode == 401) {
            if (data != null && data.has("message") && data.has("showNotification")) {
                // Handle unauthorized user
            }
        }

        if (data != null && data.has("err") && !"402".equals(data.optString("status"))) {
            if (data.has("showNotification")) {
                // Handle error
            }
        }
    }

    private boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
}


//package com.repocket.androidsdk.services;
//
//import com.android.volley.toolbox.HttpResponse;
//import com.repocket.androidsdk.shared.Global;
//import com.repocket.androidsdk.shared.MyPlayerPrefs;
//import com.repocket.androidsdk.types.Types;
//
//import java.io.BufferedReader;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.io.OutputStream;
//import java.net.HttpURLConnection;
//import java.net.URL;
//import java.nio.charset.StandardCharsets;
//import java.util.ArrayList;
//import java.util.Map;
//import java.util.concurrent.CompletableFuture;
//
//import org.json.JSONException;
//import org.json.JSONObject;
//
//public class HttpService {
//    private String _accessToken;
//    private String _peerToken;
//    private Types.RuntimeInfo _runtimeInfo;
//    private String _sdkApiKey;
//
//    public HttpService() {
//        _accessToken = MyPlayerPrefs.GetString("loginToken");
//        _peerToken = MyPlayerPrefs.GetString("p-api-token");
//        _sdkApiKey = MyPlayerPrefs.GetString("sdk-api-key");
//        _runtimeInfo = Global.GetRuntimeInfo();
//    }
//
//    public CompletableFuture<HttpResponse> GetAsync(String url, Map<String, Object> parameters) {
//        if (parameters != null && !parameters.isEmpty()) {
//            url += "?" + parameters.entrySet().stream()
//                    .map(p -> p.getKey() + "=" + p.getValue())
//                    .reduce((p1, p2) -> p1 + "&" + p2)
//                    .orElse("");
//        }
//
//        return SendHttpRequest(url, "GET", null);
//    }
//
//    public CompletableFuture<HttpResponse> PostAsync(String url, Object payload) {
//        return SendHttpRequest(url, "POST", payload);
//    }
//
//    public CompletableFuture<HttpResponse> PutAsync(String url, Object payload) {
//        return SendHttpRequest(url, "PUT", payload);
//    }
//
//    public CompletableFuture<HttpResponse> DeleteAsync(String url, Object data) {
//        return SendHttpRequest(url, "DELETE", data);
//    }
//
//    private CompletableFuture<HttpResponse> SendHttpRequest(String url, String method, Object payload) {
//        CompletableFuture<HttpResponse> responseFuture = new CompletableFuture<>();
//
//        try {
//            URL requestUrl = new URL(url);
//            HttpURLConnection connection = (HttpURLConnection) requestUrl.openConnection();
//            connection.setRequestMethod(method);
//            connection.setRequestProperty("Accept", "application/json");
//
//            _accessToken = MyPlayerPrefs.GetString("loginToken");
//            _peerToken = MyPlayerPrefs.GetString("p-api-token");
//            _sdkApiKey = MyPlayerPrefs.GetString("sdk-api-key");
//            _runtimeInfo = Global.GetRuntimeInfo();
//
//            if (!_accessToken.isEmpty() || !_peerToken.isEmpty() || !_sdkApiKey.isEmpty()) {
//                if (!method.equals("OPTIONS")) {
//                    if (!_accessToken.isEmpty()) {
//                        connection.setRequestProperty("auth-token", _accessToken);
//                    }
//                    if (!_peerToken.isEmpty()) {
//                        connection.setRequestProperty("p-auth-token", _peerToken);
//                    }
//                    if (!_sdkApiKey.isEmpty()) {
//                        connection.setRequestProperty("api-key", _sdkApiKey);
//                        connection.setRequestProperty("x-sdk-version", _runtimeInfo.AppVersion);
//                    }
//                }
//            }
//
//            connection.setRequestProperty("x-app-version", _runtimeInfo.AppVersion);
//
//            if (_runtimeInfo.IsDocker) {
//                connection.setRequestProperty("device-os", "node");
//            } else if (_runtimeInfo.IsMac) {
//                connection.setRequestProperty("device-os", "mac");
//            } else if (_runtimeInfo.IsWindows) {
//                connection.setRequestProperty("device-os", "windows");
//            } else if (_runtimeInfo.IsLinux) {
//                connection.setRequestProperty("device-os", "linux");
//            }
//
//            if (payload != null) {
//                connection.setDoOutput(true);
//                connection.setRequestProperty("Content-Type", "application/json");
//
//                String payloadJson = new JSONObject((Map) payload).toString();
//                try (OutputStream os = connection.getOutputStream()) {
//                    byte[] input = payloadJson.getBytes(StandardCharsets.UTF_8);
//                    os.write(input, 0, input.length);
//                }
//            }
//
//            int responseCode = connection.getResponseCode();
//
//            StringBuilder content = new StringBuilder();
//            try (BufferedReader reader = new BufferedReader(
//                    new InputStreamReader(connection.getInputStream()))) {
//                String line;
//                while ((line = reader.readLine()) != null) {
//                    content.append(line);
//                }
//            }
//
//            responseFuture.complete(new HttpResponse(responseCode,  connection.getHeaderFields(), content.toString().getBytes()));
//            connection.disconnect();
//        } catch (IOException e) {
//            e.printStackTrace();
//            responseFuture.completeExceptionally(e);
//        }
//
//        return responseFuture;
//    }
//
//    private void HandleResponseAsync(JSONObject data) throws JSONException {
//        if (data != null && data.has("message") && data.has("showNotification")) {
//            String title = data.getJSONObject("message").optString("title", null);
//            String body = data.getJSONObject("message").optString("body", null);
//            String variant = data.getJSONObject("message").optString("variant", "success");
//
//            // notificationService.openNotification({
//            //   title: title,
//            //   html: body,
//            //   variant: variant || 'success',
//            // });
//        }
//    }
//
//    private void HandleErrorAsync(int statusCode, JSONObject data) {
//        if (statusCode == 401) {
//            if (data != null && data.has("message") && data.has("showNotification")) {
//                // Handle unauthorized user
//            }
//        }
//
//        if (data != null && data.has("err") && !data.optString("status", "").equals("402")) {
//            if (data.has("showNotification")) {
//                // Handle error
//            }
//        }
//
//        // if (data != null) throw new Exception(data.toString());
//    }
//}
