package com.repocket.androidsdk.P2P;

import java.io.IOException;
import java.net.Socket;

public class ReqHandlerSocket extends Socket {
    public Boolean IsBusy;
    public Integer RetryConnectionCounter;
    public String Type;
    public Integer Uid;

    public ReqHandlerSocket() throws IOException {
        super();
        this.setTcpNoDelay(true);
        // Additional socket initialization if needed
    }
}
