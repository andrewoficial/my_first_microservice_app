package org.example.gui.devices.testa.emulation;

/**
 * Модель термодинамики климатической камеры Testa (для эмулятора).
 * <p>Фактическая температура сходится к уставке со скоростью {@code rampRateDegPerMin}.
 * Значения потокобезопасны.
 */
public class TestaEmulator {

    private volatile double setpoint = 25.0;
    private volatile double actual = 25.0;
    private volatile double rampRateDegPerMin = 5.0;
    private volatile double minDeg = -40.0;
    private volatile double maxDeg = 120.0;

    private volatile byte[] rawStatusBytes = new byte[0];

    private volatile boolean manualFrameEnabled = false;
    private volatile byte[] manualFrame = new byte[40];

    /** Тик симуляции: двигает фактическую температуру к уставке. */
    public synchronized void advance(double dtSec) {
        if (dtSec <= 0) {
            return;
        }
        double diff = setpoint - actual;
        if (Math.abs(diff) < 0.01) {
            actual = setpoint;
            return;
        }
        double step = (rampRateDegPerMin / 60.0) * dtSec;
        actual = Math.min(step, Math.abs(diff)) * Math.signum(diff) + actual;
        actual = clamp(actual);
    }

    public synchronized void setSetpoint(double t) {
        setpoint = clamp(t);
    }

    public synchronized double getSetpoint() { return setpoint; }
    public synchronized double getActual() { return actual; }

    public void setRampRateDegPerMin(double v) { if (v > 0) this.rampRateDegPerMin = v; }
    public double getRampRateDegPerMin() { return rampRateDegPerMin; }
    public void setRange(double min, double max) { this.minDeg = min; this.maxDeg = max; }

    /** Произвольные «сырые» байты 8..39 статусной датаграммы (для отладки протокола). */
    public void setRawStatusBytes(byte[] raw) {
        this.rawStatusBytes = (raw == null) ? new byte[0] : raw.clone();
    }

    public byte[] getRawStatusBytes() {
        return rawStatusBytes.clone();
    }

    /** Включить «ручной кадр»: статус уходит байт-в-байт из {@code manualFrame}, без автоперезаписи. */
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

    private double clamp(double v) {
        return Math.max(minDeg, Math.min(maxDeg, v));
    }
}
