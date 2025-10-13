package org.example.gui.mgstest.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.zip.CRC32;
import org.apache.log4j.Logger;

public class CrcValidator {
    public static boolean checkCrc(byte[] data, int payloadStart, int payloadEnd, int crcOffset) {
        final Logger log = Logger.getLogger(CrcValidator.class);
        if(data.length < payloadStart || data.length < payloadEnd || data.length < (crcOffset + 5)){
            log.warn("В проверку payload по crc передан слишком маленький массив данных" +
                    " payloadStart: " + payloadStart +
                    " payloadEnd: " + payloadEnd +
                    " crcOffset: " + crcOffset +
                    " crc end (crcOffset + 5): " + (crcOffset + 5) +
                    " data.length: " + data.length);
            return false;
        }
        // Извлекаем payload
        byte[] payload = Arrays.copyOfRange(data, payloadStart, payloadEnd);
        byte[] exceptedCryBytes = Arrays.copyOfRange(data, crcOffset, crcOffset+4);

        // Рассчитываем CRC для payload
        CRC32 crc = new CRC32();
        crc.update(payload);
        long crcValue = crc.getValue();
        byte[] calculatedCrcBytes = new byte[4];
        calculatedCrcBytes[0] = (byte) (crcValue & 0xFF);
        calculatedCrcBytes[1] = (byte) ((crcValue >> 8) & 0xFF);
        calculatedCrcBytes[2] = (byte) ((crcValue >> 16) & 0xFF);
        calculatedCrcBytes[3] = (byte) ((crcValue >> 24) & 0xFF);
        log.warn("Calculated: " + bytesToHexNoSpace(calculatedCrcBytes));
        log.warn("Got: " + bytesToHexNoSpace(exceptedCryBytes));
        log.warn("Payload: " + bytesToHexNoSpace(payload));
        return Arrays.equals(calculatedCrcBytes, exceptedCryBytes);
    }

    private static String bytesToHexNoSpace(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (byte b : data) {
            sb.append(String.format("%02X", b & 0xFF));
        }
        return sb.toString();
    }

    public static void validateSerialNumber(String input) throws IllegalArgumentException {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Serial number cannot be empty");
        }

        if (!input.matches("\\d{8}")) {
            throw new IllegalArgumentException("Please enter exactly 8 digits");
        }
    }

    public static byte[] calculateCRCBytes(byte[] data) {
        int crc = calculateCRC(data);
        System.out.println("Calculated CRC: " + Integer.toHexString(crc));
        ByteBuffer bb = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(crc);
        byte[] crcBytes = bb.array();
        System.out.println("CRC bytes: " + bytesToHex(crcBytes));
        return crcBytes;
    }

    // Пример CRC-вычисления (адаптировано из C++: CRC-32 reversed, poly 0xEDB88320)
    // Вычисляет над data (без header/FE)
    public static int calculateCRC(byte[] data) {
        int crc = 0xFFFFFFFF;
        int poly = 0xEDB88320;
        for (byte b : data) {
            crc ^= (b & 0xFF);
            for (int i = 0; i < 8; i++) {
                if ((crc & 1) != 0) {
                    crc = (crc >>> 1) ^ poly;
                } else {
                    crc = (crc >>> 1);
                }
            }
        }

        return ~crc;
    }

    public static void coolPrint(byte[] array) {
        System.out.println("Cool print of array:");
        for (int i = 0; i < array.length; i += 4) {
            StringBuilder line = new StringBuilder();
            for (int j = 0; j < 4; j++) {
                if (i + j < array.length) {
                    line.append(String.format("%02X ", array[i + j] & 0xFF));
                } else {
                    line.append("00 ");
                }
            }
            System.out.println(line.toString().trim());
        }
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b & 0xFF));
        }
        return sb.toString().trim();
    }

    public static ArrayList<byte[]> splitIntoParts(byte[] source, int chunkSize) {
        ArrayList<byte[]> result = new ArrayList<>();

        for (int i = 0; i < source.length; i += chunkSize) {
            byte[] chunk = new byte[chunkSize];
            int bytesToCopy = Math.min(chunkSize, source.length - i);
            System.arraycopy(source, i, chunk, 0, bytesToCopy);
            result.add(chunk);
        }
        return result;
    }

    public static ArrayList<byte[]> addPrefixWriteToParts(byte [] payload) {
        ArrayList<byte[]> parts = CrcValidator.splitIntoParts(payload, 4);
        ArrayList<byte[]> result = new ArrayList<>(parts.size() + 1);
        for (int i = 0; i < parts.size(); i++) {
            byte [] part = new byte[10];
            part[0] = (byte) 0x01;
            part[1] = (byte) 0x04;
            part[2] = (byte) 0x07;
            part[3] = (byte) 0x02;
            part[4] = (byte) 0x21;
            part[5] = (byte) i;
            part[6] = (byte) parts.get(i)[0];
            part[7] = (byte) parts.get(i)[1];
            part[8] = (byte) parts.get(i)[2];
            part[9] = (byte) parts.get(i)[3];
            //System.arraycopy(parts.get(i), 0, part, 4, parts.get(i).length);
            result.add(part);
        }
        return result;
    }


    public static void writeDataEmulator(ArrayList<byte[]> parts) {
        for (int i = 0; i < parts.size(); i++) {
            String partNumber = String.format("%02X", i);
            System.out.println("01 04 07 02 21 " + partNumber + " " + bytesToHex(parts.get(i)));
        }
    }

    public static void readDataEmulator(byte[] requestOffsets) {
        for (byte offset : requestOffsets) {
            String partNumber = String.format("%02X", offset);
            System.out.println("01 04 04 02 23 " + partNumber + " 07");
        }
    }

}
