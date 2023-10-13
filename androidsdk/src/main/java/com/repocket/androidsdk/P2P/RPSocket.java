package com.repocket.androidsdk.P2P;

import java.io.IOException;
import java.net.Socket;

public class RPSocket extends Socket {
    public boolean IsBusy;
    public int RetryConnectionCounter;
    public String Type;
    public int Uid;

    public RPSocket() throws IOException {
        super();
        this.setTcpNoDelay(true);
//        this.connect(new InetSocketAddress("your_ip_here", your_port_here));
    }
}
