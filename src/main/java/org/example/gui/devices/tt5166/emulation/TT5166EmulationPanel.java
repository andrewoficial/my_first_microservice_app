package org.example.gui.devices.tt5166.emulation;

import com.fazecast.jSerialComm.SerialPort;
import org.example.gui.devices.emulation.EmulatorCommandLog;
import org.example.gui.utilites.GuiUtilities;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Панель эмулятора климатической камеры TT5166 (Modbus RTU, 38400 8E1).
 */
public class TT5166EmulationPanel extends JPanel {

    private final TT5166Emulator emulator = new TT5166Emulator();
    private final TT5166ModbusResponder responder = new TT5166ModbusResponder(emulator, 1);
    private final TT5166ModbusSerialService service = new TT5166ModbusSerialService(responder);
    private final EmulatorCommandLog commandLog = new EmulatorCommandLog("TT-5166");

    private final JComboBox<String> portCombo = new JComboBox<>();
    private final JButton refreshBtn = new JButton("Обновить");
    private final JButton openBtn = new JButton("Открыть");
    private final JButton closeBtn = new JButton("Закрыть");

    private final JCheckBox onCheckBox = new JCheckBox("ВКЛ", false);
    private final JSpinner setpointSpinner = new JSpinner(new SpinnerNumberModel(25.0, -100.0, 200.0, 0.5));
    private final JSpinner humSetpointSpinner = new JSpinner(new SpinnerNumberModel(50.0, 0.0, 100.0, 1.0));
    private final JSpinner rampSpinner = new JSpinner(new SpinnerNumberModel(2.0, 0.1, 50.0, 0.1));
    private final JSpinner tempGradSpinner = new JSpinner(new SpinnerNumberModel(1.0, 0.0, 50.0, 0.1));
    private final JSpinner humGradSpinner = new JSpinner(new SpinnerNumberModel(1.0, 0.0, 50.0, 0.1));
    private final JCheckBox programModeCheck = new JCheckBox("Режим программы", false);
    private final JSpinner faultSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 0xFFFF, 1));
    private final JSpinner progHoursSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 9999, 1));
    private final JSpinner progMinsSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 59, 1));
    private final JSpinner ch2Spinner = new JSpinner(new SpinnerNumberModel(20.0, -100.0, 200.0, 0.5));
    private final JSpinner ch3Spinner = new JSpinner(new SpinnerNumberModel(20.0, -100.0, 200.0, 0.5));
    private final JSpinner ch4Spinner = new JSpinner(new SpinnerNumberModel(20.0, -100.0, 200.0, 0.5));

    private final JCheckBox mappingCheckBox = new JCheckBox("Маппинг адресов", false);
    private final JSpinner addRegSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 65535, 1));
    private final JSpinner addValSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 65535, 1));
    private final JButton addRegBtn = new JButton("Добавить");
    private final JComboBox<Integer> regCombo = new JComboBox<>();
    private final JTextField selValField = new JTextField(8);
    private final JButton delRegBtn = new JButton("Удалить");

    private final JLabel tempScreen = screenLabel();
    private final JLabel humBig = screenLabel();
    private final JLabel timeLabel = infoLabel();
    private final JLabel modeLabel = infoLabel();
    private final JLabel setpointInfo = infoLabel();
    private final JLabel humInfo = infoLabel();
    private final JCheckBox logAuto = new JCheckBox("автопрокрутка лога", true);
    private final JLabel statusLabel = new JLabel("Эмулятор отключён");
    private final JTextArea logArea = new JTextArea();

    private final javax.swing.Timer simTimer;
    private long lastTickNanos = System.nanoTime();

    private final List<String> logLines = new CopyOnWriteArrayList<>();
    private static final int MAX_LOG = 400;

    public TT5166EmulationPanel() {
        responder.setCommandLog(commandLog);
        humBig.setForeground(new Color(60, 150, 255));
        humBig.setText("-- %");

        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Эмулятор TT5166 (Modbus RTU, 38400 8E1)",
                TitledBorder.LEFT, TitledBorder.TOP));

        add(createLeftPanel(), BorderLayout.WEST);
        add(createCenterPanel(), BorderLayout.CENTER);

        refreshBtn.addActionListener(e -> refreshPorts());
        openBtn.addActionListener(e -> openPort());
        closeBtn.addActionListener(e -> closePort());
        closeBtn.setEnabled(false);

        onCheckBox.addActionListener(e -> emulator.setOn(onCheckBox.isSelected()));
        setpointSpinner.addChangeListener(e -> emulator.setSetpointC(((Number) setpointSpinner.getValue()).doubleValue()));
        humSetpointSpinner.addChangeListener(e -> emulator.setHumiditySetpoint(((Number) humSetpointSpinner.getValue()).doubleValue()));
        rampSpinner.addChangeListener(e -> emulator.setRampRateCPerSec(((Number) rampSpinner.getValue()).doubleValue()));
        tempGradSpinner.addChangeListener(e -> emulator.setTempGradientCPer10Min(((Number) tempGradSpinner.getValue()).doubleValue()));
        humGradSpinner.addChangeListener(e -> emulator.setHumGradientPctPer10Min(((Number) humGradSpinner.getValue()).doubleValue()));
        programModeCheck.addActionListener(e -> emulator.setProgramMode(programModeCheck.isSelected()));
        faultSpinner.addChangeListener(e -> emulator.setFaultCode(((Number) faultSpinner.getValue()).intValue()));
        progHoursSpinner.addChangeListener(e -> applyProgramTime());
        progMinsSpinner.addChangeListener(e -> applyProgramTime());
        ch2Spinner.addChangeListener(e -> emulator.setChannelTempC(2, ((Number) ch2Spinner.getValue()).doubleValue()));
        ch3Spinner.addChangeListener(e -> emulator.setChannelTempC(3, ((Number) ch3Spinner.getValue()).doubleValue()));
        ch4Spinner.addChangeListener(e -> emulator.setChannelTempC(4, ((Number) ch4Spinner.getValue()).doubleValue()));

        mappingCheckBox.addActionListener(e -> responder.setAddressMapping(mappingCheckBox.isSelected()));
        selValField.setEditable(false);
        selValField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        addRegBtn.addActionListener(e -> addManualRegister());
        delRegBtn.addActionListener(e -> removeSelectedRegister());
        regCombo.addActionListener(e -> showSelectedValue());

        service.addLogListener(line -> SwingUtilities.invokeLater(() -> appendLog(line)));

        simTimer = new javax.swing.Timer(100, e -> advanceSim());
        simTimer.start();
        refreshPorts();

        GuiUtilities.darkenInputs(this);
    }

    private void applyProgramTime() {
        emulator.setProgramTime(((Number) progHoursSpinner.getValue()).intValue(),
                ((Number) progMinsSpinner.getValue()).intValue());
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
        modeLabel.setText("Режим: " + (emulator.isOn() ? "Работа" : "Остановлен")
                + " · " + (emulator.isProgramMode() ? "программа" : "фикс."));
        setpointInfo.setText(String.format(Locale.US, "Уставка: %.1f °C", emulator.getSetpointC()));
        humInfo.setText(String.format(Locale.US, "Влажность: %.1f%% / уставка %.1f%%",
                emulator.getCurrentHumidity(), emulator.getHumiditySetpoint()));
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
                        statusLabel.setText("Открыт " + portName + " @ 38400 8E1");
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

        p.add(label("Уставка, °C (рег 0x0026)"));
        p.add(fullWidth(setpointSpinner));
        p.add(Box.createVerticalStrut(4));
        p.add(label("Уставка влаги, % (рег 0x0027)"));
        p.add(fullWidth(humSetpointSpinner));
        p.add(Box.createVerticalStrut(4));
        p.add(label("Скорость выхода, °C/сек"));
        p.add(fullWidth(rampSpinner));

        p.add(Box.createVerticalStrut(10));
        p.add(sectionLabel("Настройки (регистры)"));
        p.add(label("Градиент темп., °C/10мин (0x0064)"));
        p.add(fullWidth(tempGradSpinner));
        p.add(Box.createVerticalStrut(4));
        p.add(label("Градиент влаги, %/10мин (0x0065)"));
        p.add(fullWidth(humGradSpinner));
        p.add(Box.createVerticalStrut(4));
        programModeCheck.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(programModeCheck);
        p.add(Box.createVerticalStrut(4));
        p.add(label("Код ошибки (рег 0x001B)"));
        p.add(fullWidth(faultSpinner));

        p.add(Box.createVerticalStrut(10));
        p.add(sectionLabel("Время программы (0x0006/0x0008)"));
        JPanel progRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        progRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        progRow.add(new JLabel("ч"));
        progHoursSpinner.setPreferredSize(new Dimension(70, progHoursSpinner.getPreferredSize().height));
        progRow.add(progHoursSpinner);
        progRow.add(new JLabel("м"));
        progMinsSpinner.setPreferredSize(new Dimension(70, progMinsSpinner.getPreferredSize().height));
        progRow.add(progMinsSpinner);
        p.add(progRow);

        p.add(Box.createVerticalStrut(10));
        p.add(sectionLabel("Доп. каналы (°C, 0x0024-0x0026)"));
        p.add(label("Канал 2"));
        p.add(fullWidth(ch2Spinner));
        p.add(Box.createVerticalStrut(2));
        p.add(label("Канал 3"));
        p.add(fullWidth(ch3Spinner));
        p.add(Box.createVerticalStrut(2));
        p.add(label("Канал 4"));
        p.add(fullWidth(ch4Spinner));

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
        appendLog("Доп. регистр 0x" + String.format("%04X", addr) + " = " + value);
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
        appendLog("Доп. регистр 0x" + String.format("%04X", addr) + " удалён");
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

    private static JPanel panelBox(String title, JLabel value) {
        JPanel box = new JPanel(new BorderLayout());
        box.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.DARK_GRAY), title),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        box.add(value, BorderLayout.CENTER);
        return box;
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

    public void shutdown() { simTimer.stop(); service.closePort(); }
}