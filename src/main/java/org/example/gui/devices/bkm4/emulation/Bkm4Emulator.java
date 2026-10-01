package org.example.gui.devices.bkm4.emulation;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Модель блока коммутации БКМ-4 (имитация).
 * Фактический расход возникает только при открытом клапане и включённой
 * генерации и выходит на уставку с инерцией, как реальный прибор.
 */
public class Bkm4Emulator {

    public static final int MAX_SETPOINT = 3000;

    private static final double RESPONSE_TAU_SECONDS = 8.0;

    private int mode = 0;
    private int valve = 0;
    private double setpointMlMin = 0;
    private int generation = 0;

    private double currentFlowMlMin = 0;
    private double noisePhase = 0;
    private double noiseAmp = 1.5;
    private double noisePeriodMs = 1500;

    private final List<Runnable> stateListeners = new CopyOnWriteArrayList<>();

    public synchronized int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        int v = (mode == 1) ? 1 : 0;
        synchronized (this) {
            if (this.mode == v) {
                return;
            }
            this.mode = v;
        }
        fireStateChanged();
    }

    public synchronized int getValve() {
        return valve;
    }

    public void setValve(int valve) {
        int v = valve < 0 ? 0 : Math.min(valve, 4);
        synchronized (this) {
            if (this.valve == v) {
                return;
            }
            this.valve = v;
        }
        fireStateChanged();
    }

    public synchronized double getSetpointMlMin() {
        return setpointMlMin;
    }

    public void setSetpointMlMin(double setpointMlMin) {
        double v = Math.max(0, Math.min(MAX_SETPOINT, setpointMlMin));
        synchronized (this) {
            if (Double.compare(this.setpointMlMin, v) == 0) {
                return;
            }
            this.setpointMlMin = v;
        }
        fireStateChanged();
    }

    public synchronized int getGeneration() {
        return generation;
    }

    public void setGeneration(int generation) {
        int v = (generation == 1) ? 1 : 0;
        synchronized (this) {
            if (this.generation == v) {
                return;
            }
            this.generation = v;
        }
        fireStateChanged();
    }

    public synchronized double getCurrentFlowMlMin() {
        return Math.max(0, currentFlowMlMin + Math.sin(noisePhase) * noiseAmp);
    }

    public synchronized void advance(double dtSeconds) {
        if (dtSeconds <= 0) {
            return;
        }
        noisePhase += (dtSeconds * 1000.0 / noisePeriodMs) * 2.0 * Math.PI;
        if (noisePhase > 2 * Math.PI) {
            noisePhase -= 2 * Math.PI * Math.floor(noisePhase / (2 * Math.PI));
        }
        double target = targetFlow();
        double alpha = 1.0 - Math.exp(-dtSeconds / RESPONSE_TAU_SECONDS);
        currentFlowMlMin += (target - currentFlowMlMin) * alpha;
    }

    private double targetFlow() {
        if (generation != 1 || valve <= 0 || setpointMlMin <= 0) {
            return 0;
        }
        return setpointMlMin;
    }

    public synchronized void setFluctuationAmp(double amp) {
        this.noiseAmp = Math.max(0, amp);
    }

    public void addStateListener(Runnable listener) {
        stateListeners.add(listener);
    }

    public void removeStateListener(Runnable listener) {
        stateListeners.remove(listener);
    }

    private void fireStateChanged() {
        for (Runnable listener : stateListeners) {
            try {
                listener.run();
            } catch (Exception ignored) {
            }
        }
    }
}
