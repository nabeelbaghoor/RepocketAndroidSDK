//package com.repocket.androidsdk.socks5;
//
//import java.io.IOException;
//import java.net.Socket;
//import java.nio.charset.StandardCharsets;
//import java.util.Arrays;
//import java.util.Dictionary;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Random;
//
//public class RequestHandlerSocket {
//    private byte[] buffer;
//    private String ip;
//    private String peerId;
//    private int port;
//    private String reqId;
//    private boolean isSocks5Req;
//    private ReqHandlerSocket socket;
//    private Socket5Handler socks5TargetSocket;
//    private TargetSocket targetSocket;
//
//    public RequestHandlerSocket(String ip, int port, String reqId, String peerId) {
//        this.ip = ip;
//        this.port = port;
//        this.reqId = reqId;
//        this.peerId = peerId;
//        this.isSocks5Req = false;
//        this.buffer = new byte[4096];
//    }
//
//    public void removeAllListeners() {
//        // Remove all listeners
//    }
//
//    public void connect() throws IOException {
//        socket = new ReqHandlerSocket(ip, port);
//        socket.retryConnectionCounter = 0;
//        socket.type = "main";
//        socket.uid = new Random().nextInt();
//        socket.isBusy = false;
//        socket.setTcpNoDelay(true);
//
//        // socket.setSoTimeout(5000); // Set receive timeout if needed
//
//        socket.connect(null);
//        connectCallback();
//    }
//
//    private void connectCallback() throws IOException {
//        try {
//            socket.finishConnect();
//
//            System.out.println("new socket req - " + reqId);
//
//            socket.beginReceive(buffer, 0, buffer.length, 0, receiveCallback, null);
//        } catch (Exception ex) {
//            System.out.println("ConnectCallback: error when connecting to socket-server: " + ex.getMessage());
//            socket.close();
//            if (targetSocket != null) {
//                targetSocket.socket.close();
//            }
//        }
//    }
//
//    private final java.nio.channels.CompletionHandler<Integer, Void> receiveCallback = new java.nio.channels.CompletionHandler<Integer, Void>() {
//        @Override
//        public void completed(Integer bytesRead, Void attachment) {
//            if (bytesRead > 0) {
//                byte[] receivedData = Arrays.copyOf(buffer, bytesRead);
//
//                try {
//                    handleRead(receivedData);
//                } catch (Exception e) {
//                    System.out.println("e: " + e);
//                    throw new RuntimeException(e);
//                }
//
//                socket.beginReceive(buffer, 0, buffer.length, 0, receiveCallback, null);
//            } else {
//                System.out.println("RequestHandlerSocket -> closed - " + reqId);
//                socket.close();
//                if (targetSocket != null) {
//                    targetSocket.socket.close();
//                }
//            }
//        }
//
//        @Override
//        public void failed(Throwable exc, Void attachment) {
//            System.out.println("ReceiveCallback error: " + exc.getMessage());
//            try {
//                socket.close();
//                if (targetSocket != null) {
//                    targetSocket.socket.close();
//                }
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//        }
//    };
//
//    private void handleRead(byte[] data) throws Exception {
//        String request = new String(data, StandardCharsets.US_ASCII);
//
//        final String authPacket = PeerSocketEvents.Authentication;
//        final String remoteSocketClosePacket = PeerSocketEvents.RemoteSocketClosed;
//        final String httpFirstLineRegex = "^(GET|HEAD|POST|PUT|DELETE|OPTIONS|TRACE|PATCH|CONNECT) (\\S+\\s+HTTP\\/1\\.(0|1)(\\r\\n([A-Za-z0-9-_]+:\\s+[\\S ]+)?)+\\r\\n\\r\\n.*)*$";
//
//        System.out.println("RequestHandlerSocket -> request: " + request);
//        if (request.equals(authPacket)) {
//            String authenticationResponse = "authentication " + peerId + " " + reqId;
//            byte[] responseBytes = authenticationResponse.getBytes(StandardCharsets.US_ASCII);
//            socket.send(responseBytes);
//            return;
//        }
//
//        if (request.equals(remoteSocketClosePacket)) {
//            if (targetSocket != null) {
//                targetSocket.socket.close();
//            }
//        } else if (targetSocket != null) {
//            try {
//                targetSocket.socket.getOutputStream().write(data);
//            } catch (Exception ex) {
//                System.out.println("TargetSocket send error: " + ex.getMessage());
//            }
//            return;
//        } else if (isSocks5Req) {
//            // write events handled by pipe method on the socket for socks5
//            // Handle events for SOCKS5
//            return;
//        }
//
//        if (request.startsWith("CONNECT") || request.contains("HTTP/1.1") || request.matches(httpFirstLineRegex)) {
//            // http/https
//            socket.isBusy = true;
//
//            httpProtocolHandler(request, data);
//        } else if (isSocks5Request(data)) {
//            // socks5
//            isSocks5Req = true;
//            socks5TargetSocket = new Socket5Handler(socket, port, ip, "8.8.8.8"); // TODO: hardcoded dns
//            socks5TargetSocket.handle(data);
//        }
//        // Handle other cases
//    }
//
//    private void httpProtocolHandler(String data, byte[] buffer) {
//        Map<String, Object> httpRequest = parseHttpRequest(data);
//
//        if (httpRequest == null) {
//            return;
//        }
//
//        int port = (httpRequest.containsKey("port") && !httpRequest.get("port").toString().isEmpty()) ? Integer.parseInt(httpRequest.get("port").toString()) : 80;
//        try {
//            Map<String, Object> httpRequestConverted = new HashMap<>();
//            for (Map.Entry<String, Object> entry : httpRequest.entrySet()) {
//                httpRequestConverted.put(entry.getKey(), entry.getValue());
//            }
//
//            targetSocket = new TargetSocket(socket, (Dictionary<String, Object>) httpRequestConverted, buffer);
//
//            targetSocket.targetWebsiteError = (sender, e) -> {
//                // Handle target website error
//            };
//
//            targetSocket.connect();
//        } catch (Exception ex) {
//            System.out.println("TargetSocket creation error: " + ex.getMessage());
//        }
//    }
//
//    private boolean isSocks5Request(byte[] buffer) {
//        if (buffer == null) {
//            return false;
//        }
//
//        // Check if buffer length is at least 10 bytes
//        // Check SOCKS version
//        // Check command code
//        // Check reserved byte
//        if (buffer.length < 10 || buffer[0] != 5 || buffer[1] != 1 || buffer[2] != 0) {
//            return false;
//        }
//
//        return true;
//    }
//
//    private Map<String, Object> parseHttpRequest(String data) {
//        try {
//            Map<String, Object> httpRequest = new HashMap<>();
//
//            if (data.contains("HTTP/1.0")) {
//                // Parse HTTP/1.0 request
//                String[] splitted = data.split("[ \r\n]+");
//
//                httpRequest.put("method", splitted[0]);
//                httpRequest.put("path", splitted[1].split(":")[0]);
//                httpRequest.put("httpVersion", splitted[2].split("\r")[0]);
//                httpRequest.put("host", splitted[1].split(":")[0]);
//                httpRequest.put("port", splitted[1].split(":")[1]);
//            } else {
//                // Parse other HTTP requests
//                String[] splitted = data.split("[\r\n]+");
//
//                String[] firstLine = splitted[0].trim().split(" ");
//                httpRequest.put("method", firstLine[0]);
//                httpRequest.put("path", firstLine[1]);
//                httpRequest.put("httpVersion", firstLine[2]);
//
//                int index = -1;
//                for (int i = 0; i < splitted.length; i++) {
//                    if (splitted[i].toLowerCase().startsWith("host: ")) {
//                        index = i;
//                        break;
//                    }
//                }
//                String hostLine = splitted[index];
//                String host = hostLine.split(":")[1].trim();
//                httpRequest.put("host", host);
//
//                String[] hostParts = hostLine.split(":");
//                if (hostParts.length > 2) {
//                    httpRequest.put("port", Integer.parseInt(hostParts[2].trim()));
//                } else {
//                    httpRequest.put("port", 80);
//                }
//            }
//
//            return httpRequest;
//        } catch (Exception ex) {
//            System.out.println("ParseHttpRequest error: " + ex.getMessage());
//            return null;
//        }
//    }
//}