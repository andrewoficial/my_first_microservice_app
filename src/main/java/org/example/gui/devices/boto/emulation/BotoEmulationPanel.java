package org.example.gui.devices.boto.emulation;

import com.fazecast.jSerialComm.SerialPort;
import org.example.gui.devices.emulation.EmulatorCommandLog;
import org.example.gui.utilites.GuiUtilities;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.prefs.Preferences;

/**
 * Панель эмулятора термокамеры BOTO (Modbus RTU, 9600 8N1).
 * Параметризуется через конструктор: регистры и масштаб.
 */
public class BotoEmulationPanel extends JPanel {

    private static final String PREFS_NODE = "org/example/gui/devices/boto/emulation";
    private static final String LAST_PORT = "lastEmuPort";
    private final AtomicBoolean loadingSettings = new AtomicBoolean(false);

    /** Имена битов флагов ошибок (рег 20). Пока условные — уточнить по реальной камере. */
    private static final String[] ERROR_BIT_NAMES = {
            "Перегрев", "Недогрев/датчик", "Датчик темп.", "Датчик влаги",
            "Мало воды", "Парогенератор", "Вентилятор", "Дверь открыта"
    };

    private final BotoEmulator emulator = new BotoEmulator();

    /** Чекбоксы флагов ошибок (рег 20); инициализируются сразу, т.к. используются при построении UI. */
    private JCheckBox[] initErrorBits() {
        JCheckBox[] boxes = new JCheckBox[ERROR_BIT_NAMES.length];
        for (int b = 0; b < boxes.length; b++) {
            final int bit = b;
            boxes[b] = new JCheckBox(ERROR_BIT_NAMES[b]);
            boxes[b].addActionListener(e -> emulator.setErrorFlag(bit, boxes[bit].isSelected()));
        }
        return boxes;
    }
    private final BotoModbusResponder responder;
    private final BotoModbusSerialService service;

    private final JComboBox<String> portCombo = new JComboBox<>();
    private final JButton refreshBtn = new JButton("Обновить");
    private final JButton openBtn = new JButton("Открыть");
    private final JButton closeBtn = new JButton("Закрыть");

