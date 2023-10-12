//package com.repocket.androidsdk.P2P;
//
//import android.util.Log;
//
//import com.repocket.androidsdk.shared.Debouncer;
//import com.repocket.androidsdk.shared.eventHandling.MyEventHandler;
//
//import java.io.IOException;
//import java.net.InetSocketAddress;
//import java.net.Socket;
//import java.util.Random;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//
//class RPSocket extends Socket {
//    public boolean isBusy;
//    public int retryConnectionCounter;
//    public String type;
//    public int uid;
//
//    public RPSocket() throws IOException {
//        super();
//        this.setTcpNoDelay(true);
//    }
//}
//
//public class MainSocket {
//    private final int port;
//    private final int socketReqHandlerPort;
//    private final String ip;
//    private final String peerId;
//    private final String token;
//    private final String userId;
//    private static final int MaxSocketRetries = 10; // try to reconnect for 1 minute
//    private RPSocket mainSocket;
//    private final byte[] buffer;
//    private final Debouncer resetConnectionDebouncer;
//    private boolean isReconnecting;
//    private boolean onConnectionEstablishedEventFired;
//    private boolean peerCloseWithError;
//    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
//
//    private MyEventHandler myEventHandler;
//
////    public interface EventHandler<T> {
////        void handle(T value);
////    }
//
//    public interface DebouncerCallback {
//        void onDebounced();
//    }
//
//    public MainSocket(int port, String ip, String peerId, String token, String userId, int socketReqHandlerPort) {
//        this.ip = ip;
//        this.port = port;
//        this.socketReqHandlerPort = socketReqHandlerPort;
//        this.peerId = peerId;
//        this.token = token;
//        this.userId = userId;
//
//        this.buffer = new byte[1024];
//        this.resetConnectionDebouncer = new Debouncer(this::resetConnectionTimerElapsed, 500);
//        myEventHandler = new MyEventHandler();
//    }
//
//    public void removeAllListeners() {
//        // Implement logic to remove event listeners.
//    }
//
//    public void connect(final MyEventHandler<Boolean> callback) {
//        executorService.submit(() -> {
//            try {
//                mainSocket = new RPSocket();
//                mainSocket.retryConnectionCounter = 0;
//                mainSocket.type = "main";
//                mainSocket.uid = new Random().nextInt();
//                mainSocket.isBusy = false;
//                setKeepAlive(mainSocket);
//                mainSocket.connect(new InetSocketAddress(ip, port));
//                callback.handle(true);
//            } catch (Exception ex) {
//                Log.e("MainSocket", "Connect error: " + ex.getMessage());
//                callback.handle(false);
//            }
//        });
//    }
//
//    private void resetConnectionTimerElapsed() {
//        executorService.submit(() -> {
//            if (isReconnecting) {
//                return;
//            }
//
//            if (mainSocket.retryConnectionCounter < MaxSocketRetries) {
//                setKeepAlive(mainSocket);
//                setTcpNoDelay(mainSocket);
//                mainSocket.retryConnectionCounter++;
//                Log.d("MainSocket", "Before reconnect: " + ip);
//
//                try {
//                    isReconnecting = true;
//                    mainSocket.connect(new InetSocketAddress(ip, port));
//                    Log.d("MainSocket", "Reconnected");
//                    mainSocket.isBusy = false;
//                    mainSocket.retryConnectionCounter--;
//                    isReconnecting = false;
//                    peerCloseWithError = false;
//                } catch (Exception ex) {
//                    Log.e("MainSocket", "Reconnect error: " + ex.getMessage());
//                    onMainSocketCloseWithError();
//                }
//            } else {
//                Log.d("MainSocket", "Don't renew connection");
//                onMainSocketCloseWithError();
//            }
//        });
//    }
//
//    public void end() {
//        Log.d("MainSocket", "Destroying main socket");
//        onMainSocketCloseWithError();
//        executorService.shutdown();
//    }
//
//    public void setKeepAlive(Socket socket) {
//        try {
//            socket.setKeepAlive(true);
//        } catch (Exception ex) {
//            Log.e("MainSocket", "Failed to enable keep-alive: " + ex.getMessage());
//        }
//    }
//
//    public void setTcpNoDelay(Socket socket) {
//        try {
//            socket.setKeepAlive(true);
//        } catch (Exception ex) {
//            Log.e("MainSocket", "Failed to set tcp no delay: " + ex.getMessage());
//        }
//    }
//}
