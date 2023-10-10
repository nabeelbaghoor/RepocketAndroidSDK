package com.repocket.androidsdk.socks5;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class TargetSocket {
    private final byte[] buffer;
    private final CountDownLatch onConnected = new CountDownLatch(1);
    private final byte[] receivedBuffer;
    private final Dictionary<String, Object> request;
    private final Socket requestHandlerSocket;
    public Socket socket;

    public TargetSocket(Socket requestHandlerSocket, Dictionary<String, Object> request, byte[] buffer) {
        this.requestHandlerSocket = requestHandlerSocket;
        this.request = request;
        this.receivedBuffer = buffer;
        this.buffer = new byte[8192];
    }

    public interface TargetWebsiteErrorListener {
        void onTargetWebsiteError(TargetSocket targetSocket);
    }

    private TargetWebsiteErrorListener targetWebsiteErrorListener;

    public void setTargetWebsiteErrorListener(TargetWebsiteErrorListener listener) {
        targetWebsiteErrorListener = listener;
    }

    public void removeAllListeners() {
        targetWebsiteErrorListener = null;
    }

    public void connect() {
        if (request == null) return;

        int port = request.get("port") != null ? Integer.parseInt(request.get("port").toString()) : 80;

        try {
            socket = new Socket();
            socket.setSoTimeout(30000);
            socket.setTcpNoDelay(true);

            onConnected.await();
            socket.connect(new InetSocketAddress(request.get("host").toString(), port));

            if (isHttps()) {
                String response = request.get("httpVersion") + " 200 Connection Established\r\n\r\n";
                byte[] responseBuffer = response.getBytes();
                requestHandlerSocket.getOutputStream().write(responseBuffer);
            }

            if (!isHttps()) {
                socket.getOutputStream().write(receivedBuffer);
            }
        } catch (IOException | InterruptedException e) {
            System.out.println("Error connecting to target socket: " + e);
            return;
        }
    }

    private boolean isHttps() {
        return request.get("method").toString().toLowerCase().equals("connect");
    }

    private void onConnect() {
        onConnected.countDown();
        try {
            socket.getInputStream().read(buffer);
        } catch (IOException e) {
            System.out.println("Error connecting to target socket: " + e);
        }
    }

    private void onReceive() {
        if (!socket.isConnected()) return;

        try {
            int bytesRead = socket.getInputStream().read(buffer);

            System.out.println("TargetSocket -> bytesRead: " + bytesRead);

            if (bytesRead > 0) {
                byte[] receivedData = new byte[bytesRead];
                System.arraycopy(buffer, 0, receivedData, 0, bytesRead);

                requestHandlerSocket.getOutputStream().write(receivedData);

                onReceive();
            } else {
                close();
            }
        } catch (IOException e) {
            onError(e);
        }
    }

    private void close() {
        System.out.println("TargetSocket -> onSocketCloseEvent");
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            Thread.sleep(3000);
            requestHandlerSocket.close();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void onError(Exception error) {
        System.out.println("Error receiving data from target socket: " + error);
        if (targetWebsiteErrorListener != null) {
            targetWebsiteErrorListener.onTargetWebsiteError(this);
        }
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}