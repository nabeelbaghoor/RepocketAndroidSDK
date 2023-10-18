package com.repocket.androidsdk.P2P;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import android.util.Log;

import com.repocket.androidsdk.P2P.socks5.Socket5Handler;
import com.repocket.androidsdk.services.PeerSocketEvents;
import com.repocket.androidsdk.shared.EventHandler;

public class RequestHandlerSocket {
    private final byte[] _buffer;
    private final String _ip;
    private final String _peerId;
    private final int _port;
    private final String _reqId;
    private boolean _isSocks5Req;
    private ReqHandlerSocket _socket;
    private Socket5Handler _socks5TargetSocket;
    private TargetSocket _targetSocket;

    public EventHandler<String> TargetWebsiteError = new EventHandler<>();
    public EventHandler<String> SocketConnectionFailed = new EventHandler<>();

    public RequestHandlerSocket(String ip, int port, String reqId, String peerId) {
        _ip = ip;
        _port = port;
        _reqId = reqId;
        _peerId = peerId;
        _isSocks5Req = false;
        _buffer = new byte[4096];
    }

    public void Connect() {
        try {
            _socket = new ReqHandlerSocket();
            _socket.RetryConnectionCounter = 0;
            _socket.Type = "main";
            _socket.Uid = new Random().nextInt();
            _socket.IsBusy = false;
            _socket.setTcpNoDelay(true);
            // _socket.setReceiveTimeout(5000); // Set receive timeout if needed
            _socket.connect(new InetSocketAddress(_ip, _port));
            Log.d("RepocketSDK", "RequestHandlerSocket -> Connect: new socket req - " + _reqId);
            ReceiveData();
        } catch (IOException ex) {
            Log.d("RepocketSDK", "RequestHandlerSocket -> Connect: error when connecting to socket-server: " + ex.getMessage());
            // Handle connection error
            SocketConnectionFailed.broadcast(ex.getMessage());
            CloseSockets();
        }
    }

//    private void ConnectCallback() {
//        try {
//            // Handle socket connection callback
//            _socket.connect(new InetSocketAddress(_ip, _port));
//            Log.d("RepocketSDK", "RequestHandlerSocket -> ConnectCallback: new socket req - " + _reqId);
//            _socket.getInputStream().read(_buffer, 0, _buffer.length);
////            _socket.BeginReceive(_buffer, 0, _buffer.length, ReceiveData, null);
//        } catch (IOException ex) {
//            // Handle connection error
//            Log.d("RepocketSDK", "RequestHandlerSocket -> ConnectCallback: error when connecting to socket-server: " + ex.getMessage());
//            SocketConnectionFailed.broadcast(ex.getMessage());
//            CloseSockets();
//        }
//    }

    private void ReceiveData() {
        try {
            // Handle data received on the socket
            int bytesRead = _socket.getInputStream().read(_buffer, 0, _buffer.length);
            if (bytesRead > 0) {
                byte[] receivedData = new byte[bytesRead];
                System.arraycopy(_buffer, 0, receivedData, 0, bytesRead);

                // Handle received data
                HandleRead(receivedData);

                ReceiveData();
            } else {
                // Handle socket closure
                Log.d("RepocketSDK", "RequestHandlerSocket -> ReceiveData -> closed - " + _reqId);
                CloseSockets();
            }
        } catch (IOException ex) {
            // Handle receive error
            Log.d("RepocketSDK", "RequestHandlerSocket -> ReceiveData -> error: " + ex.getMessage());
            CloseSockets();
        }
    }

