package org.example.gui.mgstest.device;

import lombok.Getter;
import lombok.Setter;
import org.example.gui.mgstest.transport.CradleController;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;
import java.util.logging.Logger;

@Getter @Setter
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
    public boolean daylightSaving;  // bDaylight at [67 - compat]
    public int logTimeout;         // nLogTimeout, single byte at [68 - compat]
    public byte replaceCount;      // btReplaceCount = [43] if compat=4
    public byte enabledHW;         // btEnabledHW = [46] if nSWVer >=97
    public byte verControl;        // btVerControl =1 if nSWVer <27 else 0
    public String softwareVer;     // Formatted "%u.%02u%s"
    public String hardwareVer;     // Formatted "%u.%02u"
    public boolean loaded;         // bLoaded = true on success
    // Другие поля при необходимости (e.g., base struct fields if needed)

    // Assumed bitmasks for HW flags (adjust based on your defines)
    private static final int HW_LORA = 0x01;           // Example: bit 0
    private static final int HW_SUBSEC_TIM = 0x02;     // Example: bit 1
    private static final int HW_RTC_CLOCK_QUARZ = 0x04; // Example: bit 2
    private static final int HW_RTC_CLOCK_LSI = 0x08;  // Example: bit 3

    public static DeviceInfo parseDeviceInfo(byte[] data) {
        final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(DeviceInfo.class);
        if (data.length < 64) {
            throw new IllegalArgumentException("Data too short, expected at least 64 bytes, got " + data.length);
        }

        DeviceInfo info = new DeviceInfo();
        info.loaded = false;  // Set to true only on full success

        // CPU ID: 12 bytes from 27 to 38 inclusive (C++: i=0..11, [27+i])
        StringBuilder cpuIdBuilder = new StringBuilder();
        for (int i = 27; i <= 38; i++) {  // Fixed: <=38 for 12 bytes
            cpuIdBuilder.append(String.format("%02x", data[i] & 0xFF));  // Lowercase %02x to match expected
        }
        info.cpuId = cpuIdBuilder.toString();
        log.info("CPU ID: " + info.cpuId + " (expected: 3030470c3239383203b4fc4b)");

        // Serial number: 4 bytes LE at 39-42
        info.serialNumber = ByteBuffer.wrap(data, 39, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
        log.info("Serial number: " + info.serialNumber + " (expected: 12345677)");

        // Debugging search for serial (keep for verification)
        for (int i = 0; i < data.length - 3; i++) {  // -3 for 4-byte int
            int candidate = ByteBuffer.wrap(data, i, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            if (candidate == 12345677) {
                log.info("Found serial 12345677 at offset " + i);
            }
        }
        log.info("Serial expected at position 39");

        // Compatibility check: [43]==0x40 && [44]==0xE2 ? 0 : 4
        byte btCompatibility = 0;
        info.replaceCount = 0;  // Default
        if (data[43] == 0x40 && data[44] == (byte) 0xE2) {
            btCompatibility = 0;
        } else {
            btCompatibility = 4;
            info.replaceCount = data[43];  // btReplaceCount = [43]
        }
        log.info("Compatibility offset: " + (int) btCompatibility + ", replaceCount: " + (int) info.replaceCount);

        // SW versions: offsets 51/52 - compat
        info.swMin = data[51 - btCompatibility] & 0xFF;
        info.swMaj = data[52 - btCompatibility] & 0xFF;
        log.info("Firmware raw: " + info.swMaj + "." + info.swMin + " (expected: 2.14)");

        // HW versions: offsets 53/54 - compat
        info.hwMin = data[53 - btCompatibility] & 0xFF;
        info.hwMaj = data[54 - btCompatibility] & 0xFF;
        log.info("Hardware raw: " + info.hwMaj + "." + info.hwMin + " (expected: 3.03)");

        // Compute nSWVer (assumed: maj*100 + min, as used in C++ for conditions)
        int nSWVer = info.swMaj * 100 + info.swMin;
        log.info("Computed nSWVer: " + nSWVer);

        // Ver control: 1 if nSWVer <27 else 0
        info.verControl = (byte) (nSWVer < 27 ? 1 : 0);
        log.info("Version control: " + (int) info.verControl);

        // HW enabled: if nSWVer >=97, read [46]
        StringBuilder strHW = new StringBuilder(" ");
        info.enabledHW = 0;
        if (nSWVer >= 97) {
            info.enabledHW = data[46];  // Raw byte
            if ((info.enabledHW & HW_LORA) != 0) strHW.append("L");
            else strHW.append("N");
            if ((info.enabledHW & HW_SUBSEC_TIM) != 0) strHW.append("T");
            else strHW.append("R");
            if ((info.enabledHW & HW_RTC_CLOCK_QUARZ) != 0) strHW.append("Q");
            else if ((info.enabledHW & HW_RTC_CLOCK_LSI) != 0) strHW.append("I");
            else strHW.append("G");
        }
        log.info("Enabled HW: " + (int) info.enabledHW + ", strHW: " + strHW);

        // Format versions (matching C++)
        info.softwareVer = String.format("%d.%02d%s", info.swMaj, info.swMin, strHW.toString());
        info.hardwareVer = String.format("%d.%02d", info.hwMaj, info.hwMin);
        log.info("Software ver formatted: " + info.softwareVer);
        log.info("Hardware ver formatted: " + info.hardwareVer);

        // Time: 4 bytes LE at 55 - compat
        int rawTime = ByteBuffer.wrap(data, 55 - btCompatibility, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
        long timeValue = rawTime & 0xFFFFFFFFL;  // Unsigned int as long
        info.time = (int) timeValue;  // Or keep as long if >2^31
        log.info("Time (raw unsigned): " + timeValue);
        if (timeValue == 0) {
            log.warn("Invalid time value (0), parsing failed");
            return null;  // Match C++ return 0
        }
        if (timeValue > 0 && timeValue < 0xFFFFFFFFL) {
            Date date = new Date(timeValue * 1000L);  // Assuming Unix seconds to ms
            log.info("Formatted time: " + date.toString());
        }

        // Validate bytes 59-61 - compat: must be 0 or 1 each (fixed C++ bug: check [i - compat])
        for (int i = 59; i <= 61; i++) {
            byte val = data[i - btCompatibility];
            if (val != 0 && val != 1) {
                log.warn("Invalid byte at offset " + (i - btCompatibility) + ": " + (int) val + " (must be 0 or 1)");
                return null;  // Match C++ return 0
            }
        }

        // Beep/Vibro/Alarm: ! [59/60/61 - compat] (0=true enabled)
        info.beepEnabled = (data[59 - btCompatibility] & 0xFF) == 0;  // !byte (0 -> true)
        info.vibroEnabled = (data[60 - btCompatibility] & 0xFF) == 0;
        info.alarmEnabled = (data[61 - btCompatibility] & 0xFF) == 0;
        log.info("Beep enabled: " + info.beepEnabled);
        log.info("Vibro enabled: " + info.vibroEnabled);
        log.info("Alarm enabled: " + info.alarmEnabled);

        // Daylight saving: [67 - compat] (assuming !=0 true, like others)
        info.daylightSaving = (data[67 - btCompatibility] & 0xFF) != 0;
        log.info("Daylight saving: " + info.daylightSaving);

        // Log timeout: single byte at [68 - compat]
        info.logTimeout = data[68 - btCompatibility] & 0xFF;
        log.info("Log timeout: " + info.logTimeout);

        info.loaded = true;  // Success
        return info;
    }
}