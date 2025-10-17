package org.example.gui.mgstest.parser.answer.mkrs;

import org.apache.log4j.Logger;
import org.example.gui.mgstest.model.answer.GetDeviceInfoModel;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class GetDeviceInfoParserMkrs {
    private static final Logger log = Logger.getLogger(GetDeviceInfoParserMkrs.class);

    public static GetDeviceInfoModel parse(byte[] data) {
        validateDataLength(data);

        GetDeviceInfoModel info = new GetDeviceInfoModel();
        info.setLoaded(false);

        try {
            parseSerialNumber(info, data);
            parseFirmwareVersion(info, data);
            parseProductionDate(info, data);
            parseHardwareVersion(info, data);

            info.setLoaded(true);
            log.info("DeviceInfo parsed successfully");

        } catch (Exception e) {
            log.error("Error parsing DeviceInfo: " + e.getMessage(), e);
            throw new IllegalArgumentException("Failed to parse DeviceInfo: " + e.getMessage(), e);
        }

        return info;
    }

    private static void validateDataLength(byte[] data) {
        if (data.length < 64) {
            throw new IllegalArgumentException("Data too short, expected at least 64 bytes, got " + data.length);
        }
    }

    private static void parseSerialNumber(GetDeviceInfoModel info, byte[] data) {
        // Серийный номер 45985 найден по смещению 38 (байты A1 B3 в little-endian)
        int serial = ByteBuffer.wrap(data, 38, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
        info.setSerialNumber(serial);
        log.info("Serial number: " + serial + " (hex: " + String.format("%04X", serial) + ")");
    }

    private static void parseFirmwareVersion(GetDeviceInfoModel info, byte[] data) {
        // Версия прошивки 6.10 найдена по смещению 28-29 (байты 06 0A)
        int major = data[28] & 0xFF;  // 06 = 6
        int minor = data[29] & 0xFF;  // 0A = 10

        info.setSwMaj(major);
        info.setSwMin(minor);
        log.info("Firmware version: " + major + "." + minor);
    }

    private static void parseProductionDate(GetDeviceInfoModel info, byte[] data) {
        // Дата производства 2015.01.21 найдена по смещению 40-43 (байты 00 EC BE 54)
        // Это Unix timestamp в little-endian: 0x54BEEC00 = 1421798400 = 2015-01-21
        long timestamp = ByteBuffer.wrap(data, 40, 4).order(ByteOrder.LITTLE_ENDIAN).getInt() & 0xFFFFFFFFL;

        // Конвертируем timestamp в дату
        Instant instant = Instant.ofEpochSecond(timestamp);
        LocalDateTime date = LocalDateTime.ofInstant(instant, ZoneId.of("UTC"));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");
        String formattedDate = date.format(formatter);

        // Сохраняем в модель (если есть подходящее поле)
        log.info("Production date: " + formattedDate + " (timestamp: " + timestamp + ")");

        // Если в модели нет поля для даты, можно сохранить в другое поле или добавить новое
        info.setTime((int) timestamp); // Временное сохранение в поле time
    }

    private static void parseHardwareVersion(GetDeviceInfoModel info, byte[] data) {
        // Версия HW 4.0 найдена по смещению 30-31 (байты 04 00)
        int major = data[30] & 0xFF;  // 04 = 4
        int minor = data[31] & 0xFF;  // 00 = 0

        info.setHwMaj(major);
        info.setHwMin(minor);
        log.info("Hardware version: " + major + "." + minor);
    }
}