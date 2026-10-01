package ru.kantser.gui.gauges;

/**
 * Чистая модель спидометра: значение, диапазон, подпись, единица и слушатели.
 *
 * <p>Интерфейс намеренно не зависит от Swing/AWT — это позволяет использовать
 * модель в любом контексте и без графики.
 */
public interface GaugeModel {

    double getValue();

    void setValue(double value);

    double getMin();

    double getMax();

    /**
     * Установить новый диапазон. Значение должно быть зажато в новые границы.
     */
    void setRange(double min, double max);

    String getLabel();

    String getUnit();

    void addChangeListener(ChangeListener listener);

    void removeChangeListener(ChangeListener listener);

    /**
     * Слушатель изменений модели. Реализация сама решает, нужен ли
     * {@code SwingUtilities.invokeLater} — модель об EDT ничего не знает.
     */
    @FunctionalInterface
    interface ChangeListener {
        void gaugeChanged(GaugeModel model);
    }
}
