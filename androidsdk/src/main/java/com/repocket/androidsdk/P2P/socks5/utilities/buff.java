package com.repocket.androidsdk.P2P.socks5.utilities;

public class buff {
    public static byte[] convert(long num, int len, int byteOrder) {
        if (len < 1) {
            throw new IllegalArgumentException("len must be greater than 0");
        }

        byte[] buf = new byte[len];

        if (byteOrder == 0) {
            for (int i = len - 1; i >= 0; i--) {
                buf[i] = (byte) (num & 0xff);
                num >>= 8;
            }
        } else {
            for (int i = 0; i < len; i++) {
                buf[i] = (byte) (num & 0xff);
                num >>= 8;
            }
        }

        return buf;
    }
}
