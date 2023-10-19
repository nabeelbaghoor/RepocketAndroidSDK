package com.repocket.androidsdk.P2P;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.repocket.androidsdk.shared.Callback;
import com.repocket.androidsdk.shared.EventHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class TargetSocket {
    private final byte[] buffer;
    private final byte[] receivedBuffer;
    private final Map<String, Object> request;
    private final Socket requestHandlerSocket;
    public Socket socket;
    public EventHandler<Exception> targetWebsiteError = new EventHandler<>();

    public TargetSocket(Socket requestHandlerSocket, Map<String, Object> request, byte[] buffer) {
        this.requestHandlerSocket = requestHandlerSocket;
        this.request = request;
        this.receivedBuffer = buffer;
        this.buffer = new byte[8192];
    }

    public void connect() {
        if (request == null) return;

        int port = request.containsKey("port") ? Integer.parseInt(request.get("port").toString()) : 80;

        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(request.get("host").toString(), port));
            socket.setSoTimeout(30000);
            socket.setTcpNoDelay(true);

            Runnable runnable = () -> ReceiveData();
            new Thread(runnable).start();

            if (isHttps()) {
                String response = request.get("httpVersion") + " 200 Connection Established\r\n\r\n";
                byte[] responseBuffer = response.getBytes(StandardCharsets.US_ASCII);
                requestHandlerSocket.getOutputStream().write(responseBuffer);
            }

            if (!isHttps()) {
                try {
                    socket.getOutputStream().write(receivedBuffer);
                } catch (Exception e) {
                    Log.d("RepocketSDK", "TargetSocket -> connect -> targetSocket.write error: " + e);
                    throw new RuntimeException(e);
                }
            }
        } catch (Exception e) {
            Log.d("RepocketSDK","TargetSocket -> connect -> Error connecting to target socket: " + e);
            throw new RuntimeException(e);
        }
    }

    private boolean isHttps() {
        return request.get("method").toString().toLowerCase().equals("connect");
    }

//    private void onConnect() {
//        onConnected();
//        ReceiveData();
//    }
//
//    private void onConnected() {
//        synchronized (this) {
//            notify();
//        }
//    }
//
//    private void onConnectedWait() {
//        synchronized (this) {
//            try {
//                wait();
//            } catch (InterruptedException e) {
//                Log.d("RepocketSDK", "TargetSocket -> onConnectedWait -> InterruptedException: " + e);
//                throw new RuntimeException(e);
//            }
//        }
//    }

    public void close() {
        Log.d("RepocketSDK","TargetSocket -> close: onSocketCloseEvent");
        try {
            socket.close();
            // The requestHandlerSocket should already be closed, but we verify it here
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                try {
                    requestHandlerSocket.close();
                } catch (IOException e) {
                    Log.d("RepocketSDK", "TargetSocket -> close -> IOException: " + e);
                    throw new RuntimeException(e);
                }
            }, 3000);
        } catch (IOException e) {
            Log.d("RepocketSDK", "TargetSocket -> onConnectedWait -> IOException: " + e);
            throw new RuntimeException(e);
        }
    }

    private void ReceiveData() {
        if (!socket.isConnected()) return;

        try {
            int bytesRead = socket.getInputStream().read(buffer, 0,buffer.length);
            Log.d("RepocketSDK","TargetSocket -> ReceiveData: bytesRead: " + bytesRead);
            if (bytesRead > 0) {
                byte[] receivedData = new byte[bytesRead];
                System.arraycopy(buffer, 0, receivedData, 0, bytesRead);
                requestHandlerSocket.getOutputStream().write(receivedData);
                ReceiveData();
            } else {
                close();
            }
        } catch (Exception e) {
            onError(e);
        }
    }

    private void onError(Exception error) {
        Log.d("RepocketSDK","TargetSocket -> onError -> Error receiving data from target socket: " + error);
        targetWebsiteError.broadcast(error);
        try {
            socket.close();
        } catch (IOException e) {
            Log.d("RepocketSDK", "TargetSocket -> onError -> IOException: " + e);
            throw new RuntimeException(e);
        }
    }
}
