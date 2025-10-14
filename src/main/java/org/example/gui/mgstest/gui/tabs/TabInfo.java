package org.example.gui.mgstest.gui.tabs;

import lombok.Setter;
import org.apache.log4j.Logger;
import org.example.gui.mgstest.model.answer.GetDeviceInfoModel;
import org.example.gui.mgstest.repository.DeviceState;
import org.example.gui.mgstest.service.DeviceAsyncExecutor;
import org.example.gui.mgstest.transport.CommandParameters;
import org.example.gui.mgstest.transport.DeviceCommand;
import org.example.gui.mgstest.transport.cmd.*;
import org.example.gui.mgstest.util.CrcValidator;
import org.hid4java.HidDevice;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
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

    // Компоненты для выбора даты и времени
    private JSpinner dateSpinner;
    private JSpinner timeSpinner;

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
        addLabelAndField(panel, gbc, "SW Version:", swVersionField, row++, false);
        addLabelAndField(panel, gbc, "HW Version:", hwVersionField, row++, false);
        addLabelAndField(panel, gbc, "Время устройства:", timeField, row++, false);

        return panel;
    }

    private JPanel createDateTimePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Установка даты/времени"));

        // Панель для выбора даты и времени
        JPanel datetimePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));

        // Выбор даты
        datetimePanel.add(new JLabel("Дата:"));
        dateSpinner = new JSpinner(new SpinnerDateModel());
        JSpinner.DateEditor dateEditor = new JSpinner.DateEditor(dateSpinner, "dd.MM.yyyy");
        dateSpinner.setEditor(dateEditor);
        dateSpinner.setValue(new Date()); // текущая дата по умолчанию
        datetimePanel.add(dateSpinner);

        // Выбор времени
        datetimePanel.add(new JLabel("Время:"));
        timeSpinner = new JSpinner(new SpinnerDateModel());
        JSpinner.DateEditor timeEditor = new JSpinner.DateEditor(timeSpinner, "HH:mm:ss");
        timeSpinner.setEditor(timeEditor);
        timeSpinner.setValue(new Date()); // текущее время по умолчанию
        datetimePanel.add(timeSpinner);

        // Панель кнопок
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        JButton setSelectedTimeButton = new JButton("Установить выбранное время");
        JButton setPcTimeButton = new JButton("Установить время компьютера");

        setSelectedTimeButton.addActionListener(e -> performSetSelectedTime());
        setPcTimeButton.addActionListener(e -> performSetPcTime());

        buttonPanel.add(setSelectedTimeButton);
        buttonPanel.add(setPcTimeButton);

        panel.add(datetimePanel, BorderLayout.NORTH);
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
        beepCheckBox.addActionListener(e -> switchSoundState());
        vibroCheckBox.addActionListener(e -> switchVibrationState());
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

    private void performSetSelectedTime() {
        checkDeviceState(selectedDevice);

        // Получаем выбранные дату и время
        Date selectedDate = (Date) dateSpinner.getValue();
        Date selectedTime = (Date) timeSpinner.getValue();

        // Объединяем дату и время
        Calendar dateCal = Calendar.getInstance();
        dateCal.setTime(selectedDate);

        Calendar timeCal = Calendar.getInstance();
        timeCal.setTime(selectedTime);

        Calendar combinedCal = Calendar.getInstance();
        combinedCal.set(dateCal.get(Calendar.YEAR),
                dateCal.get(Calendar.MONTH),
                dateCal.get(Calendar.DAY_OF_MONTH),
                timeCal.get(Calendar.HOUR_OF_DAY),
                timeCal.get(Calendar.MINUTE),
                timeCal.get(Calendar.SECOND));

        long unixTime = combinedCal.getTimeInMillis() / 1000L;

        CommandParameters timeParams = new CommandParameters();
        timeParams.setLongArgument(unixTime);

        SetDeviceTime timeCommand = new SetDeviceTime();
        asyncExecutor.executeCommand(timeCommand, timeParams, selectedDevice);

        // Показываем информацию о устанавливаемом времени
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        JOptionPane.showMessageDialog(this.getPanel(),
                "Устанавливается время: " + sdf.format(combinedCal.getTime()),
                "Установка времени",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void performSetPcTime() {
        checkDeviceState(selectedDevice);

        // Получаем текущее время в Unix timestamp (секунды)
        long currentUnixTime = System.currentTimeMillis() / 1000L;

        CommandParameters timeParams = new CommandParameters();
        timeParams.setLongArgument(currentUnixTime);

        SetDeviceTime timeCommand = new SetDeviceTime();
        asyncExecutor.executeCommand(timeCommand, timeParams, selectedDevice);

        // Обновляем спиннеры текущим временем
        Date now = new Date();
        dateSpinner.setValue(now);
        timeSpinner.setValue(now);

        // Показываем информацию об устанавливаемом времени
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        JOptionPane.showMessageDialog(this.getPanel(),
                "Устанавливается текущее время компьютера: " + sdf.format(now),
                "Установка времени",
                JOptionPane.INFORMATION_MESSAGE);
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

    private void switchSoundState() {
        checkDeviceState(selectedDevice);
        CommandParameters parameters = new CommandParameters();
        if (beepCheckBox.isSelected()) {
            parameters.setIntArgument(0);
            SetSoundState setAlarmState = new SetSoundState();
            asyncExecutor.executeCommand(setAlarmState, parameters, selectedDevice);
        } else {
            parameters.setIntArgument(1);
            SetSoundState setAlarmState = new SetSoundState();
            asyncExecutor.executeCommand(setAlarmState, parameters, selectedDevice);
        }
    }

    private void switchVibrationState() {
        checkDeviceState(selectedDevice);
        CommandParameters parameters = new CommandParameters();
        if (vibroCheckBox.isSelected()) {
            parameters.setIntArgument(0);
            SetVibrationState setAlarmState = new SetVibrationState();
            asyncExecutor.executeCommand(setAlarmState, parameters, selectedDevice);
        } else {
            parameters.setIntArgument(1);
            SetVibrationState setAlarmState = new SetVibrationState();
            asyncExecutor.executeCommand(setAlarmState, parameters, selectedDevice);
        }
    }

    private void refreshInfo() {
        checkDeviceState(selectedDevice);
        DeviceCommand command = new GetDeviceInformation();
        asyncExecutor.executeCommand(command, null, selectedDevice);
    }

    private void rebootDevice() {
        checkDeviceState(selectedDevice);
        DeviceCommand command = new DoRebootDevice();
        asyncExecutor.executeCommand(command, null, selectedDevice);
    }

    private void setDeviceMode() {
        String selectedMode = (String) modeComboBox.getSelectedItem();
        checkDeviceState(selectedDevice);
        CommandParameters parameters = new CommandParameters();
        if(selectedMode != null && selectedMode.equals("stop mode")) {
            parameters.setIntArgument(1);
            SetDevMode setDevMode = new SetDevMode();
            asyncExecutor.executeCommand(setDevMode, parameters, selectedDevice);
        } else if (selectedMode != null && selectedMode.equals("transport mode")) {
            parameters.setIntArgument(2);
            SetDevMode setDevMode = new SetDevMode();
            asyncExecutor.executeCommand(setDevMode, parameters, selectedDevice);
        }else{
            throw new IllegalStateException("Unknown mode");
        }
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

                // Также обновляем спиннеры временем устройства
                dateSpinner.setValue(date);
                timeSpinner.setValue(date);
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

        // Устанавливаем спиннеры на текущее время
        Date now = new Date();
        dateSpinner.setValue(now);
        timeSpinner.setValue(now);
    }

    @Override
    public void saveData(DeviceState state) {
        // Для информационной вкладки обычно не требуется сохранение
    }
}