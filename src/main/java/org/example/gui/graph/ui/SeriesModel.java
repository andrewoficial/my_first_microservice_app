package org.example.gui.graph.ui;

import lombok.Getter;
import org.apache.log4j.Logger;
import org.example.gui.graph.ChartWindow;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SeriesModel {
    private static final Logger log = Logger.getLogger(SeriesModel.class);

    @Getter
    private Map<String, JCheckBox> jBoxes = new ConcurrentHashMap<>();

    public boolean containSeries(String seriesName) {
        if (isStrInvalid(seriesName, "containSeries")) {
            return false;
        }
        return jBoxes.containsKey(seriesName);
    }

    public void addSeries(String seriesName) {
        if (isStrInvalid(seriesName, "addSeries")) {
            return;
        }

        if (containSeries(seriesName)) {
            log.error("Добавление существующего ключа: " + seriesName);
            return;
        }

        JCheckBox checkBox = new JCheckBox(seriesName, false);
        checkBox.setName(seriesName);
        checkBox.setText(seriesName);



        jBoxes.put(seriesName, checkBox);
        log.info("Добавлено серия " + seriesName);
    }

    public void removeSeries(String seriesName) {
        if (isStrInvalid(seriesName, "removeSeries")) {
            return;
        }

        if (!containSeries(seriesName)) {
            log.error("Удаление несуществующего ключа: " + seriesName);
            return;
        }
        jBoxes.remove(seriesName);
    }

    public void setVisibility(String seriesName, boolean state) {
        if (isStrInvalid(seriesName, "setVisibility")) {
            return;
        }

        JCheckBox checkBox = jBoxes.get(seriesName);
        if (checkBox == null) {
            log.error("Установка состояния несуществующего ключа: " + seriesName);
            addSeries(seriesName); // Автоматически создаст новый чекбокс
            checkBox = jBoxes.get(seriesName);
        }
        checkBox.setSelected(state);
    }

    public boolean isVisible(String seriesName) {
        if (isStrInvalid(seriesName, "isVisible")) {
            return false;
        }

        JCheckBox checkBox = jBoxes.get(seriesName);
        if (checkBox == null) {
            log.error("Получение состояния несуществующего ключа: " + seriesName);
            return false;
        }
        return checkBox.isSelected();
    }

    private boolean isStrInvalid(String seriesName, String method) {
        if (seriesName == null || seriesName.isEmpty()) {
            log.error("В метод " + method + " передана пустая или null строка");
            return true;
        }
        return false;
    }
}