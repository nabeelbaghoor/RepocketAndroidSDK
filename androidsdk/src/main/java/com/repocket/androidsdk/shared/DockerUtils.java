package com.repocket.androidsdk.shared;

import android.util.Log;

import com.repocket.androidsdk.services.Services;
import com.repocket.androidsdk.types.Types;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import okhttp3.Response;
import org.json.JSONException;

public class DockerUtils {
    public static void Initialize() {
        final String email = "adirsimona@gmail.com";
        final String apiKey = "53ecbb9c-deb4-49e9-8819-621d6ecb8aa8";

        try {
            ICallResult result = NodeLogIn(email, apiKey);
            Log.d("RepocketSDK","Peer API Token: " + result.getPeerApiToken());
            Log.d("RepocketSDK","User ID: " + result.getUserId());
        } catch (Exception ex) {
            Log.d("RepocketSDK","Error: " + ex.getMessage());
        }
    }

    public static ICallResult NodeLogIn(String email, String apiKey) {
        Map<String, Object> data = new HashMap<>();
        data.put("email", email);
        data.put("api_key", apiKey);

        try {
            Response response = Services.PeerManagerApiService.Post("peer/token", data);
            if (response.code() == 200) {
                String json = response.body().string();
                Types.PeerTokenApiResponse peerToken = Utils.fromJson(json, Types.PeerTokenApiResponse.class);
                return new CallResult(peerToken.token, peerToken.user_id);
            } else {
                String result = response.body().string();
                switch (result) {
                    case "auth/user-not-found":
                        throw new Exception("auth/user-not-found");
                    case "auth/wrong-password":
                        throw new Exception("auth/wrong-password");
                    default:
                        throw new Exception(result);
                }
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (Exception ex) {
        throw new RuntimeException(ex.getMessage());
        }
    }
}

interface ICallResult {
    String getPeerApiToken();

    String getUserId();
}

class CallResult implements ICallResult {
    private String peerApiToken;
    private String userId;

    public CallResult(String peerApiToken, String userId) {
        this.peerApiToken = peerApiToken;
        this.userId = userId;
    }

    @Override
    public String getPeerApiToken() {
        return peerApiToken;
    }

    @Override
    public String getUserId() {
        return userId;
    }
}