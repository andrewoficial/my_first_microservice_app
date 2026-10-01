package ru.kantser.gui.gauges;

import ru.kantser.gui.theme.Theme;

/**
 * Фабрика спидометров {@link RoundGauge} с разумными значениями по умолчанию.
 */
public final class Gauges {

    private Gauges() {
    }

    /** Спидометр по готовой модели, акцент подбирается по единице измерения. */
    public static RoundGauge round(GaugeModel model) {
        RoundGauge gauge = new RoundGauge(model);
        gauge.setAccent(Theme.accentForUnit(model == null ? null : model.getUnit()));
        return gauge;
    }

    /** Спидометр с собственной моделью {@link DefaultGaugeModel}. */
    public static RoundGauge round(String label, String unit, double min, double max, double value) {
        return round(new DefaultGaugeModel(label, unit, min, max, value));
    }

    /** Спидометр со значением по нижней границе диапазона. */
    public static RoundGauge round(String label, String unit, double min, double max) {
        return round(label, unit, min, max, min);
    }
}
