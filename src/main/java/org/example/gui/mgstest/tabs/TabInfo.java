package org.example.gui.mgstest.tabs;

import lombok.Setter;
import org.apache.log4j.Logger;
import org.example.gui.components.SimpleButton;
import org.example.gui.mgstest.device.DeviceInfo;
import org.example.gui.mgstest.pool.DeviceState;
import org.example.gui.mgstest.transport.CradleController;
import org.hid4java.HidDevice;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TabInfo extends DeviceTab {
    private Logger log = Logger.getLogger(TabInfo.class);
    @Setter
    private CradleController cradleController;
    @Setter
    private HidDevice selectedDevice;
    @Setter
    private DeviceState deviceState;

    // Компоненты для разделов
    private JTextField cpuIdField = new JTextField();
    private JTextField serialNumberField = new JTextField();
    private JTextField swVersionField = new JTextField();
    private JTextField hwVersionField = new JTextField();
    private JTextField timeField = new JTextField();
    private JCheckBox beepCheckBox = new JCheckBox();
    private JCheckBox vibroCheckBox = new JCheckBox();
    private JCheckBox alarmCheckBox = new JCheckBox();
    private JCheckBox skipAlarmTestCheckBox = new JCheckBox();
    private JComboBox<String> modeComboBox = new JComboBox<>(new String[]{"stop mode", "transport mode"});

    public TabInfo(CradleController cradleController, HidDevice selectedDevice, DeviceState deviceState) {
        super("Информация");
        this.selectedDevice = selectedDevice;
        this.cradleController = cradleController;
        this.deviceState = deviceState;
        initComponents();
    }

    private void initComponents() {
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Раздел: Информация о приборе
        panel.add(createDeviceInfoPanel());
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Раздел: Дата/время
        panel.add(createDateTimePanel());
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Раздел: Сигнализация
        panel.add(createAlarmPanel());
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Раздел: Проверка индикации
        panel.add(createTestPanel());
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Раздел: Состояние прибора
        panel.add(createStatusPanel());
    }

    private JPanel createDeviceInfoPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Информация о приборе"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 10, 5, 10);

        int row = 0;
        addLabelAndField(panel, gbc, "CPU ID:", cpuIdField, row++, false);
        addLabelAndField(panel, gbc, "Серийный номер:", serialNumberField, row++, false);
        addLabelAndField(panel, gbc, "Версия ПО:", swVersionField, row++, false);
        addLabelAndField(panel, gbc, "Версия железа:", hwVersionField, row++, false);

        return panel;
    }

    private JPanel createDateTimePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Дата/время"));

        JPanel contentPanel = new JPanel(new BorderLayout(5, 5));
        contentPanel.add(new JLabel("Время:"), BorderLayout.WEST);
        contentPanel.add(timeField, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        JButton setTimeButton = new SimpleButton("Задать");
        setTimeButton.setEnabled(false);
        JButton setPcTimeButton = new SimpleButton("Задать как на компьютере");
        setPcTimeButton.setEnabled(false);
        buttonPanel.add(setTimeButton);
        buttonPanel.add(setPcTimeButton);

        panel.add(contentPanel, BorderLayout.NORTH);
        panel.add(buttonPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createAlarmPanel() {
        JPanel panel = new JPanel(new GridLayout(4, 1, 5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Сигнализация"));

        JPanel vibroPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        vibroPanel.add(vibroCheckBox);
        vibroPanel.add(new JLabel("Вибрация"));
        panel.add(vibroPanel);

        JPanel beepPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        beepPanel.add(beepCheckBox);
        beepPanel.add(new JLabel("Звук"));
        panel.add(beepPanel);

        JPanel alarmPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        alarmPanel.add(alarmCheckBox);
        alarmPanel.add(new JLabel("Сигнализация"));
        panel.add(alarmPanel);

        JPanel skipTestPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        skipTestPanel.add(skipAlarmTestCheckBox);
        skipTestPanel.add(new JLabel("Пропускать тест сигнализации при включении"));
        panel.add(skipTestPanel);

        return panel;
    }

    private JPanel createTestPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Проверка индикации"));

        JButton blinkTestButton = new SimpleButton("Blink Test");
        JButton beepTestButton = new SimpleButton("Beep Test");

        blinkTestButton.addActionListener(e -> performBlinkTest());
        beepTestButton.addActionListener(e -> performBeepTest());

        panel.add(blinkTestButton);
        panel.add(beepTestButton);

        return panel;
    }

    private JPanel createStatusPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder("Состояние прибора"));

        // Первая строка кнопок
        JPanel buttonRow1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        JButton refreshButton = new SimpleButton("Обновить информацию");
        JButton rebootButton = new SimpleButton("Перезагрузить прибор");
        buttonRow1.add(refreshButton);
        buttonRow1.add(rebootButton);

        // Вторая строка - режим
        JPanel modePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        modePanel.add(new JLabel("Режим:"));
        modePanel.add(modeComboBox);
        JButton setModeButton = new SimpleButton("Задать");
        modePanel.add(setModeButton);

        // Третья строка - сброс батареи
        JPanel batteryPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        JButton resetBatteryButton = new SimpleButton("Сбросить состояние батареи");
        batteryPanel.add(resetBatteryButton);

        // Добавляем обработчики
        refreshButton.addActionListener(e -> refreshInfo());
        rebootButton.addActionListener(e -> rebootDevice());
        setModeButton.addActionListener(e -> setDeviceMode());
        resetBatteryButton.addActionListener(e -> resetBattery());
        alarmCheckBox.addActionListener(e -> switchAlarmState());

        // Компоновка
        panel.add(buttonRow1);
        panel.add(modePanel);
        panel.add(batteryPanel);

        return panel;
    }

    private void addLabelAndField(JPanel panel, GridBagConstraints gbc,
                                  String labelText, JTextField field,
                                  int row, boolean editable) {
        field.setEditable(editable);
        field.setBackground(editable ? Color.WHITE : Color.LIGHT_GRAY);

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.3;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel(labelText), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(field, gbc);
    }

    // Методы-заглушки для обработчиков событий
    private void performBlinkTest() {
        if (selectedDevice != null && selectedDevice.open()) {
            cradleController.blinkTest(selectedDevice);
        } else {
            JOptionPane.showMessageDialog(null, "Устройство не подключено");
        }
    }

    private void performBeepTest() {
        if (selectedDevice != null && selectedDevice.open()) {
            cradleController.beepTest(selectedDevice);
        } else {
            JOptionPane.showMessageDialog(null, "Устройство не подключено");
        }
    }

    private void refreshInfo() {
        // TODO: Добавить логику обновления информации
        try {
            cradleController.getDeviceInfo(selectedDevice);
        } catch (Exception e) {
            log.warn("Ошибка обновления данных" + e.getMessage());
            //throw new RuntimeException(e);
        }

    }

    private void rebootDevice() {
        if(selectedDevice.open()){
            cradleController.rebootCmd(selectedDevice);
        }
    }

    private void setDeviceMode() {
        String selectedMode = (String) modeComboBox.getSelectedItem();
        // TODO: Добавить логику установки режима
    }

    private void resetBattery() {
        cradleController.resetBatteryCounter(selectedDevice);
    }

    private void switchAlarmState(){
        if(selectedDevice.open()){
            if(alarmCheckBox.isSelected()){
                deviceState.getDeviceInfo().setAlarmEnabled(true);
                cradleController.alarmOn(selectedDevice);
            }else{
                deviceState.getDeviceInfo().setAlarmEnabled(false);
                cradleController.alarmOff(selectedDevice);
            }
        }else{
            log.warn("Не удалось подключиться");
        }
    }
    @Override
    public void updateData(DeviceState state) {
        if (state != null && state.getDeviceInfo() != null) {
            DeviceInfo info = state.getDeviceInfo();
            cpuIdField.setText(info.getCpuId());
            serialNumberField.setText(String.valueOf(info.getSerialNumber()));
            swVersionField.setText(info.getSwMaj() + "." + info.getSwMin());
            hwVersionField.setText(info.getHwMaj() + "." + info.getHwMin());

            if (info.getTime() > 0) {
                Date date = new Date(info.getTime() * 1000L);
                timeField.setText(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date));
            } else {
                timeField.setText("Нет данных");
            }

            beepCheckBox.setSelected(info.isBeepEnabled());
            vibroCheckBox.setSelected(info.isVibroEnabled());
            alarmCheckBox.setSelected(info.isAlarmEnabled());
        } else {
            clearFields();
        }
    }

    private void clearFields() {
        Component[] components = panel.getComponents();
        for (Component comp : components) {
            if (comp instanceof JTextField) {
                ((JTextField) comp).setText("Нет данных");
            } else if (comp instanceof JCheckBox) {
                ((JCheckBox) comp).setSelected(false);
            }
        }
    }

    @Override
    public void saveData(DeviceState state) {
        // Для информационной вкладки обычно не требуется сохранение
    }
}