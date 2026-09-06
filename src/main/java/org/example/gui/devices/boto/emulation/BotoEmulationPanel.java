package org.example.gui.devices.boto.emulation;

import com.fazecast.jSerialComm.SerialPort;
import org.example.gui.utilites.GuiUtilities;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Панель эмулятора термокамеры BOTO (Modbus RTU, 9600 8N1).
 * Параметризуется через конструктор: регистры и масштаб.
 */
public class BotoEmulationPanel extends JPanel {

    private final BotoEmulator emulator = new BotoEmulator();
    private final BotoModbusResponder responder;
    private final BotoModbusSerialService service;

    private final JComboBox<String> portCombo = new JComboBox<>();
    private final JButton refreshBtn = new JButton("Обновить");
    private final JButton openBtn = new JButton("Открыть");
    private final JButton closeBtn = new JButton("Закрыть");

    private final JSpinner setpointSpinner;
    private final JSpinner rampSpinner;
    private final JCheckBox onCheckBox;
    private final JCheckBox mappingCheckBox = new JCheckBox("Маппинг адресов", false);
    private final JButton applyManualBtn = new JButton("Применить доп. регистры");
    private final JButton clearManualBtn = new JButton("Очистить");

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
    private final JLabel statusLabel = new JLabel("Эмулятор отключён");
    private final JTextArea logArea = new JTextArea();
    private final JTextArea manualArea = new JTextArea(6, 30);

    private final javax.swing.Timer simTimer;
    private long lastTickNanos = System.nanoTime();

    private final List<String> logLines = new CopyOnWriteArrayList<>();
    private static final int MAX_LOG = 400;
    private final String deviceName;
    private final int tempScale;

    public BotoEmulationPanel(String deviceName, int tempReg, int setTempReg, int modReg, int tempScale) {
        this.deviceName = deviceName;
        this.tempScale = tempScale;

        this.responder = new BotoModbusResponder(emulator, 1, tempReg, setTempReg, modReg, tempScale);
        this.service = new BotoModbusSerialService(responder);

        this.setpointSpinner = new JSpinner(new SpinnerNumberModel(25.0, 0.0, 400.0, 0.5));
        this.rampSpinner = new JSpinner(new SpinnerNumberModel(2.0, 0.1, 50.0, 0.1));
        this.onCheckBox = new JCheckBox("ВКЛ", false);
        this.tempScreen = screenLabel();

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
        mappingCheckBox.addActionListener(e -> responder.setAddressMapping(mappingCheckBox.isSelected()));

        manualArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        manualArea.setText("19 0\n20 0\n21 0\n");
        applyManualBtn.addActionListener(e -> applyManualRegisters());
        clearManualBtn.addActionListener(e -> clearManualRegisters());

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
        updateRunTime();
        updateClocks();
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

        p.add(Box.createVerticalStrut(12));
        p.add(sectionLabel("Состояние эмулятора"));

        onCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(onCheckBox);
        p.add(Box.createVerticalStrut(6));

        mappingCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        mappingCheckBox.setToolTipText("Незамапленные регистры при чтении возвращают свой адрес");
        p.add(mappingCheckBox);
        p.add(Box.createVerticalStrut(6));

        p.add(label("Уставка, °C"));
        p.add(fullWidth(setpointSpinner));
        p.add(Box.createVerticalStrut(4));

        p.add(label("Скорость выхода, °C/сек"));
        p.add(fullWidth(rampSpinner));

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

        JScrollPane scroller = new JScrollPane(p);
        scroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroller.setBorder(null);
        scroller.setPreferredSize(new Dimension(320, 0));
        return scroller;
    }

    private JPanel createCenterPanel() {
        JPanel center = new JPanel(new BorderLayout(8, 8));

        JPanel screens = new JPanel(new GridLayout(1, 1, 12, 0));
        screens.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        JPanel box = new JPanel(new BorderLayout());
        box.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.DARK_GRAY), "ТЕКУЩАЯ ТЕМПЕРАТУРА"),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        tempScreen.setHorizontalAlignment(SwingConstants.CENTER);
        box.add(tempScreen, BorderLayout.CENTER);
        screens.add(box);
        center.add(screens, BorderLayout.NORTH);

        center.add(createManualEditor(), BorderLayout.CENTER);

        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Лог Modbus RTU (hex RX/TX)"));
        logScroll.setPreferredSize(new Dimension(400, 170));
        center.add(logScroll, BorderLayout.SOUTH);
        return center;
    }

    /**
     * Панель ручной установки значений регистров (для подбора карты по штатной программе).
     * Каждая строка: "адрес значение" (значения — целые raw; можно десятичные/hex).
     */
    private JPanel createManualEditor() {
        JPanel wrap = new JPanel(new BorderLayout(6, 6));
        wrap.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Доп. регистры (адрес = значение)"),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)));

        JTextArea hint = new JTextArea(
                "Строка: <адрес> <значение>. Перекрывает чтение любого регистра.\n"
                        + "Пример: 19 1234   (или  19 0x123).");
        hint.setEditable(false);
        hint.setBackground(wrap.getBackground());
        hint.setForeground(Color.GRAY);
        hint.setLineWrap(true);
        hint.setWrapStyleWord(true);

        JScrollPane scroll = new JScrollPane(manualArea);
        scroll.setPreferredSize(new Dimension(200, 110));

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        btns.add(applyManualBtn);
        btns.add(clearManualBtn);

        wrap.add(hint, BorderLayout.NORTH);
        wrap.add(scroll, BorderLayout.CENTER);
        wrap.add(btns, BorderLayout.SOUTH);
        return wrap;
    }

    private void applyManualRegisters() {
        responder.clearManualRegisters();
        int ok = 0;
        for (String raw : manualArea.getText().split("\\r?\\n")) {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split("[\\s,;]+");
            if (parts.length < 2) continue;
            try {
                int addr = parseNumber(parts[0]);
                int value = parseNumber(parts[1]);
                responder.setManualRegister(addr, value);
                ok++;
            } catch (NumberFormatException ignored) { }
        }
        appendLog("Применено доп. регистров: " + ok);
    }

    private void clearManualRegisters() {
        responder.clearManualRegisters();
        manualArea.setText("");
        appendLog("Доп. регистры очищены");
    }

    /** Парсит число: десятичное или hex (0x/префикс). */
    private static int parseNumber(String s) throws NumberFormatException {
        String t = s.trim().toLowerCase();
        if (t.startsWith("0x")) return Integer.parseInt(t.substring(2), 16);
        return Integer.parseInt(t);
    }

    private static JLabel screenLabel() {
        JLabel l = new JLabel("20.00 °C", SwingConstants.CENTER);
        l.setFont(new Font(Font.MONOSPACED, Font.BOLD, 34));
        l.setForeground(new Color(160, 160, 160));
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
        logArea.setCaretPosition(logArea.getDocument().getLength());
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
