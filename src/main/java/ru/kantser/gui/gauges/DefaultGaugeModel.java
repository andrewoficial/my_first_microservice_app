package ru.kantser.gui.gauges;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Реализация {@link GaugeModel} по умолчанию.
 *
 * <p>{@link #setValue(double)} зажимает значение в диапазон {@code [min, max]}
 * и оповещает слушателей. Класс не зависит от EDT: уведомления идут обычным
 * listener-механизмом, а вызывающий сам решает, нужен ли {@code invokeLater}.
 */
public class DefaultGaugeModel implements GaugeModel {

    private final CopyOnWriteArrayList<ChangeListener> listeners = new CopyOnWriteArrayList<>();

    private final String label;
    private final String unit;

    private volatile double min;
    private volatile double max;
    private volatile double value;

    public DefaultGaugeModel(String label, String unit, double min, double max, double value) {
        this.label = label == null ? "" : label;
        this.unit = unit == null ? "" : unit;
        if (max <= min) {
            max = min + 1.0;
        }
        this.min = min;
        this.max = max;
        this.value = clamp(value);
    }

    @Override
    public double getValue() {
        return value;
    }

    @Override
    public void setValue(double newValue) {
        double clamped = clamp(newValue);
        if (clamped == value) {
            return;
        }
        value = clamped;
        fireChanged();
    }

    @Override
    public double getMin() {
        return min;
    }

    @Override
    public double getMax() {
        return max;
    }

    @Override
    public void setRange(double newMin, double newMax) {
        if (newMax <= newMin) {
            newMax = newMin + 1.0;
        }
        double clamped = Math.max(newMin, Math.min(newMax, value));
        boolean rangeChanged = newMin != min || newMax != max;
        boolean valueChanged = clamped != value;
        min = newMin;
        max = newMax;
        value = clamped;
        if (rangeChanged || valueChanged) {
            fireChanged();
        }
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public String getUnit() {
        return unit;
    }

    @Override
    public void addChangeListener(ChangeListener listener) {
        if (listener != null) {
            listeners.addIfAbsent(listener);
        }
    }

    @Override
    public void removeChangeListener(ChangeListener listener) {
        listeners.remove(listener);
    }

    private double clamp(double v) {
        if (v < min) {
            return min;
        }
        if (v > max) {
            return max;
        }
        return v;
    }

    private void fireChanged() {
        for (ChangeListener listener : listeners) {
            listener.gaugeChanged(this);
        }
    }
}
