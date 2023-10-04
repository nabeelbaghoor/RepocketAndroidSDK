package com.repocket.androidsdk.socks5;

import java.io.IOException;
import java.net.Socket;

public class SocketHelper {
    public static void writeToSocket(Socket socket, byte[] data) throws IOException {
        socket.getOutputStream().write(data);
        socket.getOutputStream().flush(); // Flush the output stream to ensure the data is sent immediately
        pause(socket);
        socket.getInputStream().read(new byte[0]);
        resume(socket);
    }

    private static void pause(Socket socket) throws IOException {
        socket.setSoTimeout(0); // Set the socket to non-blocking mode
    }

    private static void resume(Socket socket) throws IOException {
        socket.setSoTimeout(0); // Reset the socket to the default blocking mode
    }

    public static boolean socketConnected(Socket socket) {
        try {
            boolean part1 = socket.getSoTimeout() == 0;
            boolean part2 = socket.getInputStream().available() == 0;
            return !(part1 && part2);
        } catch (IOException e) {
            return false;
        }
    }
}
