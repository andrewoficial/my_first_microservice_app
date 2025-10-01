package org.example.gui.mgstest.util;

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
        return Arrays.equals(calculatedCrcBytes, exceptedCryBytes);
    }

    private static String bytesToHexNoSpace(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (byte b : data) {
            sb.append(String.format("%02X", b & 0xFF));
        }
        return sb.toString();
    }
}
