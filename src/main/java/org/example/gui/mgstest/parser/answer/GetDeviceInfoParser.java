package org.example.gui.mgstest.parser.answer;

import org.apache.log4j.Logger;
import org.example.gui.mgstest.model.answer.GetDeviceInfoModel;
import org.example.gui.mgstest.util.CrcValidator;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;

public class GetDeviceInfoParser {
    private static final Logger log = Logger.getLogger(GetDeviceInfoParser.class);
    
    // Bitmasks for HW flags
    private static final int HW_LORA = 0x01;
    private static final int HW_SUBSEC_TIM = 0x02;
    private static final int HW_RTC_CLOCK_QUARZ = 0x04;
    private static final int HW_RTC_CLOCK_LSI = 0x08;

    public static GetDeviceInfoModel parse(byte[] data) {
        validateDataLength(data);
        validateCrc(data); // Добавляем проверку CRC

        GetDeviceInfoModel info = new GetDeviceInfoModel();
        info.setLoaded(false);

        try {
            parseCpuId(info, data);
            parseSerialNumber(info, data);
            parseCompatibilityAndReplaceCount(info, data);
            parseSoftwareVersions(info, data);
            parseHardwareVersions(info, data);
            parseEnabledHardware(info, data);
            parseTime(info, data);
            parseSettings(info, data);
            parseAdditionalSettings(info, data);
            
            info.setLoaded(true);
            log.info("DeviceInfo parsed successfully: " + info);
            
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

    private static void validateCrc(byte[] data) {
        // TODO: Настройте параметры CRC для DeviceInfo
        // Нужно определить: payloadStart, payloadEnd, crcOffset для DeviceInfo пакета
         if (!CrcValidator.checkCrc(data, 27, 67, 67)) {
             throw new IllegalArgumentException("CRC validation failed for DeviceInfo");
         }
    }

    private static void parseCpuId(GetDeviceInfoModel info, byte[] data) {
        StringBuilder cpuIdBuilder = new StringBuilder();
        for (int i = 27; i <= 38; i++) {
            cpuIdBuilder.append(String.format("%02x", data[i] & 0xFF));
        }
        info.setCpuId(cpuIdBuilder.toString());
        log.info("CPU ID: " + info.getCpuId());
    }

    private static void parseSerialNumber(GetDeviceInfoModel info, byte[] data) {
        info.setSerialNumber(ByteBuffer.wrap(data, 39, 4).order(ByteOrder.LITTLE_ENDIAN).getInt());
        log.info("Serial number: " + info.getSerialNumber());

        // Debug поиск серийного номера
        for (int i = 0; i < data.length - 3; i++) {
            int candidate = ByteBuffer.wrap(data, i, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            if (candidate == 12345677) {
                log.info("Found serial 12345677 at offset " + i);
            }
        }
        log.info("Serial expected at position 39");
    }

    private static void parseCompatibilityAndReplaceCount(GetDeviceInfoModel info, byte[] data) {
        byte btCompatibility = 0;
        info.setReplaceCount((byte) 0);

        if (data[43] == 0x40 && data[44] == (byte) 0xE2) {
            btCompatibility = 0;
        } else {
            btCompatibility = 4;
            info.setReplaceCount(data[43]);
        }
        log.info("Compatibility offset: " + (int) btCompatibility + ", replaceCount: " + (int) info.getReplaceCount());
        
        // Сохраняем compatibility для использования в других методах
        info.setVerControl(btCompatibility); // Временно используем это поле
    }

    private static void parseSoftwareVersions(GetDeviceInfoModel info, byte[] data) {
        byte compatibility = calculateCompatibility(data);
        info.setSwMin(data[51 - compatibility] & 0xFF);
        info.setSwMaj(data[52 - compatibility] & 0xFF);
        log.info("Firmware raw: " + info.getSwMaj() + "." + info.getSwMin());

        int nSWVer = info.getSwMaj() * 100 + info.getSwMin();
        info.setVerControl((byte) (nSWVer < 27 ? 1 : 0));
        log.info("Computed nSWVer: " + nSWVer + ", Version control: " + info.getVerControl());
    }

    private static void parseHardwareVersions(GetDeviceInfoModel info, byte[] data) {
        byte compatibility = calculateCompatibility(data);
        info.setHwMin(data[53 - compatibility] & 0xFF);
        info.setHwMaj(data[54 - compatibility] & 0xFF);
        log.info("Hardware raw: " + info.getHwMaj() + "." + info.getHwMin());
    }

    private static void parseEnabledHardware(GetDeviceInfoModel info, byte[] data) {
        int nSWVer = info.getSwMaj() * 100 + info.getSwMin();
        StringBuilder strHW = new StringBuilder(" ");
        
        if (nSWVer >= 97) {
            info.setEnabledHW(data[46]);
            if ((info.getEnabledHW() & HW_LORA) != 0) strHW.append("L");
            else strHW.append("N");
            if ((info.getEnabledHW() & HW_SUBSEC_TIM) != 0) strHW.append("T");
            else strHW.append("R");
            if ((info.getEnabledHW() & HW_RTC_CLOCK_QUARZ) != 0) strHW.append("Q");
            else if ((info.getEnabledHW() & HW_RTC_CLOCK_LSI) != 0) strHW.append("I");
            else strHW.append("G");
        }
        
        info.setSoftwareVer(String.format("%d.%02d%s", info.getSwMaj(), info.getSwMin(), strHW.toString()));
        info.setHardwareVer(String.format("%d.%02d", info.getHwMaj(), info.getHwMin()));
        
        log.info("Enabled HW: " + (int) info.getEnabledHW() + ", strHW: " + strHW);
        log.info("Software ver formatted: " + info.getSoftwareVer());
        log.info("Hardware ver formatted: " + info.getHardwareVer());
    }
    private static byte calculateCompatibility(byte[] data){
        if (data[43] == 0x40 && data[44] == (byte) 0xE2) {
            return 0;
        } else {
            return 4;
        }
    }
    private static void parseTime(GetDeviceInfoModel info, byte[] data) {
        byte compatibility = calculateCompatibility(data);

        int rawTime = ByteBuffer.wrap(data, 55 - compatibility, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
        long timeValue = rawTime & 0xFFFFFFFFL;
        info.setTime((int) timeValue);
        
        log.info("Time (raw unsigned): " + timeValue);
        
        if (timeValue == 0) {
            throw new IllegalArgumentException("Invalid time value (0)");
        }

        if (timeValue > 0 && timeValue < 0xFFFFFFFFL) {
            Date date = new Date(timeValue * 1000L);  // Assuming Unix seconds to ms
            log.info("Formatted time: " + date.toString());
        }
    }

    private static void parseSettings(GetDeviceInfoModel info, byte[] data) {
        byte compatibility = calculateCompatibility(data);

        // Validate bytes 59-61
        for (int i = 59; i <= 61; i++) {
            byte val = data[i - compatibility];
            if (val != 0 && val != 1) {
                throw new IllegalArgumentException("Invalid byte at offset " + (i - compatibility) + 
                        ": " + (int) val + " (must be 0 or 1)");
            }
        }

        // Beep/Vibro/Alarm settings
        info.setBeepEnabled((data[59 - compatibility] & 0xFF) == 0);
        info.setVibroEnabled((data[60 - compatibility] & 0xFF) == 0);
        info.setAlarmEnabled((data[61 - compatibility] & 0xFF) == 0);
        
        log.info("Beep enabled: " + info.isBeepEnabled());
        log.info("Vibro enabled: " + info.isVibroEnabled());
        log.info("Alarm enabled: " + info.isAlarmEnabled());
    }

    private static void parseAdditionalSettings(GetDeviceInfoModel info, byte[] data) {
        byte compatibility = calculateCompatibility(data);

        info.setDaylightSaving((data[67 - compatibility] & 0xFF) != 0);
        info.setLogTimeout(data[68 - compatibility] & 0xFF);
        
        log.info("Daylight saving: " + info.isDaylightSaving());
        log.info("Log timeout: " + info.getLogTimeout());
    }
}