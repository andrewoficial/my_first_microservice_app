package org.example.device.protGpsTest;

import lombok.Getter;
import org.apache.log4j.Logger;
import org.example.utilites.MyUtilities;

public class LoraMyGpsMessage {
    @Getter
    private final char[] hexData;
    private final char[] crcData;
    private final static int MIN_LENGTH = 34;
    private final static Logger log = Logger.getLogger(LoraMyGpsMessage.class);

    // Описание полей {start, length}
    private enum Field {
        LONGITUDE(0, 8),
        LATITUDE(8, 8),
        ACT_SATELLITES(16, 2),
        MESSAGE_NUM(18, 2),
        MESSAGES_TOTAL(20, 2),
        SERIAL_NUMBER(22, 4),
        BATTERY(26, 4);


        final int start;
        final int length;

        Field(int start, int length) {
            this.start = start;
            this.length = length;
        }
    }

    public LoraMyGpsMessage(String loraMessage) {
        validateInput(loraMessage);
        validateChecksum(loraMessage);
        this.hexData = loraMessage.substring(0, 30).toCharArray();
        this.crcData = loraMessage.substring(30,34).toCharArray();
    }

    public double parseLongitude(){
        long longitude = parseHexValue(hexData, Field.LONGITUDE.start, Field.LONGITUDE.length);
        return convertCoordinate(longitude);
    }

    public double parseLatitude(){
        long latitude = parseHexValue(hexData, Field.LATITUDE.start, Field.LATITUDE.length);
        return convertCoordinate(latitude);
    }

    public double parseActiveSatellites(){
        long activeSatellites = parseHexValue(hexData, Field.ACT_SATELLITES.start, Field.ACT_SATELLITES.length);
        return (double) activeSatellites;
    }

    public double parseMessageNumber(){
        long messageNumber = parseHexValue(hexData, Field.MESSAGE_NUM.start, Field.MESSAGE_NUM.length);
        return (double) messageNumber;
    }

    public double parseTotalMessages(){
        long messagesTotal = parseHexValue(hexData, Field.MESSAGES_TOTAL.start, Field.MESSAGES_TOTAL.length);
        return (double) messagesTotal;
    }

    public double parseSerialNumber(){
        long serialNumber = parseHexValue(hexData, Field.SERIAL_NUMBER.start, Field.SERIAL_NUMBER.length);
        return (double) serialNumber;
    }

    public double parseBatteryVoltage(){
        long batteryVoltage = parseHexValue(hexData, Field.SERIAL_NUMBER.start, Field.SERIAL_NUMBER.length);
        batteryVoltage /= 100;
        return (double) batteryVoltage;
    }
    private void validateInput(String message) {
        if (message == null || message.trim().isEmpty()) {
            logAndThrow("Передан пустой текст сообщения");
        }

        String trimmed = message.trim();
        if (trimmed.length() < MIN_LENGTH) {
            logAndThrow("Неполное сообщение: " + trimmed);
        }

        if (!trimmed.matches("^[0-9A-Fa-f]+$")) {
            logAndThrow("Недопустимые символы в сообщении");
        }
    }

    private void validateChecksum(String message) {
        String dataPart = message.substring(0, 30);
        String crcPart = message.substring(30, 34);
        int calculatedCrc = MyUtilities.calculateCRC16_GPS(dataPart.getBytes());
        int receivedCrc = (int) parseHexValue(crcPart.toCharArray(), 0, (int) crcPart.length());

        if (calculatedCrc != receivedCrc) {
            String errorMsg = String.format("CRC ошибка: расчет=%X, получено=%X", calculatedCrc, receivedCrc);
            logAndThrow(errorMsg);
        }
    }

    private double convertCoordinate(long rawValue) {
        rawValue = rawValue & 0xFFFFFFFFL;
        if ((rawValue & 0x80000000L) != 0) {
            rawValue = -(rawValue & 0x7FFFFFFFL);
        }
        return rawValue / 100000.0;
    }

    private static long parseHexValue(char[] data, int start, int length) {
        long result = 0;
        for (int i = start; i < start + length; i++) {
            result = (result << 4) | Character.digit(data[i], 16);
        }
        return result;
    }

    private void logAndThrow(String message) {
        log.error(message);
        throw new IllegalArgumentException(message);
    }
}