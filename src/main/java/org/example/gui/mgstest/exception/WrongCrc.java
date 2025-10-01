package org.example.gui.mgstest.exception;

import java.util.zip.CRC32;
import java.util.Arrays;
import org.apache.log4j.Logger;

public class WrongCrc extends IllegalArgumentException{
    private final Logger log = Logger.getLogger(WrongCrc.class);
    public WrongCrc(byte[] msg, int exceptStart, int exceptEnd, int crcOffset, String comment){
        log.warn("Received message with wrong crc. Message size: " + msg.length +
                " except start payload position: " + exceptStart + " except end payload position: " + exceptEnd +
                " except start crcOffset position: " + crcOffset + " " + comment
                );
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < msg.length; i++) {
            int unsignedValue = msg[i] & 0xFF;
            sb.append(String.format("%02X ", unsignedValue));
        }
        log.info("Начинаю поиск CRC в произвольном месте для произвольного размера данных");
        findFloatingPart(msg, sb.toString());
    }


    private void findFloatingPart(byte[] data, String allPackage) {
        allPackage = allPackage.replaceAll("\\s+", "").toUpperCase();
        log.info("Начинаю поиск возможного местоположения CRC");
        if (data.length < 4) {
            log.info("Массив слишком мал для поиска CRC");
            return;
        }

        // размер окна — от 1 до длины (можешь поставить min 2 или 4, как нужно)
        for (int size = 1; size <= data.length; size++) {
            for (int start = 0; start <= data.length - size; start++) {
                byte[] parts = Arrays.copyOfRange(data, start, start + size);
                byte[] crcLE = calculateCrcLittleEndian(parts);
                byte[] crcBE = reverse(crcLE);

                String strCrcLE = bytesToHexNoSpace(crcLE);
                String strCrcBE = bytesToHexNoSpace(crcBE);

                if (allPackage.contains(strCrcLE)) {
                    log.info("Found (LE) at start " + start + " and end " + (start + size - 1) + ", CRC: " + strCrcLE);
                    return;
                }
                if (allPackage.contains(strCrcBE)) {
                    log.info("Found (BE) at start " + start + " and end " + (start + size - 1) + ", CRC: " + strCrcBE);
                    return;
                }
                // Убираю частый noisy вывод "Not found in start position" — он засоряет лог
            }
        }
    }

    private byte[] calculateCrcLittleEndian(byte[] data) {
        CRC32 crc = new CRC32();
        crc.update(data);
        long crcValue = crc.getValue();
        byte[] crcBytes = new byte[4];
        crcBytes[0] = (byte) (crcValue & 0xFF);
        crcBytes[1] = (byte) ((crcValue >> 8) & 0xFF);
        crcBytes[2] = (byte) ((crcValue >> 16) & 0xFF);
        crcBytes[3] = (byte) ((crcValue >> 24) & 0xFF);
        return crcBytes;
    }

    private byte[] reverse(byte[] src) {
        byte[] out = new byte[src.length];
        for (int i = 0; i < src.length; i++) out[i] = src[src.length - 1 - i];
        return out;
    }

    private String bytesToHexNoSpace(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (byte b : data) {
            sb.append(String.format("%02X", b & 0xFF));
        }
        return sb.toString();
    }


}
