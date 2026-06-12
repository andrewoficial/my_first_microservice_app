package org.example.gui.mgstest.gui.tabs;

import lombok.Setter;
import org.apache.log4j.Logger;
import org.example.gui.mgstest.model.HidSupportedDevice;
import org.example.gui.mgstest.model.answer.GetDeviceInfoModel;
import org.example.gui.mgstest.model.DeviceState;
import org.example.gui.mgstest.service.DeviceAsyncExecutor;
import org.example.gui.mgstest.transport.CommandParameters;
import org.example.gui.mgstest.transport.DeviceCommand;
import org.example.gui.mgstest.transport.cmd.mgs.*;
import org.example.gui.mgstest.transport.cmd.mgs.settings.DoBatteryCounterReset;
import org.example.gui.mgstest.transport.cmd.mgs.settings.SetSerialNumber;
import org.example.gui.mgstest.transport.cmd.mkrs.GetInfo;
import org.example.gui.mgstest.util.CrcValidator;
import org.example.gui.utilites.GuiUtilities;
import org.example.utilites.Constants;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class TabInfo extends DeviceTab {
    private Logger log = Logger.getLogger(TabInfo.class);

    @Setter
    private HidSupportedDevice selectedDevice;

    private final DeviceAsyncExecutor asyncExecutor;

    // Компоненты (остались те же)
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

    private JSpinner dateSpinner;
    private JSpinner timeSpinner;

    public TabInfo(HidSupportedDevice selectedDevice, DeviceAsyncExecutor asyncExecutor) {
        super("Информация");
        this.selectedDevice = selectedDevice;
        this.asyncExecutor = asyncExecutor;
        initComponents();
    }

    // ===================== НОВАЯ КОМПОНОВКА (ДИЗАЙН ИЗ InfoTab) =====================

    private void initComponents() {
        panel.removeAll();
        panel.setLayout(new BorderLayout());

        JPanel content = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 10, 6, 10);
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.NORTH;

        // Верхняя строка: три колонки
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        content.add(createDeviceInfoPanel(), gbc);

        gbc.gridx = 1;
        content.add(createActionButtonsPanel(), gbc);

        gbc.gridx = 2;
        content.add(createAlarmAndModePanel(), gbc);

        // Строка с датой/временем (занимает 2/3 ширины)
        gbc.gridy = 1;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        content.add(createDateTimePanel(), gbc);

        // Нижние две колонки
        gbc.gridy = 2;
        gbc.gridwidth = 3;
        JPanel bottomPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbcBottom = new GridBagConstraints();
        gbcBottom.fill = GridBagConstraints.BOTH;
        gbcBottom.weightx = 0.5;
        gbcBottom.weighty = 1.0;
        gbcBottom.insets = new Insets(0, 5, 0, 5);

        JPanel leftColumn = createLeftColumnPanel();
        JPanel rightColumn = createRightColumnPanel();

        gbcBottom.gridx = 0;
        gbcBottom.gridy = 0;
        bottomPanel.add(leftColumn, gbcBottom);
        gbcBottom.gridx = 1;
        bottomPanel.add(rightColumn, gbcBottom);

        content.add(bottomPanel, gbc);

        // Распорка внизу, чтобы прижать содержимое к верху
        gbc.gridy = 3;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.VERTICAL;
        content.add(new JPanel(), gbc);

        panel.add(new JScrollPane(content), BorderLayout.CENTER);
    }

    private JPanel createDeviceInfoPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Информация о приборе"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.weightx = 0.3;

        // CPU ID
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("CPU ID:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        cpuIdField.setEditable(false);
        panel.add(cpuIdField, gbc);

        // SW Version
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("SW Version:"), gbc);
        gbc.gridx = 1;
        swVersionField.setEditable(false);
        panel.add(swVersionField, gbc);

        // HW Version
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("HW Version:"), gbc);
        gbc.gridx = 1;
        hwVersionField.setEditable(false);
        panel.add(hwVersionField, gbc);

        // Время устройства
        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("Время устройства:"), gbc);
        gbc.gridx = 1;
        timeField.setEditable(false);
        panel.add(timeField, gbc);

        return panel;
    }

    private JPanel createActionButtonsPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 1, 0, 6));
        panel.setBorder(BorderFactory.createTitledBorder("Действия"));

        JButton refreshButton = new JButton("Обновить информацию");
        JButton blinkTestButton = new JButton("Blink Test");
        JButton beepTestButton = new JButton("Beep Test");
        JButton rebootButton = new JButton("Перезагрузить прибор");
        JButton resetBatteryButton = new JButton("Сбросить состояние батареи");

        panel.add(refreshButton);
        panel.add(blinkTestButton);
        panel.add(beepTestButton);
        panel.add(rebootButton);
        panel.add(resetBatteryButton);

        // Обработчики (логика из старого кода)
        refreshButton.addActionListener(e -> refreshInfo());
        blinkTestButton.addActionListener(e -> performBlinkTest());
        beepTestButton.addActionListener(e -> performBeepTest());
        rebootButton.addActionListener(e -> rebootDevice());
        resetBatteryButton.addActionListener(e -> resetBattery());

        return panel;
    }

    private JPanel createAlarmAndModePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // Блок сигнализации
        JPanel alarmPanel = new JPanel(new GridLayout(4, 1, 5, 5));
        alarmPanel.setBorder(BorderFactory.createTitledBorder("Сигнализация"));

        JPanel vibroPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        vibroPanel.add(vibroCheckBox);
        vibroPanel.add(new JLabel("Вибрация"));
        alarmPanel.add(vibroPanel);

        JPanel beepPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        beepPanel.add(beepCheckBox);
        beepPanel.add(new JLabel("Звук"));
        alarmPanel.add(beepPanel);

        JPanel alarmCheckPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        alarmCheckPanel.add(alarmCheckBox);
        alarmCheckPanel.add(new JLabel("Сигнализация"));
        alarmPanel.add(alarmCheckPanel);

        JPanel skipTestPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        skipTestPanel.add(skipAlarmTestCheckBox);
        skipTestPanel.add(new JLabel("Пропускать тест сигнализации при включении"));
        alarmPanel.add(skipTestPanel);

        // Блок режима
        JPanel modePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        modePanel.setBorder(BorderFactory.createTitledBorder("Режим"));
        modePanel.add(new JLabel("Режим:"));
        modePanel.add(modeComboBox);
        JButton setModeButton = new JButton("Задать");
        setModeButton.addActionListener(e -> setDeviceMode());
        modePanel.add(setModeButton);

        panel.add(alarmPanel);
        panel.add(Box.createVerticalStrut(8));
        panel.add(modePanel);

        // Обработчики чекбоксов
        alarmCheckBox.addActionListener(e -> switchAlarmState());
        beepCheckBox.addActionListener(e -> switchSoundState());
        vibroCheckBox.addActionListener(e -> switchVibrationState());

        return panel;
    }

    private JPanel createDateTimePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Установка даты/времени"));

        JPanel datetimePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        datetimePanel.add(new JLabel("Дата:"));
        dateSpinner = new JSpinner(new SpinnerDateModel());
        JSpinner.DateEditor dateEditor = new JSpinner.DateEditor(dateSpinner, "dd.MM.yyyy");
        GuiUtilities.changeFont(dateEditor.getTextField());
        dateSpinner.setEditor(dateEditor);
        dateSpinner.setValue(new Date());
        datetimePanel.add(dateSpinner);

        datetimePanel.add(new JLabel("Время:"));
        timeSpinner = new JSpinner(new SpinnerDateModel());
        JSpinner.DateEditor timeEditor = new JSpinner.DateEditor(timeSpinner, "HH:mm:ss");
        GuiUtilities.changeFont(timeEditor.getTextField());
        timeSpinner.setEditor(timeEditor);
        timeSpinner.setValue(new Date());
        datetimePanel.add(timeSpinner);

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

    private JPanel createLeftColumnPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Серийный номер"));
        serialNumberField.setColumns(12);
        panel.add(serialNumberField);
        JButton setSerialNumberButton = new JButton("Задать");
        setSerialNumberButton.addActionListener(e -> onSetSerialNumber());
        panel.add(setSerialNumberButton);
        return panel;
    }

    private JPanel createRightColumnPanel() {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder("Дополнительные настройки"));
        panel.add(new JLabel("(нет доступных настроек)"));
        return panel;
    }

    // ===================== ВСЕ СТАРЫЕ МЕТОДЫ (ЛОГИКА) ОСТАЮТСЯ БЕЗ ИЗМЕНЕНИЙ =====================

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
        Date selectedDate = (Date) dateSpinner.getValue();
        Date selectedTime = (Date) timeSpinner.getValue();
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
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        JOptionPane.showMessageDialog(this.getPanel(),
                "Устанавливается время: " + sdf.format(combinedCal.getTime()),
                "Установка времени",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void performSetPcTime() {
        checkDeviceState(selectedDevice);
        long currentUnixTime = System.currentTimeMillis() / 1000L;
        CommandParameters timeParams = new CommandParameters();
        timeParams.setLongArgument(currentUnixTime);
        SetDeviceTime timeCommand = new SetDeviceTime();
        asyncExecutor.executeCommand(timeCommand, timeParams, selectedDevice);
        Date now = new Date();
        dateSpinner.setValue(now);
        timeSpinner.setValue(now);
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
        } else {
            parameters.setIntArgument(1);
        }
        SetAlarmState setAlarmState = new SetAlarmState();
        asyncExecutor.executeCommand(setAlarmState, parameters, selectedDevice);
    }

    private void switchSoundState() {
        checkDeviceState(selectedDevice);
        CommandParameters parameters = new CommandParameters();
        if (beepCheckBox.isSelected()) {
            parameters.setIntArgument(0);
        } else {
            parameters.setIntArgument(1);
        }
        SetSoundState setSoundState = new SetSoundState();
        asyncExecutor.executeCommand(setSoundState, parameters, selectedDevice);
    }

    private void switchVibrationState() {
        checkDeviceState(selectedDevice);
        CommandParameters parameters = new CommandParameters();
        if (vibroCheckBox.isSelected()) {
            parameters.setIntArgument(0);
        } else {
            parameters.setIntArgument(1);
        }
        SetVibrationState setVibrationState = new SetVibrationState();
        asyncExecutor.executeCommand(setVibrationState, parameters, selectedDevice);
    }

    private void refreshInfo() {
        checkDeviceState(selectedDevice);
        if (selectedDevice.getDeviceType() == Constants.SupportedHidDeviceType.MIKROSENSE) {
            DeviceCommand command = new GetInfo();
            asyncExecutor.executeCommand(command, null, selectedDevice);
        } else if (selectedDevice.getDeviceType() == Constants.SupportedHidDeviceType.MULTIGASSENSE) {
            DeviceCommand command = new GetDeviceInformation();
            asyncExecutor.executeCommand(command, null, selectedDevice);
        } else {
            throw new IllegalStateException("Unknown device type");
        }
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
        if (selectedMode != null && selectedMode.equals("stop mode")) {
            parameters.setIntArgument(1);
        } else if (selectedMode != null && selectedMode.equals("transport mode")) {
            parameters.setIntArgument(2);
        } else {
            throw new IllegalStateException("Unknown mode");
        }
        SetDevMode setDevMode = new SetDevMode();
        asyncExecutor.executeCommand(setDevMode, parameters, selectedDevice);
    }

    private void resetBattery() {
        checkDeviceState(selectedDevice);
        DeviceCommand command = new DoBatteryCounterReset();
        asyncExecutor.executeCommand(command, null, selectedDevice);
    }

    private void checkDeviceState(HidSupportedDevice device) {
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
        Date now = new Date();
        dateSpinner.setValue(now);
        timeSpinner.setValue(now);
    }

    @Override
    public void saveData(DeviceState state) {
        // Для информационной вкладки обычно не требуется сохранение
    }
}