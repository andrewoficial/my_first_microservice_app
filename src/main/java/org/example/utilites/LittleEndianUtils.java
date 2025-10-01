package org.example.utilites;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.CRC32;

public class LittleEndianUtils {
    
    /**
     * Преобразует int в массив байт в little-endian порядке
     */
    public static byte[] intToBytesLE(int value) {
        return ByteBuffer.allocate(4)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putInt(value)
                .array();
    }
    
    /**
     * Преобразует массив байт (little-endian) в int
     */
    public static int bytesToIntLE(byte[] bytes) {
        return bytesToIntLE(bytes, 0);
    }
    
    /**
     * Преобразует массив байт (little-endian) в int начиная с указанной позиции
     */
    public static int bytesToIntLE(byte[] bytes, int offset) {
        if (bytes.length - offset < 4) {
            throw new IllegalArgumentException("Недостаточно байт для преобразования в int");
        }
        
        return ByteBuffer.wrap(bytes, offset, 4)
                .order(ByteOrder.LITTLE_ENDIAN)
                .getInt();
    }
    
    /**
     * Преобразует long в массив байт в little-endian порядке
     */
    public static byte[] longToBytesLE(long value) {
        return ByteBuffer.allocate(8)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putLong(value)
                .array();
    }
    
    /**
     * Преобразует массив байт (little-endian) в long
     */
    public static long bytesToLongLE(byte[] bytes) {
        return bytesToLongLE(bytes, 0);
    }
    
    /**
     * Преобразует массив байт (little-endian) в long начиная с указанной позиции
     */
    public static long bytesToLongLE(byte[] bytes, int offset) {
        if (bytes.length - offset < 8) {
            throw new IllegalArgumentException("Недостаточно байт для преобразования в long");
        }
        
        return ByteBuffer.wrap(bytes, offset, 8)
                .order(ByteOrder.LITTLE_ENDIAN)
                .getLong();
    }
    
    /**
     * Преобразует short в массив байт в little-endian порядке (2 байта)
     */
    public static byte[] shortToBytesLE(short value) {
        return ByteBuffer.allocate(2)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putShort(value)
                .array();
    }
    
    /**
     * Преобразует массив байт (little-endian) в short
     */
    public static short bytesToShortLE(byte[] bytes) {
        return bytesToShortLE(bytes, 0);
    }
    
    /**
     * Преобразует массив байт (little-endian) в short начиная с указанной позиции
     */
    public static short bytesToShortLE(byte[] bytes, int offset) {
        if (bytes.length - offset < 2) {
            throw new IllegalArgumentException("Недостаточно байт для преобразования в short");
        }
        
        return ByteBuffer.wrap(bytes, offset, 2)
                .order(ByteOrder.LITTLE_ENDIAN)
                .getShort();
    }
    
    /**
     * Вычисляет CRC32 для данных и возвращает в виде массива байт (little-endian)
     */
    public static byte[] calculateCrc32LE(byte[] data) {
        CRC32 crc = new CRC32();
        crc.update(data);
        long crcValue = crc.getValue();
        
        // Берем только младшие 4 байта (CRC32)
        return intToBytesLE((int) crcValue);
    }
    
    /**
     * Вычисляет CRC32 и возвращает long значение (для сравнения)
     */
    public static long calculateCrc32(byte[] data) {
        CRC32 crc = new CRC32();
        crc.update(data);
        return crc.getValue();
    }
    
    /**
     * Сравнивает вычисленный CRC с полученным (в little-endian формате)
     */
    public static boolean verifyCrc(byte[] data, byte[] receivedCrcLE) {
        long calculatedCrc = calculateCrc32(data);
        long receivedCrc = bytesToIntLE(receivedCrcLE) & 0xFFFFFFFFL;
        return calculatedCrc == receivedCrc;
    }
    
    /**
     * Форматирует байты в HEX строку для удобного вывода
     */
    public static String bytesToHex(byte[] bytes) {
        if (bytes == null) return "null";
        
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }
    
    /**
     * Преобразует HEX строку в массив байт
     */
    public static byte[] hexToBytes(String hexString) {
        String cleaned = hexString.replaceAll("\\s+", "");
        if (cleaned.length() % 2 != 0) {
            throw new IllegalArgumentException("HEX строка должна содержать четное количество символов");
        }
        
        byte[] result = new byte[cleaned.length() / 2];
        for (int i = 0; i < result.length; i++) {
            int index = i * 2;
            result[i] = (byte) Integer.parseInt(cleaned.substring(index, index + 2), 16);
        }
        return result;
    }
}