    private final JSpinner setpointSpinner;
    private final JSpinner rampSpinner;
    private final JCheckBox onCheckBox;
    private final JCheckBox humiEnCheckBox = new JCheckBox("Поддержка влаги ВКЛ", false);
    private final JSpinner humiSetpointSpinner = new JSpinner(new SpinnerNumberModel(50.0, 0.0, 100.0, 1.0));
    private final JLabel lightStateLabel = new JLabel("Подсветка: выкл");
    private final JCheckBox[] errorBits = initErrorBits();
    private final JCheckBox mappingCheckBox = new JCheckBox("Маппинг адресов", false);
    private final JSpinner addRegSpinner = new JSpinner(new SpinnerNumberModel(19, 0, 65535, 1));
    private final JSpinner addValSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 65535, 1));
    private final JButton addRegBtn = new JButton("Добавить");
    private final JComboBox<Integer> regCombo = new JComboBox<>();
    private final JTextField selValField = new JTextField(8);
    private final JButton delRegBtn = new JButton("Удалить");

    private final JSpinner tempMaxSpinner = new JSpinner(new SpinnerNumberModel(90.0, 0.0, 400.0, 1.0));
    private final JSpinner humiMaxSpinner = new JSpinner(new SpinnerNumberModel(98.0, 0.0, 100.0, 1.0));
    private final JSpinner timeSetSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 2359, 1));
    private final JSpinner tempRateSpinner = new JSpinner(new SpinnerNumberModel(0.5, 0.0, 10.0, 0.1));
    private final JSpinner humiRateSpinner = new JSpinner(new SpinnerNumberModel(0.5, 0.0, 10.0, 0.1));
    private final JLabel runTimeLabel = new JLabel("00:00:00");
    private final JButton resetRunBtn = new JButton("Сбросить RUN time");
    private final JLabel sysClockLabel = new JLabel();
    private final JLabel resClockLabel = new JLabel();

    private final JLabel tempScreen;
    private final JLabel humBig;
    private final JLabel timeLabel = infoLabel();
    private final JLabel modeLabel = infoLabel();
    private final JLabel setpointInfo = infoLabel();
    private final JLabel humInfo = infoLabel();
    private final JLabel lightInfo = infoLabel();
    private final JLabel errorInfo = infoLabel();
    private final JCheckBox logAuto = new JCheckBox("автопрокрутка лога", true);
    private final JLabel statusLabel = new JLabel("Эмулятор отключён");
    private final JTextArea logArea = new JTextArea();
    private final EmulatorCommandLog commandLog;

    private final javax.swing.Timer simTimer;
    private long lastTickNanos = System.nanoTime();

    private final List<String> logLines = new CopyOnWriteArrayList<>();
    private static final int MAX_LOG = 400;
    private final String deviceName;
    private final int tempScale;

    public BotoEmulationPanel(String deviceName, int tempReg, int setTempReg, int modReg, int tempScale) {
        this.deviceName = deviceName;
        this.tempScale = tempScale;
        this.commandLog = new EmulatorCommandLog(deviceName);

        this.responder = new BotoModbusResponder(emulator, 1, tempReg, setTempReg, modReg, tempScale);
        this.responder.setCommandLog(commandLog);
        this.service = new BotoModbusSerialService(responder);

        this.setpointSpinner = new JSpinner(new SpinnerNumberModel(25.0, 0.0, 400.0, 0.5));
        this.rampSpinner = new JSpinner(new SpinnerNumberModel(2.0, 0.1, 50.0, 0.1));
        this.onCheckBox = new JCheckBox("ВКЛ", false);
        this.tempScreen = screenLabel();
        this.humBig = screenLabel();
        humBig.setForeground(new Color(60, 150, 255));
        humBig.setText("-- %");

        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Эмулятор " + deviceName + " (Modbus RTU, 9600 8N1)",
                TitledBorder.LEFT, TitledBorder.TOP));

        add(createLeftPanel(), BorderLayout.WEST);
        add(createCenterPanel(), BorderLayout.CENTER);

        refreshBtn.addActionListener(e -> refreshPorts());
        openBtn.addActionListener(e -> openPort());
        closeBtn.addActionListener(e -> closePort());
        closeBtn.setEnabled(false);

        setpointSpinner.addChangeListener(e -> emulator.setSetpointC(((Number) setpointSpinner.getValue()).doubleValue()));
        rampSpinner.addChangeListener(e -> emulator.setRampRateCPerSec(((Number) rampSpinner.getValue()).doubleValue()));
        onCheckBox.addActionListener(e -> emulator.setOn(onCheckBox.isSelected()));
        humiEnCheckBox.addActionListener(e -> emulator.setHumidityEnabled(humiEnCheckBox.isSelected()));
        humiSetpointSpinner.addChangeListener(e -> emulator.setHumiditySetpoint(((Number) humiSetpointSpinner.getValue()).doubleValue()));
        emulator.setHumiditySetpoint(((Number) humiSetpointSpinner.getValue()).doubleValue());
        mappingCheckBox.addActionListener(e -> responder.setAddressMapping(mappingCheckBox.isSelected()));

        selValField.setEditable(false);
        selValField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        addRegBtn.addActionListener(e -> addManualRegister());
        delRegBtn.addActionListener(e -> removeSelectedRegister());
        regCombo.addActionListener(e -> showSelectedValue());

        tempMaxSpinner.addChangeListener(e -> emulator.setTempMaxLimitC(((Number) tempMaxSpinner.getValue()).doubleValue()));
        humiMaxSpinner.addChangeListener(e -> emulator.setHumiMaxLimitPct(((Number) humiMaxSpinner.getValue()).doubleValue()));
        timeSetSpinner.addChangeListener(e -> emulator.setTimeSetRaw(((Number) timeSetSpinner.getValue()).intValue()));
        tempRateSpinner.addChangeListener(e -> emulator.setTempRatePerMin(((Number) tempRateSpinner.getValue()).doubleValue()));
        humiRateSpinner.addChangeListener(e -> emulator.setHumiRatePerMin(((Number) humiRateSpinner.getValue()).doubleValue()));
        resetRunBtn.addActionListener(e -> { emulator.resetRunTime(); updateRunTime(); });

        emulator.setTempMaxLimitC(((Number) tempMaxSpinner.getValue()).doubleValue());
        emulator.setHumiMaxLimitPct(((Number) humiMaxSpinner.getValue()).doubleValue());
        emulator.setTimeSetRaw(((Number) timeSetSpinner.getValue()).intValue());
        emulator.setTempRatePerMin(((Number) tempRateSpinner.getValue()).doubleValue());
        emulator.setHumiRatePerMin(((Number) humiRateSpinner.getValue()).doubleValue());

        service.addLogListener(line -> SwingUtilities.invokeLater(() -> appendLog(line)));

        simTimer = new javax.swing.Timer(100, e -> advanceSim());
        simTimer.start();
        refreshPorts();
        loadLastSettings();
        portCombo.addItemListener(e -> saveSelectedPort());

        GuiUtilities.darkenInputs(this);
    }

    private void advanceSim() {
        long now = System.nanoTime();
        double dt = (now - lastTickNanos) / 1_000_000_000.0;
        lastTickNanos = now;
        emulator.advance(Math.min(dt, 0.5));
        double t = emulator.getCurrentTempC();
        tempScreen.setText(String.format(Locale.US, "%.2f °C", t));
        tempScreen.setForeground(emulator.isOn() ? new Color(0, 140, 0) : new Color(160, 160, 160));
        humBig.setText(String.format(Locale.US, "%.1f %%", emulator.getCurrentHumidity()));
        timeLabel.setText("Время: " + java.time.LocalTime.now().withNano(0).toString());
        modeLabel.setText("Режим: " + (emulator.isOn() ? "Работа" : "Остановлен"));
        setpointInfo.setText(String.format(Locale.US, "Уставка: %.1f °C", emulator.getSetpointC()));
        humInfo.setText(String.format(Locale.US, "Влажность: %.1f%% / уставка %.1f%%",
                emulator.getCurrentHumidity(), emulator.getHumiditySetpoint()));
        lightInfo.setText("Подсветка: " + (emulator.isLightOn() ? "вкл" : "выкл") + " (следует за работой)");
        lightStateLabel.setText("Подсветка: " + (emulator.isLightOn() ? "вкл" : "выкл") + " (следует за работой)");
        errorInfo.setText(errorFlagsText());
        updateRunTime();
        updateClocks();
    }

    private String errorFlagsText() {
        StringBuilder sb = new StringBuilder();
        for (int b = 0; b < ERROR_BIT_NAMES.length; b++) {
            if (emulator.isErrorFlag(b)) {
                if (sb.length() > 0) sb.append("; ");
                sb.append(ERROR_BIT_NAMES[b]);
            }
        }
        if (sb.length() == 0) {
            errorInfo.setForeground(new Color(160, 160, 160));
            return "Ошибки: нет";
        }
        errorInfo.setForeground(new Color(200, 40, 40));
        return "Ошибки: " + sb;
    }

    private void updateRunTime() {
        runTimeLabel.setText(String.format("%02d:%02d:%02d",
                emulator.getRunHours(), emulator.getRunMinutes(), emulator.getRunSeconds()));
    }

    private void updateClocks() {
        int[] s = emulator.getSysTime();
        sysClockLabel.setText(String.format("%04d-%02d-%02d %02d:%02d:%02d", s[0], s[1], s[2], s[3], s[4], s[5]));
        int[] r = emulator.getReserveTime();
        resClockLabel.setText(String.format("%04d-%02d-%02d %02d:%02d:%02d", r[0], r[1], r[2], r[3], r[4], r[5]));
    }

    private void openPort() {
        String selected = (String) portCombo.getSelectedItem();
        if (selected == null || selected.contains("Нет")) {
            JOptionPane.showMessageDialog(this, "Выберите COM-порт");
            return;
        }
        String portName = selected.split(" — ")[0].trim();
        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() { return service.openPort(portName); }
            @Override protected void done() {
                try {
                    if (get()) {
                        openBtn.setEnabled(false);
                        closeBtn.setEnabled(true);
                        statusLabel.setText("Открыт " + portName + " @ 9600 8N1");
                    } else {
                        statusLabel.setText("Ошибка открытия порта");
                    }
                } catch (Exception ex) { statusLabel.setText("Ошибка: " + ex.getMessage()); }
            }
        }.execute();
    }

    private void closePort() {
        service.closePort();
        openBtn.setEnabled(true);
        closeBtn.setEnabled(false);
        statusLabel.setText("Эмулятор отключён");
    }

    // ─── UI ─────────────────────────────────────────────────────────────

    private JScrollPane createLeftPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        p.setPreferredSize(new Dimension(300, 0));
        p.add(label("COM-порт"));
        portCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        portCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, portCombo.getPreferredSize().height));
        p.add(portCombo);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        btns.setAlignmentX(Component.LEFT_ALIGNMENT);
        btns.add(refreshBtn);
        btns.add(openBtn);
        btns.add(closeBtn);
        p.add(btns);
        p.add(Box.createVerticalStrut(2));
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(statusLabel);
        p.add(Box.createVerticalStrut(4));
        JPanel cmdRow = EmulatorCommandLog.createControls(commandLog);
        cmdRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(cmdRow);

        p.add(Box.createVerticalStrut(12));
        p.add(sectionLabel("Состояние эмулятора"));

        onCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(onCheckBox);
        p.add(Box.createVerticalStrut(6));

        p.add(label("Уставка, °C"));
        p.add(fullWidth(setpointSpinner));
        p.add(Box.createVerticalStrut(4));

        p.add(label("Скорость выхода, °C/сек"));
        p.add(fullWidth(rampSpinner));

        p.add(Box.createVerticalStrut(10));
        p.add(sectionLabel("Влага (рег 18 / уставка 61)"));
        humiEnCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(humiEnCheckBox);
        p.add(Box.createVerticalStrut(2));
        p.add(label("Уставка влажности, %"));
        p.add(fullWidth(humiSetpointSpinner));

        p.add(Box.createVerticalStrut(8));
        p.add(sectionLabel("Подсветка (рег 19)"));
        lightStateLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        lightStateLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
        p.add(lightStateLabel);

        p.add(Box.createVerticalStrut(8));
        p.add(sectionLabel("Флаги ошибок (рег 20)"));
        JPanel errGrid = new JPanel(new GridLayout(0, 2, 6, 2));
        errGrid.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (JCheckBox b : errorBits) {
            b.setAlignmentX(Component.LEFT_ALIGNMENT);
            errGrid.add(b);
        }
        p.add(errGrid);

        p.add(Box.createVerticalStrut(10));
        p.add(sectionLabel("RUN time (рег 32/33/34)"));
        runTimeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        runTimeLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 18));
        p.add(runTimeLabel);
        p.add(Box.createVerticalStrut(4));
        resetRunBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(resetRunBtn);

        p.add(Box.createVerticalStrut(10));
        p.add(sectionLabel("Часы"));
        JPanel clockRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        JButton sysNowBtn = new JButton("Sys=сейчас");
        JButton resNowBtn = new JButton("Res=сейчас");
        sysNowBtn.addActionListener(e -> emulator.setSysTimeNow());
        resNowBtn.addActionListener(e -> emulator.setReserveTimeNow());
        clockRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        clockRow.add(sysNowBtn);
        clockRow.add(resNowBtn);
        p.add(clockRow);
        p.add(Box.createVerticalStrut(2));
        sysClockLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        sysClockLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(label("SYSTEM TIME (рег 92-97)"));
        p.add(sysClockLabel);
        p.add(Box.createVerticalStrut(2));
        resClockLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        resClockLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(label("RESERVE TIME (рег 151-156)"));
        p.add(resClockLabel);

        p.add(Box.createVerticalStrut(10));
        p.add(sectionLabel("Найденные регистры"));
        p.add(label("Предел темп. уставки °C (рег 39)"));
        p.add(fullWidth(tempMaxSpinner));
        p.add(Box.createVerticalStrut(4));
        p.add(label("Предел влаги уставки % (рег 41)"));
        p.add(fullWidth(humiMaxSpinner));
        p.add(Box.createVerticalStrut(4));
        p.add(label("TIME SET H.M, raw (рег 101)"));
        p.add(fullWidth(timeSetSpinner));
        p.add(Box.createVerticalStrut(4));
        p.add(label("C/M темп, °C/мин (рег 102)"));
        p.add(fullWidth(tempRateSpinner));
        p.add(Box.createVerticalStrut(4));
        p.add(label("%/M влага, %/мин (рег 103)"));
        p.add(fullWidth(humiRateSpinner));

        p.add(Box.createVerticalStrut(10));
        p.add(createManualEditor());

        JScrollPane scroller = new JScrollPane(p);
        scroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroller.setBorder(null);
        scroller.setPreferredSize(new Dimension(320, 0));
        return scroller;
    }

    private JPanel createCenterPanel() {
        JPanel center = new JPanel(new BorderLayout(8, 8));

        JPanel screens = new JPanel(new GridLayout(1, 2, 12, 0));
        screens.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        tempScreen.setHorizontalAlignment(SwingConstants.CENTER);
        humBig.setHorizontalAlignment(SwingConstants.CENTER);
        screens.add(panelBox("ТЕКУЩАЯ ТЕМПЕРАТУРА", tempScreen));
        screens.add(panelBox("ТЕКУЩАЯ ВЛАЖНОСТЬ", humBig));

        JPanel info = new JPanel(new GridLayout(0, 1, 4, 2));
        info.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.DARK_GRAY), "ПАРАМЕТРЫ"),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        info.add(timeLabel);
        info.add(modeLabel);
        info.add(setpointInfo);
        info.add(humInfo);
        info.add(lightInfo);
        info.add(errorInfo);

        JPanel top = new JPanel(new BorderLayout(4, 4));
        top.add(screens, BorderLayout.NORTH);
        top.add(info, BorderLayout.CENTER);
        center.add(top, BorderLayout.NORTH);

        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Лог обмена данными (hex RX/TX)"));
        logScroll.setPreferredSize(new Dimension(400, 170));

        JPanel logAutoRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        logAutoRow.add(logAuto);
        JPanel logWrap = new JPanel(new BorderLayout());
        logWrap.add(logAutoRow, BorderLayout.NORTH);
        logWrap.add(logScroll, BorderLayout.CENTER);
        center.add(logWrap, BorderLayout.CENTER);
        return center;
    }

    private static JPanel panelBox(String title, JLabel value) {
        JPanel box = new JPanel(new BorderLayout());
        box.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.DARK_GRAY), title),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        box.add(value, BorderLayout.CENTER);
        return box;
    }

    /**
     * Панель добавления «доп. регистров»: адрес+значение, список добавленных (выпадашка),
     * просмотр выбранного и удаление. Похожа на редактор байт Testa, но хранит регистры.
     */
    private JPanel createManualEditor() {
        JPanel wrap = new JPanel();
        wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));
        wrap.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Доп. регистры"),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)));

        JPanel addRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        addRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        addRow.add(new JLabel("адрес"));
        addRegSpinner.setPreferredSize(new Dimension(70, addRegSpinner.getPreferredSize().height));
        addRow.add(addRegSpinner);
        addRow.add(new JLabel("значение"));
        addValSpinner.setPreferredSize(new Dimension(90, addValSpinner.getPreferredSize().height));
        addRow.add(addValSpinner);
        addRow.add(addRegBtn);
        wrap.add(addRow);

        JPanel selRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        selRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        selRow.add(new JLabel("добавленные:"));
        regCombo.setPreferredSize(new Dimension(90, regCombo.getPreferredSize().height));
        selRow.add(regCombo);
        selRow.add(new JLabel("значение:"));
        selValField.setPreferredSize(new Dimension(90, selValField.getPreferredSize().height));
        selRow.add(selValField);
        selRow.add(delRegBtn);
        wrap.add(selRow);

        JPanel mapRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        mapRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        mapRow.add(mappingCheckBox);
        wrap.add(mapRow);
        return wrap;
    }

    private void addManualRegister() {
        int addr = ((Number) addRegSpinner.getValue()).intValue();
        int value = ((Number) addValSpinner.getValue()).intValue() & 0xFFFF;
        responder.setManualRegister(addr, value);
        if (!containsItem(addr)) {
            regCombo.addItem(addr);
        }
        regCombo.setSelectedItem(addr);
        showSelectedValue();
        appendLog("Доп. регистр " + addr + " = " + value);
    }

    private boolean containsItem(int addr) {
        for (int i = 0; i < regCombo.getItemCount(); i++) {
            if (regCombo.getItemAt(i) == addr) {
                return true;
            }
        }
        return false;
    }

    private void removeSelectedRegister() {
        Object sel = regCombo.getSelectedItem();
        if (sel == null) {
            return;
        }
        int addr = (Integer) sel;
        responder.removeManualRegister(addr);
        regCombo.removeItem(addr);
        showSelectedValue();
        appendLog("Доп. регистр " + addr + " удалён");
    }

    private void showSelectedValue() {
        Object sel = regCombo.getSelectedItem();
        if (sel == null) {
            selValField.setText("");
            return;
        }
        int addr = (Integer) sel;
        Integer v = responder.manualRegisters().get(addr);
        selValField.setText(v == null ? "" : String.valueOf(v));
    }

    private static JLabel screenLabel() {
        JLabel l = new JLabel("20.00 °C", SwingConstants.CENTER);
        l.setFont(new Font(Font.MONOSPACED, Font.BOLD, 34));
        l.setForeground(new Color(160, 160, 160));
        return l;
    }

    private static JLabel infoLabel() {
        JLabel l = new JLabel(" ");
        l.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 15));
        l.setHorizontalAlignment(SwingConstants.LEFT);
        return l;
    }

    private static JLabel label(String t) {
        JLabel l = new JLabel(t);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private static JLabel sectionLabel(String t) {
        JLabel l = new JLabel(t);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setFont(l.getFont().deriveFont(Font.BOLD));
        return l;
    }

    private static JComponent fullWidth(JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
        return c;
    }

    private void appendLog(String line) {
        logLines.add(line);
        while (logLines.size() > MAX_LOG) logLines.remove(0);
        logArea.setText(String.join("\n", logLines));
        if (logAuto.isSelected()) {
            logArea.setCaretPosition(logArea.getDocument().getLength());
        }
    }

    private void refreshPorts() {
        portCombo.removeAllItems();
        for (SerialPort p : SerialPort.getCommPorts()) {
            portCombo.addItem(p.getSystemPortName() + " — " + p.getDescriptivePortName());
        }
        if (portCombo.getItemCount() == 0) portCombo.addItem("Нет доступных портов");
    }

    private void loadLastSettings() {
        loadingSettings.set(true);
        try {
            String lastPort = Preferences.userRoot().node(PREFS_NODE).get(LAST_PORT, "");
            if (!lastPort.isEmpty()) {
                for (int i = 0; i < portCombo.getItemCount(); i++) {
                    String item = portCombo.getItemAt(i);
                    if (item != null && item.startsWith(lastPort)) {
                        portCombo.setSelectedIndex(i);
                        break;
                    }
                }
            }
        } finally {
            loadingSettings.set(false);
        }
    }

    private void saveSelectedPort() {
        if (loadingSettings.get()) return;
        Object sel = portCombo.getSelectedItem();
        if (sel == null || sel.toString().contains("Нет")) return;
        String portName = sel.toString().split(" — ")[0].trim();
        Preferences.userRoot().node(PREFS_NODE).put(LAST_PORT, portName);
    }

    public void shutdown() { simTimer.stop(); service.closePort(); }
}
