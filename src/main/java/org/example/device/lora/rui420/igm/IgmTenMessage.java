package org.example.device.lora.rui420.igm;

import org.apache.log4j.Logger;

import java.util.Optional;

public class IgmTenMessage {  // Renamed from IgmTenParser for consistency with message parsing pattern
    private static final Logger log = Logger.getLogger(IgmTenMessage.class);

    private int temperature; // int °C * 10
    private int gasType; // 1..32, e.g. 1=CH4, 2=C3H8
    private int concentration; // word, value * precision (for CH4 0.01%vol)
    private int unit; // 0/1/3/4 %vol / ppm / %LEL / g/cm3
    private boolean errMem; // 0/1 No / EEPROM write error
    private boolean imit; // 0/1 No / Imitation mode
    private boolean limit2; // 0/1 No / Threshold 2 exceeded
    private boolean limit1; // 0/1 No / Threshold 1 exceeded
    private boolean special; // 0/1 No / Special mode
    private boolean errDat; // 0/1 OK / Optical sensor data error
    private boolean start; // 0/1 OK / Warming up
    private boolean errSnr; // 0/1 OK / Optical sensor comm error
    private boolean errOpt; // 0/1 OK / Optical sensor optics error
    private boolean range; // 0/1 No / Range exceeded
    private boolean err; // 0/1 Faulty / Working
    private boolean errRel2; // 0/1 OK / Relay 1 error
    private boolean errRel1; // 0/1 OK / Relay 2 error
    private boolean magnet; // 0/1 No / Magnet detected
    private boolean errPwr; // 0/1 OK / Low power
    private boolean locked; // 0/1 No / Readings locked
    private boolean manual; // 0/1 Auto / Manual unblock
    private boolean fixCo; // 0/1 No / Current output fixed

    private IgmTenMessage(int temperature, int gasType, int concentration, int unit,
                          boolean errMem, boolean imit, boolean limit2, boolean limit1, boolean special,
                          boolean errDat, boolean start, boolean errSnr, boolean errOpt, boolean range,
                          boolean err, boolean errRel2, boolean errRel1, boolean magnet, boolean errPwr,
                          boolean locked, boolean manual, boolean fixCo) {
        this.temperature = temperature;
        this.gasType = gasType;
        this.concentration = concentration;
        this.unit = unit;
        this.errMem = errMem;
        this.imit = imit;
        this.limit2 = limit2;
        this.limit1 = limit1;
        this.special = special;
        this.errDat = errDat;
        this.start = start;
        this.errSnr = errSnr;
        this.errOpt = errOpt;
        this.range = range;
        this.err = err;
        this.errRel2 = errRel2;
        this.errRel1 = errRel1;
        this.magnet = magnet;
        this.errPwr = errPwr;
        this.locked = locked;
        this.manual = manual;
        this.fixCo = fixCo;
    }

    public static Optional<IgmTenMessage> parse(byte[] msg) {
        if (msg == null || msg.length < 8) { // Assume minimal size
            log.error("Invalid IGM10 payload: too short");
            return Optional.empty();
        }

        try {
            int offset = 0;

            int temperature = (msg[offset + 1] << 8) | (msg[offset] & 0xFF); // little-endian, signed
            if ((temperature & 0x8000) != 0) temperature -= 0x10000;
            offset += 2;

            int gasType = msg[offset++] & 0xFF;

            int concentration = ((msg[offset + 1] & 0xFF) << 8) | (msg[offset] & 0xFF);
            offset += 2;

            int unit = msg[offset++] & 0xFF;

            if (msg.length < offset + 3) {
                throw new IllegalArgumentException("Payload too short for flags");
            }

            int flagBits = ((msg[offset + 2] & 0xFF) << 16) | ((msg[offset + 1] & 0xFF) << 8) | (msg[offset] & 0xFF);

            boolean errMem = ((flagBits >> 0) & 1) == 1;
            boolean imit = ((flagBits >> 1) & 1) == 1;
            boolean limit2 = ((flagBits >> 2) & 1) == 1;
            boolean limit1 = ((flagBits >> 3) & 1) == 1;
            boolean special = ((flagBits >> 4) & 1) == 1;
            boolean errDat = ((flagBits >> 5) & 1) == 1;
            boolean start = ((flagBits >> 6) & 1) == 1;
            boolean errSnr = ((flagBits >> 7) & 1) == 1;
            boolean errOpt = ((flagBits >> 8) & 1) == 1;
            boolean range = ((flagBits >> 9) & 1) == 1;
            boolean err = ((flagBits >> 10) & 1) == 1;
            boolean errRel2 = ((flagBits >> 11) & 1) == 1;
            boolean errRel1 = ((flagBits >> 12) & 1) == 1;
            boolean magnet = ((flagBits >> 13) & 1) == 1;
            boolean errPwr = ((flagBits >> 14) & 1) == 1;
            boolean locked = ((flagBits >> 15) & 1) == 1;
            boolean manual = ((flagBits >> 16) & 1) == 1;
            boolean fixCo = ((flagBits >> 17) & 1) == 1;

            log.info("Parsed IGM10: Temp=" + (temperature / 10.0) + "°C, GasType=" + gasType + ", Conc=" + concentration);
            return Optional.of(new IgmTenMessage(temperature, gasType, concentration, unit,
                    errMem, imit, limit2, limit1, special, errDat, start, errSnr, errOpt, range,
                    err, errRel2, errRel1, magnet, errPwr, locked, manual, fixCo));
        } catch (Exception e) {
            log.error("Error parsing IGM10 payload", e);
            return Optional.empty();
        }
    }

    // Getters (as before)

    public double getTemperature() {
        return temperature / 10.0;
    }

    public int getGasType() {
        return gasType;
    }

    public int getConcentration() {
        return concentration;
    }

    public int getUnit() {
        return unit;
    }

    public boolean isErrMem() {
        return errMem;
    }

    public boolean isImit() {
        return imit;
    }

    public boolean isLimit2() {
        return limit2;
    }

    public boolean isLimit1() {
        return limit1;
    }

    public boolean isSpecial() {
        return special;
    }

    public boolean isErrDat() {
        return errDat;
    }

    public boolean isStart() {
        return start;
    }

    public boolean isErrSnr() {
        return errSnr;
    }

    public boolean isErrOpt() {
        return errOpt;
    }

    public boolean isRange() {
        return range;
    }

    public boolean isErr() {
        return err;
    }

    public boolean isErrRel2() {
        return errRel2;
    }

    public boolean isErrRel1() {
        return errRel1;
    }

    public boolean isMagnet() {
        return magnet;
    }

    public boolean isErrPwr() {
        return errPwr;
    }

    public boolean isLocked() {
        return locked;
    }

    public boolean isManual() {
        return manual;
    }

    public boolean isFixCo() {
        return fixCo;
    }
}