package org.example.gui.devices.testa.emulation;

/**
 * Модель термодинамики климатической камеры Testa (для эмулятора).
 * <p>
 * Пока {@code running} — фактическая температура идёт к уставке со скоростью
 * {@code rampRateDegPerMin}. Когда камера остановлена — температура медленно дрейфует
 * к фиксированным 25 °C (своя малая скорость, не скорость выхода).
 * Влажность ведётся аналогично: текущая плавно стремится к заданной.
 * Все значения потокобезопасны.
 */
public class TestaEmulator {

    /** Целевая температура при остановленной камере. */
    private static final double STOP_TEMP_DEG = 25.0;
    /** Медленная скорость дрейфа к 25 при остановке, °C/мин. */
    private static final double STOP_DRIFT_RATE = 0.5;

    private volatile double setpoint = 25.0;
    private volatile double actual = 25.0;
    private volatile double rampRateDegPerMin = 5.0;
    private volatile double minDeg = -40.0;
    private volatile double maxDeg = 120.0;

    private volatile boolean running = true;

    private volatile double humidityCurrent = Double.NaN;
    private volatile double humiditySet = 0.0;
    private volatile double humidityRateDegPerMin = 2.0;

    private volatile boolean manualFrameEnabled = false;
    private volatile byte[] manualFrame = new byte[40];

    private volatile byte[] rawStatusBytes = new byte[0];

    /** Тик симуляции: двигает температуру (к уставке или к 25) и влажность к уставке. */
    public synchronized void advance(double dtSec) {
        if (dtSec <= 0) {
            return;
        }
        double tempTarget = running ? setpoint : STOP_TEMP_DEG;
        double tempRate = running ? rampRateDegPerMin : STOP_DRIFT_RATE;
        actual = clamp(move(actual, tempTarget, tempRate, dtSec));

        if (Double.isNaN(humidityCurrent)) {
            humidityCurrent = humiditySet;
        }
        humidityCurrent = clampHum(move(humidityCurrent, humiditySet, humidityRateDegPerMin, dtSec));
    }

    private static double move(double cur, double target, double ratePerMin, double dtSec) {
        if (Math.abs(cur - target) < 0.001) {
            return target;
        }
        double step = (ratePerMin / 60.0) * dtSec;
        double d = Math.min(step, Math.abs(target - cur)) * Math.signum(target - cur);
        return cur + d;
    }

    public synchronized void setSetpoint(double t) {
        setpoint = clamp(t);
    }

    public synchronized double getSetpoint() { return setpoint; }
    public synchronized double getActual() { return actual; }

    public void setRampRateDegPerMin(double v) { if (v > 0) this.rampRateDegPerMin = v; }
    public double getRampRateDegPerMin() { return rampRateDegPerMin; }
    public void setRange(double min, double max) { this.minDeg = min; this.maxDeg = max; }

    public void setRunning(boolean r) {
        this.running = r;
    }

    public boolean isRunning() {
        return running;
    }

    public synchronized void setHumiditySetpoint(double rh) {
        this.humiditySet = clampHum(rh);
    }

    public synchronized double getHumiditySet() {
        return humiditySet;
    }

    public synchronized double getHumidityCurrent() {
        return Double.isNaN(humidityCurrent) ? humiditySet : humidityCurrent;
    }

    public void setHumidityRateDegPerMin(double v) {
        if (v > 0) {
            this.humidityRateDegPerMin = v;
        }
    }

    public double getHumidityRateDegPerMin() {
        return humidityRateDegPerMin;
    }

    public void setManualFrameEnabled(boolean on) {
        this.manualFrameEnabled = on;
    }

    public boolean isManualFrameEnabled() {
        return manualFrameEnabled;
    }

    public void setManualFrame(byte[] frame) {
        this.manualFrame = (frame == null) ? new byte[40] : frame.clone();
    }

    public byte[] getManualFrame() {
        return manualFrame.clone();
    }

    /** Произвольные «сырые» байты 8..39 статусной датаграммы (для отладки протокола). */
    public void setRawStatusBytes(byte[] raw) {
        this.rawStatusBytes = (raw == null) ? new byte[0] : raw.clone();
    }

    public byte[] getRawStatusBytes() {
        return rawStatusBytes.clone();
    }

    /** Ставит/снимает бит подсветки (байт 21 статуса, бит 7) в текущем сыром буфере. */
    public void setLight(boolean on) {
        byte[] raw = rawStatusBytes.length == 32 ? rawStatusBytes.clone() : new byte[32];
        int idx = 21 - 8;
        if (on) {
            raw[idx] = (byte) (raw[idx] | 0x80);
        } else {
            raw[idx] = (byte) (raw[idx] & ~0x80);
        }
        rawStatusBytes = raw;
    }

    private double clamp(double v) {
        return Math.max(minDeg, Math.min(maxDeg, v));
    }

    private static double clampHum(double v) {
        if (Double.isNaN(v)) {
            return 0;
        }
        return Math.max(0, Math.min(100, v));
    }
}
