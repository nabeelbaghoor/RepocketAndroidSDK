//package com.repocket.androidsdk.socks5;
//
//import com.repocket.androidsdk.shared.Debouncer;
//
//import java.io.IOException;
//import java.net.InetSocketAddress;
//import java.net.Socket;
//import java.net.SocketAddress;
//import java.nio.ByteBuffer;
//import java.nio.channels.SocketChannel;
//import java.nio.charset.StandardCharsets;
//import java.util.Random;
//import java.util.concurrent.atomic.AtomicInteger;
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
//    private final ByteBuffer buffer;
//    private final Debouncer resetConnectionDebouncer;
//    private boolean isReconnecting;
//    private boolean onConnectionEstablishedEventFired;
//    private boolean peerCloseWithError;
//
//    public interface ExceptionHandler {
//        void handle(Exception e);
//    }
//
//    public interface StringHandler {
//        void handle(String s);
//    }
//
//    public MainSocket(int port, String ip, String peerId, String token, String userId,
//                      int socketReqHandlerPort) {
//        this.port = port;
//        this.ip = ip;
//        this.peerId = peerId;
//        this.token = token;
//        this.userId = userId;
//        this.socketReqHandlerPort = socketReqHandlerPort != 0 ? socketReqHandlerPort : 7072;
//
//        this.buffer = ByteBuffer.allocate(1024);
//        this.resetConnectionDebouncer = new Debouncer(this::resetConnectionTimerElapsed, 500);
//    }
//
//    public boolean connect() {
//        try {
//            mainSocket = new RPSocket();
//            mainSocket.retryConnectionCounter = 0;
//            mainSocket.type = "main";
//            mainSocket.uid = new Random().nextInt();
//            mainSocket.isBusy = false;
//
//            mainSocket.connect(new InetSocketAddress(ip, port));
//            return true;
//        } catch (Exception ex) {
//            System.out.println("Main socket connect error: " + ex);
//            return false;
//        }
//    }
//
//    private void enableKeepAliveAndroid(Socket socket) {
//        try {
//            socket.setKeepAlive(true);
//            socket.setTcpKeepAlive(true);
//        } catch (Exception ex) {
//            System.out.println("Failed to enable keep-alive on Android: " + ex.getMessage());
//        }
//    }
//
//    private void enableKeepAlive(Socket socket) {
//        try {
//            socket.setKeepAlive(true);
//        } catch (Exception ex) {
//            System.out.println("Failed to enable keep-alive: " + ex.getMessage());
//        }
//    }
//
//    private void connectCallback() {
//        try {
//            mainSocket.beginReceive(buffer);
//        } catch (Exception ex) {
//            System.out.println("MainSocket -> error: " + ex);
//            peerCloseWithError = true;
//            handleSocketConnectionFailed(ex);
//        }
//    }
//
//    private void receiveCallback() {
//        if (peerCloseWithError) return;
//
//        try {
//            int bytesRead = mainSocket.endReceive(buffer);
//            if (bytesRead > 0) {
//                byte[] receivedBytes = new byte[bytesRead];
//                buffer.get(receivedBytes, 0, bytesRead);
//
//                try {
//                    handleRead(receivedBytes);
//                } catch (Exception e) {
//                    System.out.println("MainSocket -> HandleRead -> e: " + e);
//                    throw e;
//                }
//
//                mainSocket.beginReceive(buffer);
//            } else {
//                System.out.println("MainSocket -> close");
//                onClose();
//            }
//        } catch (Exception ex) {
//            System.out.println("Main socket receive error: " + ex);
//            handleSocketConnectionFailed(ex);
//        }
//    }
//
//    private void onClose() {
//        System.out.println("doneHandler");
//
//        if (!peerCloseWithError) {
//            System.out.println("main socket try to re connect " + peerId);
//            resetConnectionDebouncer.call();
//        } else {
//            System.out.println("main socket dont renew connection cause of error");
//            try {
//                mainSocket.close();
//            } catch (IOException e) {
//                System.out.println("Error closing main socket: " + e);
//            }
//            handleSocketConnectionClose();
//        }
//    }
//
//    private void handleRead(byte[] data) {
//        String request = new String(data, StandardCharsets.UTF_8);
//        String reqAsStr = request;
//        boolean isAuthPacket = reqAsStr.equals(PeerSocketEvents.Authentication);
//        boolean isPingPacket = reqAsStr.equals(PeerSocketEvents.Ping);
//        boolean isAuthFailedPacket = reqAsStr.equals(PeerSocketEvents.AuthenticationFailed);
//        boolean isConnCompletedPacket = reqAsStr.equals(PeerSocketEvents.ConnectionCompleted);
//
//        if (isAuthPacket) {
//            System.out.println("P2PS-MainSocket -> Authentication");
//            String authData = "authentication " + token + " " + userId + " " + peerId;
//            mainSocket.send(authData.getBytes(StandardCharsets.UTF_8));
//            return;
//        }
//
//        if (isConnCompletedPacket) {
//            if (onConnectionEstablishedEventFired) return;
//            handleConnectionEstablished(peerId);
//            onConnectionEstablishedEventFired = true;
//            return;
//        }
//
//        if (isPingPacket) {
//            System.out.println("MainSocket -> PING");
//            String pongData = PeerSocketEvents.Pong;
//            mainSocket.send(pongData.getBytes(StandardCharsets.UTF_8));
//            return;
//        }
//
//        if (isAuthFailedPacket) {
//            System.out.println("MainSocket -> Authentication Failed");
//            onMainSocketCloseWithError();
//            return;
//        }
//
//        String[] requests = reqAsStr.split("reqId:", -1);
//        System.out.println("req: " + requests[0]);
//
//        if (requests != null && requests.length > 0) {
//            for (String reqId : requests) {
//                if (reqId.isEmpty()) continue;
//                initRequestSocketHandler(reqId);
//            }
//        }
//    }
//
//    private void initRequestSocketHandler(String reqId) {
//        RequestHandlerSocket reqHandlerSocket = new RequestHandlerSocket(ip,
//                socketReqHandlerPort, reqId, peerId);
//        reqHandlerSocket.setSocketConnectionFailedHandler((e) -> {
//            String connectionFailedData = PeerSocketEvents.SocketHandlerConnectionFailed + ":" + reqId;
//            mainSocket.send(connectionFailedData.getBytes(StandardCharsets.UTF_8));
//        });
//
//        reqHandlerSocket.setTargetWebsiteErrorHandler((s) -> {
//            String websiteErrorData = PeerSocketEvents.TargetWebsiteError + ":" + reqId;
//            System.out.println("MainSocket -> websiteErrorData: " + websiteErrorData);
//            mainSocket.send(websiteErrorData.getBytes(StandardCharsets.UTF_8));
//        });
//
//        reqHandlerSocket.connect();
//    }
//
//    private void onMainSocketCloseWithError() {
//        peerCloseWithError = true;
//        try {
//            mainSocket.close();
//        } catch (IOException e) {
//            System.out.println("Error closing main socket: " + e);
//        }
//    }
//
//    private void resetConnectionTimerElapsed() {
//        if (isReconnecting) return;
//        if (mainSocket.retryConnectionCounter < MaxSocketRetries) {
//            mainSocket.setKeepAlive(true);
//            mainSocket.setTcpNoDelay(true);
//            mainSocket.retryConnectionCounter++;
//            System.out.println("MainSocket -> before reconnect: " + ip);
//
//            try {
//                isReconnecting = true;
//                mainSocket.connect(new InetSocketAddress(ip, port));
//                System.out.println("Main socket reconnected");
//                mainSocket.isBusy = false;
//                mainSocket.retryConnectionCounter--;
//                isReconnecting = false;
//                peerCloseWithError = false;
//            } catch (Exception ex) {
//                System.out.println("Main socket reconnect error: " + ex);
//                onMainSocketCloseWithError();
//            }
//        } else {
//            System.out.println("Main socket don't renew connection");
//            onMainSocketCloseWithError();
//            handleSocketConnectionClose();
//        }
//    }
//
//    public void end() {
//        System.out.println("Main socket destroy");
//        onMainSocketCloseWithError();
//    }
//
//    // These methods below need to be implemented with your specific handling logic.
//    private void handleSocketConnectionFailed(Exception e) {
//        // Implement your logic for handling socket connection failures here.
//    }
//
//    private void handleConnectionEstablished(String peerId) {
//        // Implement your logic for handling established connections here.
//    }
//
//    private void handleSocketConnectionClose() {
//        // Implement your logic for handling socket connection close events here.
//    }
//}
