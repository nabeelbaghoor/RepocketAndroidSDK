package com.repocket.androidsdk.P2P.socks5.utilities;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public class Ipv6 {

    public static byte[] toByteArray(String addr) {
        byte[] bufArr = new byte[16];
        InetAddress ipAddr;
        try {
            ipAddr = InetAddress.getByName(addr);
        } catch (UnknownHostException e) {
            return bufArr;
        }

        byte[] addressBytes = ipAddr.getAddress();

        int index = 0;
        int doubleColonIndex = -1;
        int ipv4Index = -1;
        StringBuilder groupStr = new StringBuilder();

        for (int i = 0; i < addressBytes.length; i++) {
            byte b = addressBytes[i];

            if (b == (byte) 58) { // 58 :
                if (groupStr.length() > 0) {
                    int byte2 = Integer.parseInt(groupStr.toString(), 16);
                    bufArr[index++] = (byte) (byte2 >> 8);
                    bufArr[index++] = (byte) (byte2 & 0xFF);
                    groupStr.setLength(0);
                }

                if (i < addressBytes.length - 1 && addressBytes[i + 1] == (byte) 58) {
                    doubleColonIndex = index;
                    i++;
                }
            } else if (b == (byte) 46) { // 46 .
                if (ipv4Index == -1) ipv4Index = index;
                if (groupStr.length() > 0) {
                    int byte1 = Integer.parseInt(groupStr.toString(), 10);
                    bufArr[index++] = (byte) byte1;
                    groupStr.setLength(0);
                }
            } else {
                groupStr.append(String.format("%02X", b));
            }
        }

        if (groupStr.length() > 0) {
            if (ipv4Index > -1) {
                int byte1 = Integer.parseInt(groupStr.toString(), 10);
                bufArr[index++] = (byte) byte1;
            } else {
                int byte2 = Integer.parseInt(groupStr.toString(), 16);
                bufArr[index++] = (byte) (byte2 >> 8);
                bufArr[index++] = (byte) (byte2 & 0xFF);
            }
            groupStr.setLength(0);
        }

        if (doubleColonIndex > -1) {
            int offset = 16 - index;
            for (int i = index - 1; i >= doubleColonIndex; i--) {
                bufArr[i + offset] = bufArr[i];
                bufArr[i] = (byte) 0x00;
            }
        }

        return bufArr;
    }

    public static String toString(byte[] buf) {
        if (buf.length != 16) {
            throw new IllegalArgumentException("Buffer length must be 16 bytes");
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i += 2) {
            int dw = ByteBuffer.wrap(buf, i, 2).getShort();
            sb.append(Integer.toHexString(dw));
            if (i < 14) {
                sb.append(":");
            }
        }

        return sb.toString();
    }

    public static String toString2(byte[] buf) {
        if (buf.length != 16) {
            throw new IllegalArgumentException("Buffer length must be 16 bytes");
        }

        StringBuilder sb = new StringBuilder();
        boolean zeroStarted = false;
        for (int i = 0; i < 16; i += 2) {
            int dw = ByteBuffer.wrap(buf, i, 2).getShort();

            if (dw == 0 && !zeroStarted) {
                zeroStarted = true;
                if (i == 0 || i == 14) {
                    sb.append(":");
                }
            } else if (dw != 0 && zeroStarted) {
                zeroStarted = false;
                if (i > 2 && sb.charAt(sb.length() - 1) == ':') {
                    sb.deleteCharAt(sb.length() - 1);
                }
            }

            if (!zeroStarted) {
                sb.append(Integer.toHexString(dw));
            }

            if (i < 14 && !zeroStarted) {
                sb.append(":");
            }
        }

        return sb.toString();
    }

//    public static void main(String[] args) {
//        String ipAddress = "2001:0db8:85a3:0000:0000:8a2e:0370:7334";
//        byte[] byteArray = toByteArray(ipAddress);
//        System.out.println(toString(byteArray));
//        System.out.println(toString2(byteArray));
//    }
}
