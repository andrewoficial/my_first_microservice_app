package org.example.gui.autoresponder;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ByteUtils {

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }

    public static byte[] hexStringToBytes(String hex) {
        hex = hex.replace(" ", "");
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length(); i += 2) {
            bytes[i / 2] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
        }
        return bytes;
    }

    public static byte[] floatToBytes(float value) {
        ByteBuffer bb = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(value);
        return bb.array();
    }

    public static byte[] concatenate(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    public static byte calculateCs(byte[] data) {
        int sum = 0;
        for (int i = 2; i < data.length; i++) {
            sum += (data[i] & 0xFF);
        }
        return (byte) (sum % 256);
    }
}