package com.repocket.androidsdk.P2P.socks5;

import android.util.Log;
import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Socket5Handler {
    private final int port;
    private final Socket reqHandlerSocket;
    private String dns;
    private String localAddress;
    private final byte[] reqHandlerBuffer;
    private final byte[] proxyBuffer;
    private Socket proxySocket;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    public Socket5Handler(Socket socket, int port, String localAddress, String dns) {
        this.reqHandlerSocket = socket;
        this.port = port;
        this.localAddress = localAddress;
        this.dns = dns;
        this.reqHandlerBuffer = new byte[8192];
        this.proxyBuffer = new byte[8192];
    }

    public void handle(byte[] data) throws UnknownHostException {
        Log.d("RepocketSDK","Socket5Handler -> handle: Handling SOCKS5 incoming request");

        if (data[0] != 0x05) {
            Log.d("RepocketSDK", "Socket5Handler -> handle: Unsupported SOCKS version: " + data[0]);
            reply((byte) 0x01);
            closeSocket(reqHandlerSocket);
            return;
        }

        switch (data[1]) {
            case 0x01: // CONNECT
                Log.d("RepocketSDK", "Socket5Handler -> handle: Handling CONNECT");
                handleConnectCommand(data);
                break;
            case 0x02: // BIND
                Log.d("RepocketSDK", "Socket5Handler -> handle: BIND METHOD REQUEST not supported");
                reply((byte) 0x07);
                closeSocket(reqHandlerSocket);
                break;
            case 0x03: // UDP ASSOCIATE
                Log.d("RepocketSDK", "Socket5Handler -> handle: Handling UDP ASSOCIATE");
                reply((byte) 0x00, new InetSocketAddress(0));
                closeSocket(reqHandlerSocket);
                break;
            default:
                Log.d("RepocketSDK", "Socket5Handler -> handle: Unsupported method: " + data[1]);
                reply((byte) 0x07);
                closeSocket(reqHandlerSocket);
                break;
        }
    }

    private void handleConnectCommand(byte[] data) throws UnknownHostException {
        if (data[2] != 0x00) {
            Log.d("RepocketSDK", "Socket5Handler -> handleConnectCommand: RESERVED should be 0x00");
        }

        String dstHost;
        int dstPort;

        switch (data[3]) {
            case 0x01: // IPv4
                dstHost = InetAddress.getByAddress(Arrays.copyOfRange(data, 4, 8)).getHostAddress();
                dstPort = ((data[8] & 0xFF) << 8) | (data[9] & 0xFF);
                createConnection(dstHost, dstPort, 3);
                break;
            case 0x03: // Domain
                int domainLen = data[4];
                String domain = new String(data, 5, domainLen, StandardCharsets.US_ASCII);
                try {
                    InetAddress[] ips = InetAddress.getAllByName(domain);
                    dstHost = ips[0].getHostAddress();
                    dstPort = ((data[5 + domainLen] & 0xFF) << 8) | (data[5 + domainLen + 1] & 0xFF);
                    createConnection(dstHost, dstPort, 3);
                } catch (UnknownHostException ex) {
                    Log.d("RepocketSDK", "Socket5Handler -> handleConnectCommand: Error resolving domain: " + ex.getMessage());
                    reply((byte) 0x04);
                    closeSocket(reqHandlerSocket);
                }
                break;
            case 0x04: // IPv6
                byte[] addrBytes = Arrays.copyOfRange(data, 4, 20);
                dstHost = InetAddress.getByAddress(addrBytes).getHostAddress();
                dstPort = ((data[20] & 0xFF) << 8) | (data[21] & 0xFF);
                createConnection(dstHost, dstPort, 3);
                break;
            default:
                Log.d("RepocketSDK", "Socket5Handler -> handleConnectCommand: ATYP " + data[3] + " not supported");
                reply((byte) 0x08);
                closeSocket(reqHandlerSocket);
                break;
        }
    }

    private void createConnection(String dstHost, int dstPort, int retries) {
        if (retries <= 0) {
            Log.d("RepocketSDK", "Socket5Handler -> createConnection: Connection retries exceeded");
            reply((byte) 0x05);
            closeSocket(reqHandlerSocket);
            return;
        }

        proxySocket = new Socket();
        try {
            proxySocket.connect(new InetSocketAddress(dstHost, dstPort));
            InetSocketAddress localEndPoint = (InetSocketAddress) proxySocket.getLocalSocketAddress();
            byte[] reply = new byte[4 + localEndPoint.getAddress().getAddress().length + 2];
            reply[0] = 0x05; // Version
            reply[1] = 0x00; // Succeeded
            reply[2] = 0x00; // Reserved
            reply[3] = (byte) (localEndPoint.getAddress() instanceof Inet6Address ? 0x04 : 0x01); // IPv6 or IPv4

            System.arraycopy(localEndPoint.getAddress().getAddress(), 0, reply, 4, localEndPoint.getAddress().getAddress().length);
            ByteBuffer portBuffer = ByteBuffer.allocate(2);
            portBuffer.putShort((short) localEndPoint.getPort());
            System.arraycopy(portBuffer.array(), 0, reply, 4 + localEndPoint.getAddress().getAddress().length, 2);

            reply((byte) 0x00, new InetSocketAddress(localEndPoint.getAddress(), localEndPoint.getPort()));
            proxySocket.setSoTimeout(0);
            proxySocket.setTcpNoDelay(true);
            proxySocket.setKeepAlive(true);
            proxySocket.setReuseAddress(true);

            proxySocket.getInputStream().read(proxyBuffer);
            proxySocket.getOutputStream().write(proxyBuffer);
            proxySocket.close();

            reqHandlerSocket.getInputStream().read(reqHandlerBuffer);
            reqHandlerSocket.getOutputStream().write(reqHandlerBuffer);
        } catch (IOException e) {
            Log.d("RepocketSDK", "Socket5Handler -> createConnection: Failed to connect to " + dstHost + ":" + dstPort + ", retrying (" + (retries - 1) + " attempts left)...");
            createConnection(dstHost, dstPort, retries - 1);
        }
    }

    private void reply(byte code) {
        reply(code, null);
    }

    private void reply(byte code, InetSocketAddress endpoint) {
        byte[] reply;
        if (endpoint != null) {
            byte[] addrBytes = endpoint.getAddress().getAddress();
            ByteBuffer portBuffer = ByteBuffer.allocate(2);
            portBuffer.putShort((short) endpoint.getPort());
            byte[] portBytes = portBuffer.array();
            reply = new byte[4 + addrBytes.length + portBytes.length];
            reply[3] = (byte) (addrBytes.length == 4 ? 0x01 : 0x04);
            System.arraycopy(addrBytes, 0, reply, 4, addrBytes.length);
            System.arraycopy(portBytes, 0, reply, 4 + addrBytes.length, portBytes.length);
        } else {
            reply = new byte[]{0x05, code, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
        }
        try {
            reqHandlerSocket.getOutputStream().write(reply);
        } catch (IOException e) {
            Log.d("RepocketSDK", "Socket5Handler -> reply: Failed to send reply: " + e.getMessage());
        }
    }

    private void closeSocket(Socket socket) {
        try {
            socket.close();
        } catch (IOException e) {
            Log.d("RepocketSDK", "Socket5Handler -> closeSocket: Failed to close socket: " + e.getMessage());
        }
    }

    public void end() {
        closeSocket(reqHandlerSocket);
        if (proxySocket != null) {
            closeSocket(proxySocket);
        }
        executorService.shutdown();
    }
}

