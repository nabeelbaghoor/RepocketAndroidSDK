package com.repocket.androidsdk.services;

import com.repocket.androidsdk.shared.Global;

import org.json.JSONException;

import java.io.IOException;
import java.util.Map;

import okhttp3.Response;

public class ApiService {
    private final String _baseApi;
    private final HttpService _httpService;

    public ApiService(String path, Map<String, Object> options) {
        _baseApi = (options != null && options.containsKey("baseUrl"))
                ? options.get("baseUrl") + "/" + path
                : Global.GetConfig("production").ApiUrl + "/" + path;
        _httpService = new HttpService();
    }

    public Response Get(String endpoint, Map<String, Object> parameters) throws JSONException, IOException {
        return _httpService.GetAsync(_baseApi + endpoint, parameters);
    }

    public Response Post(String endpoint, Map<String, Object> payload) throws JSONException, IOException {
        return _httpService.PostAsync(_baseApi + endpoint, payload);
    }

    public Response Put(String endpoint, Map<String, Object> payload) throws JSONException, IOException {
        return _httpService.PutAsync(_baseApi + endpoint, payload);
    }

    public Response Delete(String endpoint, Map<String, Object> data) throws JSONException, IOException {
        return _httpService.DeleteAsync(_baseApi + endpoint, data);
    }
}

