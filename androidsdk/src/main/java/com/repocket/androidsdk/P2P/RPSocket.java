package com.repocket.androidsdk.P2P;

import android.util.Log;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

public class RPSocket extends Socket {
    public boolean IsBusy;
    public int RetryConnectionCounter;
    public String Type;
    public int Uid;

    public RPSocket() {
        super();
        try {
            this.setTcpNoDelay(true);
        } catch (SocketException e) {
            Log.d("RepocketSDK", "RPSocket -> RPSocket -> SocketException: " + e);
        }
//        this.connect(new InetSocketAddress("your_ip_here", your_port_here));
    }
}