    private void HandleRead(byte[] data) {
        String request = new String(data, StandardCharsets.US_ASCII);
        final String authPacket = PeerSocketEvents.Authentication;
        final String remoteSocketClosePacket = PeerSocketEvents.RemoteSocketClosed;
        final String httpFirstLineRegex = "^(GET|HEAD|POST|PUT|DELETE|OPTIONS|TRACE|PATCH|CONNECT) (\\S+\\s+HTTP/1\\.(0|1)(\\r\\n([A-Za-z0-9-_]+:\\s+[\\S ]+)?)+\\r\\n\\r\\n.*)*$";

        Log.d("RepocketSDK", "RequestHandlerSocket -> request: " + request);
        if (authPacket.equals(request)) {
            String authenticationResponse = "authentication " + _peerId + " " + _reqId;
            byte[] responseBytes = authenticationResponse.getBytes(StandardCharsets.US_ASCII);
            try {
                _socket.getOutputStream().write(responseBytes);
            } catch (IOException e) {
                Log.d("RepocketSDK", "RequestHandlerSocket -> authPacket block -> IOException: " + e);
                throw new RuntimeException(e);
            }
            return;
        }

        if (remoteSocketClosePacket.equals(request)) {
            if (_targetSocket != null) {
                try {
                    _targetSocket.socket.close();
                } catch (IOException e) {
                    Log.d("RepocketSDK", "RequestHandlerSocket -> remoteSocketClosePacket block -> IOException: " + e);
                    throw new RuntimeException(e);
                }
            }
        } else if (_targetSocket != null) {
            try {
                SocketHelper.writeToSocket(_targetSocket.socket, data);
            } catch (IOException ex) {
                Log.d("RepocketSDK", "RequestHandlerSocket -> HandleRead -> TargetSocket send error: " + ex.getMessage());
            }
            return;
        } else if (_isSocks5Req) {
            // Handle SOCKS5 events
            return;
        }

        if (request.startsWith("CONNECT") || request.contains("HTTP/1.1") || Pattern.matches(httpFirstLineRegex, request)) {
            // http/https
            _socket.IsBusy = true;
            HttpProtocolHandler(request, data);
        } else if (IsSocks5Request(data)) {
            // socks5
            _isSocks5Req = true;
            _socks5TargetSocket = new Socket5Handler(_socket, _port, _ip, "8.8.8.8"); // TODO: hardcoded DNS
            try {
                _socks5TargetSocket.handle(data);
            } catch (UnknownHostException e) {
                Log.d("RepocketSDK", "RequestHandlerSocket -> IsSocks5Request() block -> UnknownHostException: " + e);
                throw new RuntimeException(e);
            }
        }
    }

    private void CloseSockets() {
        try {
            if (_socket != null) {
                _socket.close();
            }
            if (_targetSocket != null && _targetSocket.socket != null) {
                _targetSocket.socket.close();
            }
        } catch (IOException ex) {
            // Handle close error
            Log.d("RepocketSDK", "RequestHandlerSocket -> CloseSockets -> Socket close error: " + ex.getMessage());
        }
    }

    private boolean IsSocks5Request(byte[] buffer) {
        if (buffer == null) {
            return false;
        }

        // Check if buffer length is at least 10 bytes
        // Check SOCKS version
        // Check command code
        // Check reserved byte
        if (buffer.length < 10 || buffer[0] != 5 || buffer[1] != 1 || buffer[2] != 0) {
            return false;
        }

        return true;
    }

    private void HttpProtocolHandler(String data, byte[] buffer) {
        Map<String, String> httpRequest = ParseHttpRequest(data);

        if (httpRequest == null) {
            return;
        }

        int port = httpRequest.containsKey("port") && !httpRequest.get("port").isEmpty()
                ? Integer.parseInt(httpRequest.get("port"))
                : 80;
        try {
            Map<String, Object> httpRequestConverted = httpRequest
                    .entrySet()
                    .stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> (Object) entry.getValue()));

            _targetSocket = new TargetSocket(_socket, httpRequestConverted, buffer);

            _targetSocket.targetWebsiteError.addListener(e -> TargetWebsiteError.broadcast(e.toString()));

            _targetSocket.connect();
        } catch (Exception ex) {
            Log.d("RepocketSDK", "RequestHandlerSocket -> HttpProtocolHandler -> TargetSocket creation error: " + ex.getMessage());
        }
    }



    private Map<String, String> ParseHttpRequest(String data) {
        try {
            Map<String, String> httpRequest = new HashMap<>();
            if (data.contains("HTTP/1.0")) {
                String[] splitted = data.split(" |\r|\n");
                httpRequest.put("method", splitted[0]);
                httpRequest.put("path", splitted[1].split(":")[0]);
                httpRequest.put("httpVersion", splitted[2].split("\r")[0]);
                httpRequest.put("host", splitted[1].split(":")[0]);
                httpRequest.put("port", splitted[1].split(":")[1]);
            } else {
                String[] splitted = data.split("\r\n");
                String[] firstLine = splitted[0].trim().split(" ");
                httpRequest.put("method", firstLine[0]);
                httpRequest.put("path", firstLine[1]);
                httpRequest.put("httpVersion", firstLine[2]);
                int index = -1;
                for (int i = 0; i < splitted.length; i++) {
                    if (splitted[i].toLowerCase().startsWith("host: ")) {
                        index = i;
                        break;
                    }
                }
                if (index != -1) {
                    String hostLine = splitted[index];
                    String host = hostLine.split(":")[1].trim();
                    httpRequest.put("host", host);
                    String[] hostParts = hostLine.split(":");
                    if (hostParts.length > 2) {
                        httpRequest.put("port", Integer.toString(Integer.parseInt(hostParts[2].trim())));
                    } else {
                        httpRequest.put("port", "80");
                    }
                }
            }
            return httpRequest;
        } catch (Exception ex) {
            // Handle parse error
            Log.d("RepocketSDK", "RequestHandlerSocket -> ParseHttpRequest -> error: " + ex.getMessage());
            return null;
        }
    }
}