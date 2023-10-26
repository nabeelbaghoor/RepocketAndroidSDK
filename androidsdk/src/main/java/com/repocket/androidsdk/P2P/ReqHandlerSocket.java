package com.repocket.androidsdk.P2P;

import android.util.Log;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

public class ReqHandlerSocket extends Socket {
    public Boolean IsBusy;
    public Integer RetryConnectionCounter;
    public String Type;
    public Integer Uid;

    public ReqHandlerSocket()  {
        super();
        try {
            this.setTcpNoDelay(true);
        } catch (SocketException e) {
            Log.d("RepocketSDK", "ReqHandlerSocket -> ReqHandlerSocket -> SocketException: " + e);
            throw new RuntimeException(e);
        }
        // Additional socket initialization if needed
    }
}
