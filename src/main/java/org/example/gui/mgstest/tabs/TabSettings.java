package org.example.gui.mgstest.tabs;

import org.example.gui.mgstest.device.DeviceInfo;
import org.example.gui.mgstest.pool.DeviceState;

import javax.swing.*;
import java.awt.*;

public class TabSettings extends DeviceTab{

    private JLabel cpuIdLabel;
    private JLabel serialNumberLabel;
    // ... другие поля

    public TabSettings() {
        super("Настройки");
        initComponents();
    }

    private void initComponents() {
        panel.setLayout(new GridLayout(0, 2, 5, 5));

        panel.add(new JLabel("CPU ID:"));
        cpuIdLabel = new JLabel();
        panel.add(cpuIdLabel);

        panel.add(new JLabel("Серийный номер:"));
        serialNumberLabel = new JLabel();
        panel.add(serialNumberLabel);

        // ... остальные компоненты
    }

    @Override
    public void updateData(DeviceState state) {
        if (state != null && state.getDeviceInfo() != null) {
            DeviceInfo info = state.getDeviceInfo();
            cpuIdLabel.setText(info.getCpuId());
            serialNumberLabel.setText(String.valueOf(info.getSerialNumber()));
            // ... обновление других полей
        } else {
            cpuIdLabel.setText("Нет данных");
            serialNumberLabel.setText("Нет данных");
            // ... сброс других полей
        }
    }

    @Override
    public void saveData(DeviceState state) {
        // Для информационной вкладки обычно не требуется сохранение
    }
}
