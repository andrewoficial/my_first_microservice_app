package org.example.gui.mgstest.gui.tabs;

import lombok.Setter;
import org.apache.log4j.Logger;
import org.example.gui.mgstest.model.answer.GetDeviceInfoModel;
import org.example.gui.mgstest.repository.DeviceState;
import org.example.gui.mgstest.service.DeviceAsyncExecutor;
import org.example.gui.mgstest.transport.CommandParameters;

import org.example.gui.mgstest.transport.DeviceCommand;

import org.example.gui.mgstest.transport.cmd.SetAlarmState;
import org.example.gui.mgstest.transport.cmd.SetSerialNumber;
import org.example.gui.mgstest.transport.commands.*;


import org.example.gui.mgstest.util.CrcValidator;
import org.hid4java.HidDevice;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TabInfo extends DeviceTab {
    private Logger log = Logger.getLogger(TabInfo.class);

    @Setter
    private HidDevice selectedDevice;

    private final DeviceAsyncExecutor asyncExecutor;

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

    public TabInfo(HidDevice selectedDevice, DeviceAsyncExecutor asyncExecutor) {
        super("Информация");
        this.selectedDevice = selectedDevice;
        this.asyncExecutor = asyncExecutor;
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

        clearFields();
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
        addLabelAndField(panel, gbc, "Версия S.V.:", swVersionField, row++, false);
        addLabelAndField(panel, gbc, "Версия H.V.:", hwVersionField, row++, false);



        return panel;
    }



    private JPanel createDateTimePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Дата/время"));

        JPanel contentPanel = new JPanel(new BorderLayout(5, 5));
        contentPanel.add(new JLabel("Время:"), BorderLayout.WEST);
        contentPanel.add(timeField, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        JButton setTimeButton = new JButton("Задать");
        setTimeButton.setEnabled(false);
        JButton setPcTimeButton = new JButton("Задать как на компьютере");
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

        JButton blinkTestButton = new JButton("Blink Test");
        JButton beepTestButton = new JButton("Beep Test");

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
        JButton refreshButton = new JButton("Обновить информацию");
        JButton rebootButton = new JButton("Перезагрузить прибор");
        buttonRow1.add(refreshButton);
        buttonRow1.add(rebootButton);

        // Вторая строка - режим
        JPanel modePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        modePanel.add(new JLabel("Режим:"));
        modePanel.add(modeComboBox);
        JButton setModeButton = new JButton("Задать");
        modePanel.add(setModeButton);

        // Третья строка - сброс батареи
        JPanel batteryPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        JButton resetBatteryButton = new JButton("Сбросить состояние батареи");
        batteryPanel.add(resetBatteryButton);

        JPanel setSerialPane = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        JButton setSerialNumberButton = new JButton("Задать серийный номер");
        batteryPanel.add(setSerialNumberButton);



        // Добавляем обработчики
        refreshButton.addActionListener(e -> refreshInfo());
        rebootButton.addActionListener(e -> rebootDevice());
        setModeButton.addActionListener(e -> setDeviceMode());
        resetBatteryButton.addActionListener(e -> resetBattery());
        alarmCheckBox.addActionListener(e -> switchAlarmState());
        setSerialNumberButton.addActionListener(e -> onSetSerialNumber());

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

    private void onSetSerialNumber() {
        checkDeviceState(selectedDevice);
        String input = JOptionPane.showInputDialog(this.getPanel(),
                "Enter serial number (8 digits):",
                "Set Serial Number",
                JOptionPane.QUESTION_MESSAGE);
        CrcValidator.validateSerialNumber(input);
        long number = Long.parseLong(input);
        CommandParameters parameters = new CommandParameters();
        parameters.setLongArgument(number);
        DeviceCommand command = new SetSerialNumber();
        asyncExecutor.executeCommand(command, parameters, selectedDevice);
    }



    private void performBlinkTest() {
        checkDeviceState(selectedDevice);
        DeviceCommand command = new DoBlinkTest();
        asyncExecutor.executeCommand(command, null, selectedDevice);
    }

    private void performBeepTest() {
        checkDeviceState(selectedDevice);
        DeviceCommand command = new DoBeepTest();
        asyncExecutor.executeCommand(command, null, selectedDevice);
    }

    private void switchAlarmState() {
        checkDeviceState(selectedDevice);
        CommandParameters parameters = new CommandParameters();
        if (alarmCheckBox.isSelected()) {
            parameters.setIntArgument(0);
            SetAlarmState setAlarmState = new SetAlarmState();
            asyncExecutor.executeCommand(setAlarmState, parameters, selectedDevice);
        } else {
            parameters.setIntArgument(1);
            SetAlarmState setAlarmState = new SetAlarmState();
            asyncExecutor.executeCommand(setAlarmState, parameters, selectedDevice);
        }
    }

    private void refreshInfo() {
        checkDeviceState(selectedDevice);
        DeviceCommand command = new GetDeviceInfoCommand();
        asyncExecutor.executeCommand(command, null, selectedDevice);
    }

    private void rebootDevice() {
        checkDeviceState(selectedDevice);
        DeviceCommand command = new DoRebootDevice();
        asyncExecutor.executeCommand(command, null, selectedDevice);
    }

    private void setDeviceMode() {
        String selectedMode = (String) modeComboBox.getSelectedItem();
        // TODO: Добавить логику установки режима
    }

    private void resetBattery() {
        checkDeviceState(selectedDevice);
        DeviceCommand command = new DoBatteryCounterReset();
        asyncExecutor.executeCommand(command, null, selectedDevice);
    }

    private void checkDeviceState(HidDevice device) {
        if (device == null) {
            log.warn("device == null");
            throw new IllegalStateException("device == null");
        }
        if (asyncExecutor.isDeviceBusy(selectedDevice)) {
            throw new IllegalStateException("Busy");
        }
    }

    @Override
    public void updateData(DeviceState state) {
        log.info("Обновляю данные для tabInfo");
        if (state != null && state.getDeviceInfo() != null) {
            GetDeviceInfoModel info = state.getDeviceInfo();
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
            log.info("Очищаю поля");
            clearFields();
        }
    }

    private void clearFields() {
        cpuIdField.setText("Нет данных");
        serialNumberField.setText("Нет данных");
        swVersionField.setText("Нет данных");
        hwVersionField.setText("Нет данных");
        timeField.setText("Нет данных");
        beepCheckBox.setSelected(false);
        vibroCheckBox.setSelected(false);
        alarmCheckBox.setSelected(false);
        skipAlarmTestCheckBox.setSelected(false);
    }

    @Override
    public void saveData(DeviceState state) {
        // Для информационной вкладки обычно не требуется сохранение
    }
}