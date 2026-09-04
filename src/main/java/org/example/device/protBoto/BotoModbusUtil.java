package org.example.device.protBoto;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Общие утилиты Modbus RTU для термокамер BOTO.
 * Построение запросов, парсинг ответов, CRC-16/Modbus.
 */
public final class BotoModbusUtil {

    private BotoModbusUtil() {}

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
        byte[] data = Arrays.copyOfRange(frame, 0, len - 2);
        short recvCrc = (short) ((frame[len - 1] & 0xFF) << 8 | (frame[len - 2] & 0xFF));
        return recvCrc == calculateCrc16(data);
    }

    public static byte[] appendCrc(byte[] dataWithoutCrc) {
        short crc = calculateCrc16(dataWithoutCrc);
        ByteBuffer buf = ByteBuffer.allocate(dataWithoutCrc.length + 2);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.put(dataWithoutCrc);
        buf.putShort(crc);
        return buf.array();
    }

    // ─── Построение запросов ──────────────────────────────────────────────

    /**
     * Запрос чтения holding registers (функция 0x03).
     */
    public static byte[] buildReadHoldingRequest(int slaveId, int startReg, int quantity) {
        ByteBuffer buf = ByteBuffer.allocate(6);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.put((byte) slaveId);
        buf.put((byte) 0x03);
        buf.putShort((short) startReg);
        buf.putShort((short) quantity);
        return appendCrc(buf.array());
    }

    /**
     * Запрос записи одного регистра (функция 0x06).
     */
    public static byte[] buildWriteSingleRequest(int slaveId, int regAddr, int value) {
        ByteBuffer buf = ByteBuffer.allocate(6);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.put((byte) slaveId);
        buf.put((byte) 0x06);
        buf.putShort((short) regAddr);
        buf.putShort((short) value);
        return appendCrc(buf.array());
    }

    // ─── Парсинг ответов ──────────────────────────────────────────────────

    /**
     * Парсинг ответа на чтение (0x03). Возвращает значения регистров.
     */
    public static int[] parseReadResponse(byte[] response) {
        if (response == null || response.length < 5) return null;
        if (!validateCrc(response)) return null;
        if ((response[1] & 0x80) != 0) return null; // exception
        int byteCount = response[2] & 0xFF;
        int regCount = byteCount / 2;
        int[] values = new int[regCount];
        for (int i = 0; i < regCount; i++) {
            values[i] = ((response[3 + 2 * i] & 0xFF) << 8) | (response[4 + 2 * i] & 0xFF);
        }
        return values;
    }

    /**
     * Парсинг ответа на запись (0x06). Возвращает [regAddr, value] или null.
     */
    public static int[] parseWriteResponse(byte[] response) {
        if (response == null || response.length < 8) return null;
        if (!validateCrc(response)) return null;
        if ((response[1] & 0x80) != 0) return null;
        int regAddr = ((response[2] & 0xFF) << 8) | (response[3] & 0xFF);
        int value = ((response[4] & 0xFF) << 8) | (response[5] & 0xFF);
        return new int[]{regAddr, value};
    }

    // ─── Длина кадра (для парсинга по байтам) ─────────────────────────────

    /**
     * Возвращает ожидаемую длину Modbus-кадра (с CRC) по первым байтам.
     * 0 = кадр не опознан.
     * <p>Версия для ОТВЕТОВ (ответ на 0x03/0x04 содержит byteCount в data[2]).</p>
     */
    public static int expectedFrameLength(byte[] data) {
        if (data == null || data.length < 2) return 0;
        int function = data[1] & 0xFF;
        if (function == 0x03 || function == 0x04) {
            if (data.length < 3) return 0;
            int byteCount = data[2] & 0xFF;
            return 3 + byteCount + 2;
        }
        if (function == 0x06) return 8;
        if (function == 0x10) {
            if (data.length < 7) return 0;
            int byteCount = data[6] & 0xFF;
            return 7 + byteCount + 2;
        }
        if ((function & 0x80) != 0) return 5;
        return 0;
    }

    /**
     * Возвращает ожидаемую длину Modbus-кадра (с CRC) по первым байтам для ЗАПРОСОВ.
     * <p>Запросы 0x03/0x04/0x06 — фиксированная длина 8 байт.
     * Запрос 0x10 (запись нескольких) — переменная, byteCount в data[6].</p>
     * 0 = кадр не опознан.
     */
    public static int expectedRequestFrameLength(byte[] data) {
        if (data == null || data.length < 2) return 0;
        int function = data[1] & 0xFF;
        if (function == 0x03 || function == 0x04 || function == 0x06) return 8;
        if (function == 0x10) {
            if (data.length < 7) return 0;
            int byteCount = data[6] & 0xFF;
            return 7 + byteCount + 2;
        }
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
