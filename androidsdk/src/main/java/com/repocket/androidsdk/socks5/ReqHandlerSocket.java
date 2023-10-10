package com.repocket.androidsdk.socks5;

import java.io.IOException;
import java.net.Socket;

public class ReqHandlerSocket extends Socket {
    public Boolean isBusy;
    public Integer retryConnectionCounter;
    public String type;
    public Integer uid;

    public ReqHandlerSocket() throws IOException {
        super();
    }

    public ReqHandlerSocket(String host, int port) throws IOException {
        super(host, port);
    }

    public ReqHandlerSocket(String host, int port, boolean isSocks) throws IOException {
        super(host, port);
        if (isSocks) {
            // Handle events for SOCKS5
        }
    }
}
