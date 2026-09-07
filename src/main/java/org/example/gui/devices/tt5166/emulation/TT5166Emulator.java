package org.example.gui.devices.tt5166.emulation;

/**
 * Модель термокамеры TT5166 (эмуляция).
 * <p>
 * Температура и влажность плавно выходят на свои уставки (ramp).
 * Масштаб регистров: темп. PV — ×100, остальные поля — ×10 (см. TT5166CommandRegistry).
 */
public class TT5166Emulator {

    private static final double ROOM_TEMP_C = 20.0;
    private static final double ROOM_HUM_PCT = 40.0;
    private static final double OFF_DRIFT_C_PER_SEC = 0.2 / 60.0;
    private static final double OFF_HUM_PER_SEC = 0.2 / 60.0;

    private volatile double currentTempC = 20.0;
    private volatile double setpointC = 25.0;
    private volatile boolean on = false;
    private volatile double rampRateCPerSec = 2.0;

    private volatile double currentHumidity = 40.0;
    private volatile double humiditySetpoint = 50.0;
    private volatile double humidityRampPerSec = 1.0;

    private volatile boolean programMode = false;
    private volatile int faultCode = 0;

    private volatile double tempOutputPct = 0.0;
    private volatile double humOutputPct = 0.0;

    private volatile double ch2TempC = 20.0;
    private volatile double ch3TempC = 20.0;
    private volatile double ch4TempC = 20.0;

    private volatile int programHours = 0;
    private volatile int programMinutes = 0;

    private volatile double tempGradientCPer10Min = 1.0; // рег 0x0064 ×10
    private volatile double humGradientPctPer10Min = 1.0; // рег 0x0065 ×10

    public synchronized void advance(double dtSeconds) {
        double d = Math.min(dtSeconds, 0.5);
        if (on) {
            currentTempC = rampTowards(currentTempC, setpointC, rampRateCPerSec, d);
            currentHumidity = clamp(rampTowards(currentHumidity, humiditySetpoint,
                    humidityRampPerSec, d), 0, 100);
            tempOutputPct = clamp(Math.abs(setpointC - currentTempC) / 10.0 * 100.0, 0, 100);
            humOutputPct = clamp(Math.abs(humiditySetpoint - currentHumidity) / 10.0 * 100.0, 0, 100);
        } else {
            currentTempC = clamp(rampTowards(currentTempC, ROOM_TEMP_C, OFF_DRIFT_C_PER_SEC, d), 0, 400);
            currentHumidity = clamp(rampTowards(currentHumidity, ROOM_HUM_PCT, OFF_HUM_PER_SEC, d), 0, 100);
            tempOutputPct = 0;
            humOutputPct = 0;
        }
    }

    private static double rampTowards(double cur, double target, double rate, double dt) {
        double diff = target - cur;
        if (Math.abs(diff) < rate * dt) return target;
        return cur + Math.signum(diff) * rate * dt;
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    public synchronized double getCurrentTempC() { return currentTempC; }
    public synchronized double getSetpointC() { return setpointC; }
    public synchronized double getCurrentHumidity() { return currentHumidity; }
    public synchronized double getHumiditySetpoint() { return humiditySetpoint; }
    public synchronized double getTempOutputPct() { return tempOutputPct; }
    public synchronized double getHumOutputPct() { return humOutputPct; }
    public synchronized boolean isOn() { return on; }
    public synchronized boolean isProgramMode() { return programMode; }
    public synchronized int getFaultCode() { return faultCode; }
    public synchronized double getRampRateCPerSec() { return rampRateCPerSec; }

    public synchronized void setCurrentTempC(double t) { currentTempC = t; }
    public synchronized void setSetpointC(double t) { setpointC = Math.max(-100, Math.min(200, t)); }
    public synchronized void setCurrentHumidity(double h) { currentHumidity = clamp(h, 0, 100); }
    public synchronized void setHumiditySetpoint(double h) { humiditySetpoint = clamp(h, 0, 100); }
    public synchronized void setTempOutputPct(double v) { tempOutputPct = clamp(v, 0, 100); }
    public synchronized void setHumOutputPct(double v) { humOutputPct = clamp(v, 0, 100); }
    public synchronized void setOn(boolean on) { this.on = on; }
    public synchronized void setProgramMode(boolean programMode) { this.programMode = programMode; }
    public synchronized void setFaultCode(int v) { faultCode = Math.max(0, Math.min(0xFFFF, v)); }
    public synchronized void setRampRateCPerSec(double rate) { this.rampRateCPerSec = Math.max(0.1, rate); }

    public synchronized double getChannelTempC(int idx) {
        switch (idx) {
            case 2: return ch2TempC;
            case 3: return ch3TempC;
            default: return ch4TempC;
        }
    }
    public synchronized void setChannelTempC(int idx, double v) {
        switch (idx) {
            case 2: ch2TempC = v; break;
            case 3: ch3TempC = v; break;
            default: ch4TempC = v; break;
        }
    }

    public synchronized int getProgramHours() { return programHours; }
    public synchronized int getProgramMinutes() { return programMinutes; }
    public synchronized void setProgramTime(int h, int m) {
        programHours = Math.max(0, h);
        programMinutes = Math.max(0, Math.min(59, m));
    }

    public synchronized double getTempGradientCPer10Min() { return tempGradientCPer10Min; }
    public synchronized void setTempGradientCPer10Min(double v) { tempGradientCPer10Min = Math.max(0, v); }
    public synchronized double getHumGradientPctPer10Min() { return humGradientPctPer10Min; }
    public synchronized void setHumGradientPctPer10Min(double v) { humGradientPctPer10Min = Math.max(0, v); }
}