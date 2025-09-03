package org.example.gui.mgstest.device;

import org.apache.log4j.Logger;
import org.example.gui.mgstest.transport.CradleController;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;

public class DeviceInfo {
    public String cpuId;
    public int serialNumber;
    public int swMin;
    public int swMaj;
    public int hwMin;
    public int hwMaj;
    public int time;
    public boolean beepEnabled;
    public boolean vibroEnabled;
    public boolean alarmEnabled;
    // Другие поля при необходимости

    public static DeviceInfo parseDeviceInfo(byte[] data) {
        final Logger log = Logger.getLogger(CradleController.class);
        if (data.length < 128) {
            throw new IllegalArgumentException("Data too short, expected 128 bytes, got " + data.length);
        }

        DeviceInfo info = new DeviceInfo();

        // CPUId (12 bytes) starting from offset 28
        StringBuilder cpuIdBuilder = new StringBuilder();
        for (int i = 28; i < 40; i++) {
            cpuIdBuilder.append(String.format("%02X", data[i]));
        }
        info.cpuId = cpuIdBuilder.toString();
        log.info("cpuIdBuilder: " + info.cpuId);

        // Serial number (4 bytes, little-endian) at offset 40
        info.serialNumber = ByteBuffer.wrap(data, 40, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
        log.info("serialNumber: " + info.serialNumber);

        // Firmware version - might be at offset 48-49
        // Based on your dump, firmware 2.14 might be represented as 0x02 and 0x0E
        info.swMaj = data[48] & 0xFF;  // Major version
        info.swMin = data[49] & 0xFF;  // Minor version
        log.info("Firmware version: " + info.swMaj + "." + info.swMin);

        // Hardware version - might be at offset 50-51
        info.hwMaj = data[50] & 0xFF;
        info.hwMin = data[51] & 0xFF;
        log.info("Hardware version: " + info.hwMaj + "." + info.hwMin);

        // Time - might be at offset 55-58 (4 bytes, little-endian)
        try {
            info.time = ByteBuffer.wrap(data, 55, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            log.info("Time: " + info.time);

            // Convert timestamp to date if needed
            if (info.time != 0) {
                Date date = new Date(info.time * 1000L); // Assuming Unix timestamp
                log.info("Formatted time: " + date.toString());
            }
        } catch (Exception e) {
            log.warn("Invalid time value, might be device-specific format");
            info.time = 0;
        }

        // Flags - might be at offset 59-61
        // Based on your description: sound, vibration, general alarm
        info.beepEnabled = (data[59] & 0xFF) == 0x00;
        info.vibroEnabled = (data[60] & 0xFF) == 0x00;
        info.alarmEnabled = (data[61] & 0xFF) == 0x00;

        log.info("Beep enabled: " + info.beepEnabled);
        log.info("Vibration enabled: " + info.vibroEnabled);
        log.info("Alarm enabled: " + info.alarmEnabled);

        // Log period - might be at offset 53-54
        int logPeriod = ByteBuffer.wrap(data, 53, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
        log.info("Log period: " + logPeriod);

        return info;
    }
}
