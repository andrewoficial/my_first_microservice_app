package org.example.gui.devices.bkm4.emulation;

/**
 * Модель блока коммутации БКМ-4 (имитация).
 * <p>
 * Текущий (фактический) расход равен уставке с небольшой флуктуацией,
 * например уставка 500 → 499.9…500.1 мл/мин. При выключенной генерации
 * ({@code generation == 0}) фактический расход = 0.
 */
public class Bkm4Emulator {

    /** Режим: 0 — ручное, 1 — внешнее. */
    private volatile int mode = 0;

    /** Газовый клапан: 0 — все выключены, 1..4 — включённый. */
    private volatile int valve = 0;

    /** Уставка расхода, мл/мин (0..3000). */
    private volatile double setpointMlMin = 0;

    /** Генерация потока: 0 — выкл, 1 — вкл. */
    private volatile int generation = 0;

    private volatile double noisePhase = 0;
    private volatile double noiseAmp = 0.2;
    private volatile double noisePeriodMs = 1500;

    public synchronized int getMode() {
        return mode;
    }

    public synchronized void setMode(int mode) {
        this.mode = (mode == 1) ? 1 : 0;
    }

    public synchronized int getValve() {
        return valve;
    }

    public synchronized void setValve(int valve) {
        this.valve = valve < 0 ? 0 : Math.min(valve, 4);
    }

    public synchronized double getSetpointMlMin() {
        return setpointMlMin;
    }

    public synchronized void setSetpointMlMin(double setpointMlMin) {
        this.setpointMlMin = Math.max(0, Math.min(3000, setpointMlMin));
    }

    public synchronized int getGeneration() {
        return generation;
    }

    public synchronized void setGeneration(int generation) {
        this.generation = (generation == 1) ? 1 : 0;
    }

    /**
     * Фактический расход. Если генерация выключена или уставка = 0 — вернёт 0.
     */
    public synchronized double getCurrentFlowMlMin() {
        if (generation != 1 || setpointMlMin <= 0) {
            return 0;
        }
        return setpointMlMin + Math.sin(noisePhase) * noiseAmp;
    }

    /**
     * Продвижение имитации (флуктуация). Вызывает пол вызова из таймера панели.
     */
    public synchronized void advance(double dtSeconds) {
        noisePhase += (dtSeconds * 1000.0 / noisePeriodMs) * 2.0 * Math.PI;
    }

    public synchronized void setFluctuationAmp(double amp) {
        this.noiseAmp = Math.max(0, amp);
    }
}