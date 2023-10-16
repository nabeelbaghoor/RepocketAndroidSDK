package com.repocket.androidsdk.P2P;

import android.util.Log;

import com.repocket.androidsdk.services.PeerSocketEvents;
import com.repocket.androidsdk.shared.Debouncer;
import com.repocket.androidsdk.shared.EventHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Random;

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
    public EventHandler<Exception> SocketConnectionFailed = new EventHandler<>();
    public EventHandler<String> ConnectionEstablished = new EventHandler<>();
    public EventHandler SocketConnectionClose = new EventHandler<>();

    public MainSocket(int port, String ip, String peerId, String token, String userId, int socketReqHandlerPort) {
        _ip = ip;
        _port = port;
        _socketReqHandlerPort = socketReqHandlerPort;
        _peerId = peerId;
        _token = token;
        _userId = userId;

        _buffer = new byte[1024];
        _resetConnectionDebouncer = new Debouncer(o -> ResetConnectionTimer_Elapsed(), 500);
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
            ReceiveData();
            return true;
        } catch (IOException ex) {
            Log.d("RepocketSDK", "MainSocket -> Connect: Main socket connect error:" + ex.getMessage());
            SocketConnectionFailed.broadcast(ex);
            return false;
        }
    }

    private void EnableKeepAlive(Socket socket) {
        try {
            socket.setKeepAlive(true);
        } catch (IOException ex) {
            Log.d("RepocketSDK","MainSocket -> EnableKeepAlive: Failed to enable keep-alive: " + ex.getMessage());
        }
    }

//    private void AfterConnecting() {
//        try {
//            _mainSocket.getInputStream().read(_buffer, 0, _buffer.length);
//            ReceiveData();
//            // Handle the read data
//        } catch (IOException ex) {
//            Log.d("RepocketSDK","MainSocket -> ConnectCallback -> error: "+ ex.getMessage());
//            _peerCloseWithError = true;
//            SocketConnectionFailed.broadcast(ex);
//        }
//    }

    private void ReceiveData() {
        if (_peerCloseWithError) return;

        try {
            int bytesRead = _mainSocket.getInputStream().read(_buffer, 0, _buffer.length);
            if (bytesRead > 0) {
                byte[] receivedBytes = new byte[bytesRead];
                System.arraycopy(_buffer, 0, receivedBytes, 0, bytesRead);

                try {
                    HandleRead(receivedBytes);
                } catch (Exception e) {
                    Log.d("RepocketSDK","MainSocket -> ReceiveCallback -> e:"+ e.getMessage());
                    throw e;
                }

                ReceiveData();
            } else {
                Log.d("RepocketSDK:","MainSocket -> ReceiveCallback -> close");
                OnClose();
            }
        } catch (IOException ex) {
            Log.d("RepocketSDK","MainSocket -> ReceiveCallback: Main socket receive error: "+ex.getMessage());
            _peerCloseWithError = true;
            SocketConnectionFailed.broadcast(ex);
        }
    }

    private void OnClose() {
        Log.d("RepocketSDK", "MainSocket -> OnClose: doneHandler");

        if (!_peerCloseWithError) {
            Log.d("RepocketSDK","MainSocket -> OnClose: main socket try to re connect " + _peerId);
            _resetConnectionDebouncer.call("_resetConnectionDebouncer");
        } else {
            Log.d("RepocketSDK","MainSocket -> OnClose: main socket don't renew connection cause of error");
            try {
                _mainSocket.close();
            } catch (IOException ex) {
                Log.e("RepocketSDK","MainSocket -> OnClose: Main socket close error:" + ex.getMessage());
            }
            SocketConnectionClose.broadcast(null);
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
            Log.d("RepocketSDK","MainSocket -> HandleRead: Authentication");
            byte[] authData = ("authentication " + _token + " " + _userId + " " + _peerId).getBytes(StandardCharsets.UTF_8);
            try {
                _mainSocket.getOutputStream().write(authData);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return;
        }

        if (isConnCompletedPacket) {
            if (_onConnectionEstablishedEventFired) return;
            ConnectionEstablished.broadcast(_peerId);
            _onConnectionEstablishedEventFired = true;
            return;
        }

        if (isPingPacket) {
            Log.d("RepocketSDK","MainSocket -> HandleRead: PING");
            byte[] pongData = PeerSocketEvents.Pong.getBytes(StandardCharsets.UTF_8);
            try {
                _mainSocket.getOutputStream().write(pongData);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return;
        }

        if (isAuthFailedPacket) {
            Log.d("RepocketSDK","MainSocket -> HandleRead: Authentication Failed");
            OnMainSocketCloseWithError();
            return;
        }

        String[] requests = reqAsStr.split("reqId:");
        Log.d("RepocketSDK","MainSocket -> HandleRead -> req: " + requests[0]);

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
        reqHandlerSocket.SocketConnectionFailed.addListener(ex -> {
            byte[] connectionFailedData = (PeerSocketEvents.SocketHandlerConnectionFailed + ":" + reqId).getBytes(StandardCharsets.UTF_8);
            try {
                _mainSocket.getOutputStream().write(connectionFailedData);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        reqHandlerSocket.TargetWebsiteError.addListener((String s) -> {
            byte[] websiteErrorData = (PeerSocketEvents.TargetWebsiteError + ":" + reqId).getBytes(StandardCharsets.UTF_8);
            Log.d("RepocketSDK","MainSocket -> InitRequestSocketHandler: websiteErrorData:" + websiteErrorData.toString());
            try {
                _mainSocket.getOutputStream().write(websiteErrorData);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        reqHandlerSocket.Connect();
    }

    private void OnMainSocketCloseWithError() {
        _peerCloseWithError = true;
        try {
            _mainSocket.close();
        } catch (IOException ex) {
            Log.d("RepocketSDK","MainSocket -> OnMainSocketCloseWithError: Main socket close error: " + ex.getMessage());
        }
    }

    private void ResetConnectionTimer_Elapsed() {
        if (_isReconnecting) return;
        if (_mainSocket.RetryConnectionCounter < MaxSocketRetries) {
            try {
                _isReconnecting = true;
                _mainSocket.connect(new InetSocketAddress(_ip, _port));
                Log.d("RepocketSDK","MainSocket -> ResetConnectionTimer_Elapsed: Main socket reconnected");
                _mainSocket.IsBusy = false;
                _mainSocket.RetryConnectionCounter--;
                _isReconnecting = false;
                _peerCloseWithError = false;
            } catch (IOException ex) {
                Log.d("RepocketSDK","MainSocket -> ResetConnectionTimer_Elapsed: Main socket reconnect error: " + ex.getMessage());
                OnMainSocketCloseWithError();
            }
        } else {
            Log.d("RepocketSDK","MainSocket -> ResetConnectionTimer_Elapsed: Main socket don't renew connection");
            OnMainSocketCloseWithError();
            SocketConnectionClose.broadcast(null);
        }
    }

    public void End() {
        Log.d("RepocketSDK","MainSocket -> End: Main socket destroy");
        OnMainSocketCloseWithError();
    }
}
