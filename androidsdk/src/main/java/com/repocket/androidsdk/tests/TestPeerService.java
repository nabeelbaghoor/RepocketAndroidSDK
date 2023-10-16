package com.repocket.androidsdk.tests;

import android.util.Log;

import com.repocket.androidsdk.services.PeerService;
import com.repocket.androidsdk.services.Services;
import com.repocket.androidsdk.shared.Utils;
import com.repocket.androidsdk.types.Types;

import java.util.HashMap;
import java.util.Map;

import okhttp3.Response;

public class TestPeerService {
    public static void initialize(String email, String password) {
        Utils.checkAndroidPermissions();

        try {
            nodeLogIn(email, password);
        } catch (Exception e) {
            Log.d("RepocketSDK","TestPeerService -> initialize -> Error: " + e.getMessage());
        }
    }

    private static void nodeLogIn(String email, String apiKey) {
        try {
            Response response = Services.PeerManagerApiService.Post("peer/token", createLoginData(email, apiKey));

            if (response.code() == 200) {
                String json = response.body().string();
                Types.PeerTokenApiResponse peerToken = Utils.fromJson(json, Types.PeerTokenApiResponse.class);

                String token = peerToken.token;
                String userId = peerToken.user_id;

                Log.d("RepocketSDK","TestPeerService -> nodeLogIn -> Peer API Token: " + token);
                Log.d("RepocketSDK","TestPeerService -> nodeLogIn -> User ID: " + userId);

                PeerService peerService = new PeerService(null,token, userId, apiKey);

                peerService.onConnected.addListener(x -> Log.d("RepocketSDK","TestPeerService -> nodeLogIn -> peer connected"));
                peerService.onConnecting.addListener(x -> Log.d("RepocketSDK","TestPeerService -> nodeLogIn -> peer resetting"));
                peerService.onDisconnected.addListener(x -> Log.d("RepocketSDK","TestPeerService -> nodeLogIn -> peer disconnected"));
                peerService.onRefreshTokenRequired.addListener(x -> Log.d("RepocketSDK","TestPeerService -> nodeLogIn -> peer refresh_token_required"));

                peerService.createPeer();
            } else {
                String responseMessage = response.body().string();
                throw createException(responseMessage);
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private static Map<String, Object> createLoginData(String email, String apiKey) {
        Map<String, Object> data = new HashMap<>();
        data.put("email", email);
        data.put("api_key", apiKey);
        return data;
    }

    private static Exception createException(String response) {
        switch (response) {
            case "auth/user-not-found":
                return new Exception("auth/user-not-found");
            case "auth/wrong-password":
                return new Exception("auth/wrong-password");
            default:
                return new Exception(response);
        }
    }
}
