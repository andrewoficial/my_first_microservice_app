package org.example.gui.devices.tt5166.emulation;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Общие утилиты Modbus RTU для термокамеры TT5166 (38400 8E1).
 * CRC-16/Modbus идентичен {@code calculateModbusCRC} в {@code TT5166CommandRegistry}.
 */
public final class TT5166ModbusUtil {

    private TT5166ModbusUtil() {}

    // ─── CRC-16/Modbus ────────────────────────────────────────────────────

    public static short calculateCrc16(byte[] data) {
        int crc = 0xFFFF;
        for (byte b : data) {
            crc ^= (b & 0xFF);
            for (int i = 0; i < 8; i++) {
                if ((crc & 0x0001) != 0) {
                    crc = (crc >> 1) ^ 0xA001;
                } else {
                    crc >>= 1;
                }
            }
        }
        return (short) crc;
    }

    public static boolean validateCrc(byte[] frame) {
        if (frame == null || frame.length < 4) return false;
        int len = frame.length;
        short recv = (short) ((frame[len - 1] & 0xFF) << 8 | (frame[len - 2] & 0xFF));
        return recv == calculateCrc16(Arrays.copyOfRange(frame, 0, len - 2));
    }

    public static byte[] appendCrc(byte[] dataWithoutCrc) {
        short crc = calculateCrc16(dataWithoutCrc);
        ByteBuffer buf = ByteBuffer.allocate(dataWithoutCrc.length + 2);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.put(dataWithoutCrc);
        buf.putShort(crc);
        return buf.array();
    }

    // ─── Длина кадра (для сборки по байтам) ───────────────────────────────

    /**
     * Ожидаемая длина ЗАПРОСА (с CRC). 0x03/0x05/0x06 — фиксированные 8 байт.
     */
    public static int expectedRequestFrameLength(byte[] data) {
        if (data == null || data.length < 2) return 0;
        int function = data[1] & 0xFF;
        if (function == 0x03 || function == 0x05 || function == 0x06) return 8;
        return 0;
    }

    /**
     * Ожидаемая длина ОТВЕТА (с CRC). 0x05/0x06 — 8 байт, exception — 5 байт.
     */
    public static int expectedFrameLength(byte[] data) {
        if (data == null || data.length < 2) return 0;
        int function = data[1] & 0xFF;
        if (function == 0x03 || function == 0x04) {
            if (data.length < 3) return 0;
            return 3 + (data[2] & 0xFF) + 2;
        }
        if (function == 0x05 || function == 0x06) return 8;
        if ((function & 0x80) != 0) return 5;
        return 0;
    }

    // ─── Утилиты ──────────────────────────────────────────────────────────

    public static String bytesToHex(byte[] bytes) {
        if (bytes == null) return "";
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b & 0xFF));
        }
        return sb.toString().trim();
    }
}