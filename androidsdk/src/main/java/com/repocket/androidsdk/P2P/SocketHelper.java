package com.repocket.androidsdk.P2P;

import android.util.Log;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class SocketHelper {
    public static void writeToSocket(Socket socket, byte[] data) {
        try {
            if(socket.isClosed()){
                Log.d("RepocketSDK", "SocketHelper -> writeToSocket: Socket was closed!" );
                return;
            }
            socket.getOutputStream().write(data);
            socket.getOutputStream().flush(); // Flush the output stream to ensure the data is sent immediately
        } catch (IOException e) {
            Log.d("RepocketSDK", "SocketHelper -> writeToSocket -> IOException: " + e);
        }
    }

//    public static boolean socketConnected(Socket socket) {
//        try {
//            boolean part1 = socket.getSoTimeout() == 0;
//            boolean part2 = socket.getInputStream().available() == 0;
//            return !(part1 && part2);
//        } catch (IOException e) {
//            return false;
//        }
//    }
}
