package org.example.gui.devices.binder.camera.emulation;

/**
 * Модель термодинамики климатической камеры Binder (для эмулятора).
 *
 * <p>Фактическая температура сходится к уставке ({@code setpoint}) с настраиваемым
 * поведением выхода на режим:
 * <ul>
 *   <li><b>rampRateDegPerMin</b> — скорость выхода на уставку, °C/мин;</li>
 *   <li><b>overshootDeg</b> — размер «перелёта»: камера сначала пересекает заданное
 *       значение, затем возвращается к нему;</li>
 *   <li>на полке (после стабилизации) добавляется флуктуация:
 *       <b>fluctuationAmpDeg</b> (амплитуда, °C) и <b>fluctuationPeriodSec</b>
 *       (период колебаний, с — «скорость флуктуации»).</li>
 * </ul>
 *
 * <p>Значения потокобезопасны (volatile/synchronized): тик симуляции вызывается из таймера,
 * уставка может меняться из обработчика TCP-кадра.
 */
public class BinderEmulator {

    private static final double EPS = 0.02;

    private volatile double setpoint = 25.0;
    private volatile double actual = 25.0;
    private volatile boolean humidityControlOn = false;

    // настройки динамики
    private volatile double rampRateDegPerMin = 5.0;
    private volatile double overshootDeg = 2.0;
    private volatile double fluctuationAmpDeg = 0.3;
    private volatile double fluctuationPeriodSec = 20.0;

    private volatile double minDeg = -40.0;
    private volatile double maxDeg = 120.0;

    // внутреннее состояние прохода (двухступенчатое: перелёт -> возврат)
    private transient double stageTarget = Double.NaN;
    private transient boolean stageFirst = false;
    private transient double ripplePhase = 0.0;

    /** Тик симуляции: сдвигает фактическую температуру на dt (сек). */
    public synchronized void advance(double dtSec) {
        if (dtSec <= 0) {
            return;
        }
        double dir = Double.compare(setpoint, actual);
        double dist = Math.abs(setpoint - actual);

        if (dist <= EPS) {
            actual = setpoint;
            stageTarget = Double.NaN;
        } else if (Double.isNaN(stageTarget) || Math.signum(stageTarget - actual) != dir) {
            // начали новый переход к уставке
            beginApproach(dir, dist);
        }

        if (!Double.isNaN(stageTarget)) {
            double maxStep = (rampRateDegPerMin / 60.0) * dtSec;
            double step = Math.signum(stageTarget - actual) * Math.min(maxStep, Math.abs(stageTarget - actual));
            actual += step;
            if (Math.abs(stageTarget - actual) <= EPS) {
                actual = stageTarget;
                if (stageFirst) {
                    // достигли перелёта — возвращаемся к уставке
                    stageFirst = false;
                    stageTarget = setpoint;
                } else {
                    stageTarget = Double.NaN;
                    actual = setpoint;
                }
            }
        }

        actual = clamp(actual);
        ripplePhase = (ripplePhase + 2 * Math.PI * dtSec / fluctuationPeriodSec) % (2 * Math.PI);
    }

    private void beginApproach(double dir, double dist) {
        if (overshootDeg <= EPS) {
            stageFirst = false;
            stageTarget = setpoint;
            return;
        }
        stageFirst = true;
        double overshootLimit = clamp(setpoint + dir * overshootDeg);
        stageTarget = overshootLimit;
    }

    /** Текущая «измеренная» температура (базовая + флуктуация на полке). */
    public synchronized double getMeasuredTemperature() {
        double ripple = fluctuationAmpDeg * Math.sin(ripplePhase);
        return clamp(actual + ripple);
    }

    // ─── сеттеры от кадров Modbus ─────────────────────────────────────────

    /** Применить уставку из кадра SetT. */
    public synchronized void setSetpoint(float t) {
        setpoint = clamp(t);
        // новый целевой переход пересчитается на ближайшем тике
        stageTarget = Double.NaN;
    }

    public synchronized void setHumidityControl(boolean on) {
        humidityControlOn = on;
    }

    // ─── настройки динамики ───────────────────────────────────────────────

    public void setRampRateDegPerMin(double v) { if (v > 0) this.rampRateDegPerMin = v; }
    public void setOvershootDeg(double v) { this.overshootDeg = Math.max(0, v); }
    public void setFluctuationAmpDeg(double v) { this.fluctuationAmpDeg = Math.max(0, v); }
    public void setFluctuationPeriodSec(double v) { if (v > 0) this.fluctuationPeriodSec = v; }
    public void setRange(double min, double max) {
        this.minDeg = min;
        this.maxDeg = max;
    }

    public double getRampRateDegPerMin() { return rampRateDegPerMin; }
    public double getOvershootDeg() { return overshootDeg; }
    public double getFluctuationAmpDeg() { return fluctuationAmpDeg; }
    public double getFluctuationPeriodSec() { return fluctuationPeriodSec; }

    // ─── чтение состояния ─────────────────────────────────────────────────

    public synchronized double getSetpoint() { return setpoint; }
    public synchronized boolean isHumidityControlOn() { return humidityControlOn; }

    private double clamp(double v) {
        return Math.max(minDeg, Math.min(maxDeg, v));
    }
}
