package com.repocket.androidsdk.P2P;

import android.util.Log;

import com.repocket.androidsdk.shared.EventHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class TargetSocket {
    private final byte[] buffer;
    private final byte[] receivedBuffer;
    private final Map<String, Object> request;
    private final Socket requestHandlerSocket;
    public final Socket socket;
    public EventHandler<Exception> targetWebsiteError = new EventHandler<>();

    public TargetSocket(Socket requestHandlerSocket, Map<String, Object> request, byte[] buffer) {
        this.requestHandlerSocket = requestHandlerSocket;
        this.request = request;
        this.receivedBuffer = buffer;
        this.buffer = new byte[8192];
        this.socket = new Socket();
    }

    public void connect() {
        if (request == null) return;

        int port = request.containsKey("port") ? Integer.parseInt(request.get("port").toString()) : 80;

        try {
            socket.connect(new InetSocketAddress(request.get("host").toString(), port));
            socket.setSoTimeout(30000);
            socket.setTcpNoDelay(true);

            onConnect();

            if (isHttps()) {
                String response = request.get("httpVersion") + " 200 Connection Established\r\n\r\n";
                byte[] responseBuffer = response.getBytes();
                requestHandlerSocket.getOutputStream().write(responseBuffer);
            }

            if (!isHttps()) {
                try {
                    onConnectedWait();
                    socket.getOutputStream().write(receivedBuffer);
                } catch (Exception e) {
                    Log.d("RepocketSDK","targetSocket.Send error: " + e);
                }
            }
        } catch (Exception e) {
            Log.d("RepocketSDK","Error connecting to target socket: " + e);
        }
    }

    private boolean isHttps() {
        return request.get("method").toString().toLowerCase().equals("connect");
    }

    private void onConnect() {
        try {
            onConnected();
            socket.getInputStream().read(buffer);
        } catch (IOException e) {
            Log.d("RepocketSDK","Error connecting to target socket: " + e);
        }
    }

    private void onConnected() {
        synchronized (this) {
            notify();
        }
    }

    private void onConnectedWait() {
        synchronized (this) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void close() {
        Log.d("RepocketSDK","TargetSocket -> onSocketCloseEvent");
        try {
            socket.close();
            // The requestHandlerSocket should already be closed, but we verify it here
            new Thread(() -> {
                try {
                    Thread.sleep(3000);
                    requestHandlerSocket.close();
                } catch (InterruptedException | IOException e) {
                    e.printStackTrace();
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void onReceive() {
        try {
            int bytesRead = socket.getInputStream().read(buffer);
            Log.d("RepocketSDK","TargetSocket -> bytesRead: " + bytesRead);
            if (bytesRead > 0) {
                byte[] receivedData = new byte[bytesRead];
                System.arraycopy(buffer, 0, receivedData, 0, bytesRead);
                requestHandlerSocket.getOutputStream().write(receivedData);
                onConnected();
            } else {
                close();
            }
        } catch (Exception e) {
            onError(e);
        }
    }

    private void onError(Exception error) {
        Log.d("RepocketSDK","Error receiving data from target socket: " + error);
        targetWebsiteError.broadcast(error);
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
