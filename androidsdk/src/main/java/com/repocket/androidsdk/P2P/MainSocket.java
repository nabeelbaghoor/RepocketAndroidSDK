package com.repocket.androidsdk.P2P;

import android.util.Log;

import com.repocket.androidsdk.services.PeerSocketEvents;
import com.repocket.androidsdk.shared.Debouncer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;

public class RPSocket extends Socket {
    public boolean IsBusy;
    public int RetryConnectionCounter;
    public String Type;
    public int Uid;

    public RPSocket() throws IOException {
        super();
        this.setTcpNoDelay(true);
        this.connect(new InetSocketAddress("your_ip_here", your_port_here));
    }
}

public class MainSocket {
    private final int _port;
    private final int _socketReqHandlerPort;
    private final String _ip;
    private final String _peerId;
    private final String _token;
    private final String _userId;
    private static final int MaxSocketRetries = 10; // try to reconnect for 1 minute
    private RPSocket _mainSocket;
    private final byte[] _buffer;
    private final Debouncer _resetConnectionDebouncer;
    private boolean _isReconnecting;
    private boolean _onConnectionEstablishedEventFired;
    private boolean _peerCloseWithError;

    public MainSocket(int port, String ip, String peerId, String token, String userId, int socketReqHandlerPort) {
        _ip = ip;
        _port = port;
        _socketReqHandlerPort = socketReqHandlerPort;
        _peerId = peerId;
        _token = token;
        _userId = userId;

        _buffer = new byte[1024];
        _resetConnectionDebouncer = new Debouncer(this::ResetConnectionTimer_Elapsed, 500);
    }

    public boolean Connect() {
        try {
            _mainSocket = new RPSocket();
            _mainSocket.RetryConnectionCounter = 0;
            _mainSocket.Type = "main";
            _mainSocket.Uid = new Random().nextInt();
            _mainSocket.IsBusy = false;
            _mainSocket.setTcpNoDelay(true);
            EnableKeepAlive(_mainSocket);
            _mainSocket.connect(new InetSocketAddress(_ip, _port));
            return true;
        } catch (IOException ex) {
            Log.d("Main socket connect error", ex.getMessage());
            return false;
        }
    }

    private void EnableKeepAlive(Socket socket) {
        try {
            socket.setKeepAlive(true);
        } catch (SocketException ex) {
            Log.e("Failed to enable keep-alive", ex.getMessage());
        }
    }

    private void ConnectCallback() {
        try {
            _mainSocket.connect();
            _mainSocket.getInputStream().read(_buffer, 0, _buffer.length);
            // Handle the read data
        } catch (IOException ex) {
            Log.d("MainSocket -> error", ex.getMessage());
            _peerCloseWithError = true;
            // Handle socket connection failed event
        }
    }

    private void ReceiveCallback() {
        if (_peerCloseWithError) return;

        try {
            int bytesRead = _mainSocket.getInputStream().read(_buffer, 0, _buffer.length);
            if (bytesRead > 0) {
                byte[] receivedBytes = new byte[bytesRead];
                System.arraycopy(_buffer, 0, receivedBytes, 0, bytesRead);

                try {
                    HandleRead(receivedBytes);
                } catch (Exception e) {
                    Log.d("MainSocket -> HandleRead -> e:", e.getMessage());
                    throw e;
                }

                _mainSocket.getInputStream().read(_buffer, 0, _buffer.length);
            } else {
                Log.d("MainSocket -> close", ex.getMessage());
                OnClose();
            }
        } catch (IOException ex) {
            Log.d("Main socket receive error", ex.getMessage());
            // Handle socket connection failed event
        }
    }

    private void OnClose() {
        Log.d("doneHandler");

        if (!_peerCloseWithError) {
            Log.d("main socket try to re connect " + _peerId);
            _resetConnectionDebouncer.Call();
        } else {
            Log.d("main socket dont renew connection cause of error");
            try {
                _mainSocket.close();
            } catch (IOException ex) {
                Log.e("Main socket close error", ex.getMessage());
            }
            // Handle socket connection close event
        }
    }

