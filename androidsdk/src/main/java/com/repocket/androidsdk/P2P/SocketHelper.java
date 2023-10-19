package com.repocket.androidsdk.P2P;

import android.util.Log;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class SocketHelper {
    public static void writeToSocket(SocketChannel socketChannel, byte[] data) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        Selector selector = Selector.open();
        socketChannel.configureBlocking(false);
        socketChannel.register(selector, SelectionKey.OP_WRITE);

        while (buffer.hasRemaining()) {
            if (selector.select() > 0) {
                for (SelectionKey key : selector.selectedKeys()) {
                    if (key.isWritable()) {
                        SocketChannel channel = (SocketChannel) key.channel();
                        int bytesWritten = channel.write(buffer);
                        if (bytesWritten == 0) {
                            Log.d("RepocketSDK", "SocketHelper -> writeToSocket -> bytesWritten == 0");
                            // The socket's internal buffer is full, you can handle this case here.
                        }
                    }
                }
            }
            selector.selectedKeys().clear();
        }

        socketChannel.close();
    }

//    public static void writeToSocket(Socket socket, byte[] data) throws IOException {
//        socket.getOutputStream().write(data);
//        socket.getOutputStream().flush(); // Flush the output stream to ensure the data is sent immediately
//        pause(socket);
//        socket.getInputStream().read(new byte[0]);
//        resume(socket);
//    }
//
//    private static void pause(Socket socket) throws IOException {
//        socket.setSoTimeout(0); // Set the socket to non-blocking mode
//    }
//
//    private static void resume(Socket socket) throws IOException {
//        socket.setSoTimeout(0); // Reset the socket to the default blocking mode
//    }
//
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
