package com.repocket.androidsdk.services;

//import com.android.volley.toolbox.HttpResponse;
import com.android.volley.toolbox.HttpResponse;
import com.repocket.androidsdk.shared.Global;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ApiService {
    private final String _baseApi;
    private final HttpService _httpService;

    public ApiService(String path, Map<String, Object> options) {
        _baseApi = (options != null && options.containsKey("baseUrl"))
                ? options.get("baseUrl") + "/" + path
                : Global.GetConfig("production").ApiUrl + "/" + path;
        _httpService = new HttpService();
    }

    public CompletableFuture<HttpResponse> Get(String endpoint, Map<String, Object> parameters) {
        return _httpService.GetAsync(_baseApi + endpoint, parameters);
    }

    public CompletableFuture<HttpResponse> Post(String endpoint, Map<String, Object> payload) {
        return _httpService.PostAsync(_baseApi + endpoint, payload);
    }

    public CompletableFuture<HttpResponse> Put(String endpoint, Map<String, Object> payload) {
        return _httpService.PutAsync(_baseApi + endpoint, payload);
    }

    public CompletableFuture<HttpResponse> Delete(String endpoint, Map<String, Object> data) {
        return _httpService.DeleteAsync(_baseApi + endpoint, data);
    }
}