    private void HandleRead(byte[] data) {
        String request = new String(data, StandardCharsets.UTF_8);
        String reqAsStr = request;
        boolean isAuthPacket = reqAsStr.equals(PeerSocketEvents.Authentication);
        boolean isPingPacket = reqAsStr.equals(PeerSocketEvents.Ping);
        boolean isAuthFailedPacket = reqAsStr.equals(PeerSocketEvents.AuthenticationFailed);
        boolean isConnCompletedPacket = reqAsStr.equals(PeerSocketEvents.ConnectionCompleted);

        if (isAuthPacket) {
            Log.d("P2PS-MainSocket -> Authentication");
            byte[] authData = ("authentication " + _token + " " + _userId + " " + _peerId).getBytes(StandardCharsets.UTF_8);
            // Send authData via _mainSocket's output stream
            return;
        }

        if (isConnCompletedPacket) {
            if (_onConnectionEstablishedEventFired) return;
            // Fire ConnectionEstablished event
            _onConnectionEstablishedEventFired = true;
            return;
        }

        if (isPingPacket) {
            Log.d("MainSocket -> PING");
            byte[] pongData = PeerSocketEvents.Pong.getBytes(StandardCharsets.UTF_8);
            // Send pongData via _mainSocket's output stream
            return;
        }

        if (isAuthFailedPacket) {
            Log.d("MainSocket -> Authentication Failed");
            OnMainSocketCloseWithError();
            return;
        }

        String[] requests = reqAsStr.split("reqId:");
        Log.d("req: " + requests[0]);

        if (requests != null && requests.length > 0) {
            for (String reqId : requests) {
                if (reqId.isEmpty()) continue;
                InitRequestSocketHandler(reqId);
            }
        }
    }

    private void InitRequestSocketHandler(String reqId) {
        RequestHandlerSocket reqHandlerSocket = new RequestHandlerSocket(_ip,
                _socketReqHandlerPort != 0 ? _socketReqHandlerPort : 7072,
                reqId, _peerId);
        reqHandlerSocket.SocketConnectionFailed += (sender, s) -> {
            byte[] connectionFailedData = (PeerSocketEvents.SocketHandlerConnectionFailed + ":" + reqId).getBytes(StandardCharsets.UTF_8);
            // Send connectionFailedData via _mainSocket's output stream
        };

        reqHandlerSocket.TargetWebsiteError += (sender, s) -> {
            byte[] websiteErrorData = (PeerSocketEvents.TargetWebsiteError + ":" + reqId).getBytes(StandardCharsets.UTF_8);
            Log.d("MainSocket -> websiteErrorData:", Arrays.toString(websiteErrorData));
            // Send websiteErrorData via _mainSocket's output stream
        };

        reqHandlerSocket.Connect();
    }

    private void OnMainSocketCloseWithError() {
        _peerCloseWithError = true;
        try {
            _mainSocket.close();
        } catch (IOException ex) {
            Log.e("Main socket close error", ex.getMessage());
        }
    }

    private void ResetConnectionTimer_Elapsed() {
        if (_isReconnecting) return;
        if (_mainSocket.RetryConnectionCounter < MaxSocketRetries) {
            try {
                _isReconnecting = true;
                _mainSocket.connect(new InetSocketAddress(_ip, _port));
                Log.d("Main socket reconnected");
                _mainSocket.IsBusy = false;
                _mainSocket.RetryConnectionCounter--;
                _isReconnecting = false;
                _peerCloseWithError = false;
            } catch (IOException ex) {
                Log.d("Main socket reconnect error", ex.getMessage());
                OnMainSocketCloseWithError();
            }
        } else {
            Log.d("Main socket don't renew connection");
            OnMainSocketCloseWithError();
            // Handle socket connection close event
        }
    }

    public void End() {
        Log.d("Main socket destroy");
        OnMainSocketCloseWithError();
    }

    public void setKeepAlive(Socket socket) {
        try {
            socket.setKeepAlive(true);
        } catch (Exception ex) {
            Log.e("MainSocket", "Failed to enable keep-alive: " + ex.getMessage());
        }
    }

    public void setTcpNoDelay(Socket socket) {
        try {
            socket.setKeepAlive(true);
        } catch (Exception ex) {
            Log.e("MainSocket", "Failed to set tcp no delay: " + ex.getMessage());
        }
    }
}


//
//import android.util.Log;
//
//import com.repocket.androidsdk.shared.Debouncer;
//import com.repocket.androidsdk.shared.EventHandler;
//
//import java.io.IOException;
//import java.net.InetSocketAddress;
//import java.net.Socket;
//import java.util.Random;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.function.Consumer;
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
//    private EventHandler eventHandler;
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
//        eventHandler = new EventHandler();
//    }
//
//    public void removeAllListeners() {
//        eventHandler.removeAllListeners();
//    }
//
//    public void connect(final Consumer<Boolean> callback) {
//        executorService.submit(() -> {
//            try {
//                mainSocket = new RPSocket();
//                mainSocket.retryConnectionCounter = 0;
//                mainSocket.type = "main";
//                mainSocket.uid = new Random().nextInt();
//                mainSocket.isBusy = false;
//                setKeepAlive(mainSocket);
//                mainSocket.connect(new InetSocketAddress(ip, port));
//                callback.accept(true);
//            } catch (Exception ex) {
//                Log.e("MainSocket", "Connect error: " + ex.getMessage());
//                callback.accept(false);
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
