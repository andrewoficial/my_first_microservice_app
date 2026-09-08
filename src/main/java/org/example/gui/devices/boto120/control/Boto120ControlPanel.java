package org.example.gui.devices.boto120.control;

import org.example.device.protBoto.BotoModbusUtil;
import org.example.gui.devices.boto.control.BotoModbusCommunicationService;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.prefs.Preferences;

/**
 * Панель управления BOTO-120 (China Modbus, Modbus RTU 9600 8N1).
 *
 * <p>Протокол подтверждён: полевым чтением и перехватом штатной программы.
 * рег 12 — текущая температура ×100 (0x03),
 * рег 100 — уставка ×100 (0x06),
 * рег 105 — вкл/выкл (0x06, 1/0).
 * Штатная программа делает так: сначала ВКЛ (рег 105=1), затем уставка (рег 100).
 * Каждую секунду опрашивается рег 12 и показывается текущая температура.
 */
public class Boto120ControlPanel extends JPanel {

    private static final String PREFS_NODE = "org/example/gui/devices/boto120/control";
    private static final String PREFS_KEY_PORT = "lastPort";

    private static final int SLAVE_ID = 1;

    private final BotoModbusCommunicationService service = new BotoModbusCommunicationService();

    private final JComboBox<String> portCombo = new JComboBox<>();
    private final JSpinner setpointSpinner = new JSpinner(
            new SpinnerNumberModel(25.0, 0.0, 500.0, 0.5));
    private final JLabel readbackLabel = label("Рег 12: —");
    private final JLabel statusLabel = label("Отключено");
    private final JTextArea logArea = new JTextArea();

    private final JTextField scanFromField = new JTextField("0", 4);
    private final JTextField scanToField = new JTextField("255", 4);
    private final JButton scanButton = new JButton("Сканировать (чтение)");
    private volatile boolean scanning = false;
    private int scanReg = 0;
    private int scanTo = 255;
    private javax.swing.Timer scanTimer;

    private final Preferences prefs = Preferences.userRoot().node(PREFS_NODE);

    public Boto120ControlPanel() {
        super(new BorderLayout(8, 8));
        setBorder(new TitledBorder("BOTO-120 — China Modbus (рег 12/100/105)"));
        buildUi();
        refreshPorts();
        String lastPort = prefs.get(PREFS_KEY_PORT, "");
        if (!lastPort.isEmpty()) portCombo.setSelectedItem(lastPort);

        service.addLogListener(l -> SwingUtilities.invokeLater(() -> appendLog(l)));
        service.addStatusListener(s -> SwingUtilities.invokeLater(() -> statusLabel.setText(s)));
        service.addResponseListener(this::onResponse);
    }

    private void buildUi() {
        JPanel top = new JPanel(new BorderLayout(8, 4));

        JPanel portRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        portRow.add(new JLabel("COM:"));
        portCombo.setPreferredSize(new Dimension(160, 26));
        portRow.add(portCombo);
        JButton refreshBtn = new JButton("Обновить");
        refreshBtn.addActionListener(e -> refreshPorts());
        portRow.add(refreshBtn);
        JButton openBtn = new JButton("Открыть");
        openBtn.addActionListener(e -> openPort());
        portRow.add(openBtn);
        JButton closeBtn = new JButton("Закрыть");
        closeBtn.addActionListener(e -> closePort());
        portRow.add(closeBtn);

        JPanel setRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        buildSetpointRow(setRow);

        JPanel scanRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        scanRow.add(new JLabel("Скан чтения рег: от"));
        scanRow.add(scanFromField);
        scanRow.add(new JLabel("до"));
        scanRow.add(scanToField);
        scanRow.add(scanButton);
        scanButton.addActionListener(e -> toggleScan());

        top.add(portRow, BorderLayout.NORTH);
        top.add(scanRow, BorderLayout.CENTER);
        top.add(setRow, BorderLayout.SOUTH);

        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(logArea);

        add(top, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
    }

    private void buildSetpointRow(JPanel setRow) {
        setRow.add(new JLabel("Уставка, °C:"));
        addSetpointSpinner(setRow);
        setRow.add(writeButton("Уставка (рег 100 ×100)", () -> writeSetpoint(100, 100, "China/100")));
        setRow.add(writeButton("ВКЛ (рег 105=1)", () -> writeCmd(105, 1, "включение")));
        setRow.add(writeButton("ВЫКЛ (рег 105=0)", () -> writeCmd(105, 0, "выключение")));
        readbackLabel.setToolTipText("Опрос рег 12 каждую секунду: текущая температура ×100 (China Modbus, подтверждено)");
        setRow.add(readbackLabel);
    }

    private void addSetpointSpinner(JPanel row) {
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(setpointSpinner, "0.0");
        setpointSpinner.setEditor(editor);
        setpointSpinner.setPreferredSize(new Dimension(80, 26));
        row.add(setpointSpinner);
    }

    private JButton writeButton(String text, Runnable action) {
        JButton b = new JButton(text);
        b.addActionListener(e -> action.run());
        return b;
    }

    private void refreshPorts() {
        String prev = (String) portCombo.getSelectedItem();
        portCombo.removeAllItems();
        com.fazecast.jSerialComm.SerialPort[] ports = com.fazecast.jSerialComm.SerialPort.getCommPorts();
        for (com.fazecast.jSerialComm.SerialPort p : ports) portCombo.addItem(p.getSystemPortName());
        if (portCombo.getItemCount() == 0) portCombo.addItem("— нет COM-портов —");
        else if (prev != null) portCombo.setSelectedItem(prev);
    }

    private void openPort() {
        String portName = (String) portCombo.getSelectedItem();
        if (portName == null || portName.startsWith("—")) return;
        if (!service.open(portName)) {
            appendLog("Не удалось открыть " + portName);
            return;
        }
        prefs.put(PREFS_KEY_PORT, portName);
        service.setPollingEnabled(true, this::pollOnce);
    }

    private void closePort() {
        service.close();
        service.setPollingEnabled(false, this::pollOnce);
    }

    /** Опрос кандидата «текущая температура» (рег 12) — что ответит, то и видим. */
    private void pollOnce() {
        service.sendRequest(BotoModbusUtil.buildReadHoldingRequest(SLAVE_ID, 12, 1));
    }

    private void writeSetpoint(int regAddr, int scale, String hint) {
        double setpoint = ((Number) setpointSpinner.getValue()).doubleValue();
        int raw = (int) Math.round(setpoint * scale);
        writeCmd(regAddr, raw, hint + ", уставка " + String.format("%.2f", setpoint) + " °C");
    }

    private void writeCmd(int regAddr, int value, String hint) {
        appendLog("Запись (" + hint + "): рег " + regAddr + " = " + value);
        service.sendRequest(BotoModbusUtil.buildWriteSingleRequest(SLAVE_ID, regAddr, value));
    }

    private void onResponse(byte[] frame) {
        if (frame.length < 5) return;
        if (frame[1] != 0x03 || (frame[2] & 0xFF) != 2) return;   // только ответ на чтение 1 регистра
        int[] v = BotoModbusUtil.parseReadResponse(frame);
        if (v == null || v.length == 0) return;
        int raw = v[0];

        if (scanning) {
            if (raw != 0) {
                double maybe = raw / 100.0;
                String extra = (maybe > -55 && maybe < 400)
                        ? String.format("  (→ %.2f, если ×100)", maybe)
                        : "";
                appendLog(String.format("рег %d = %d (0x%04X)%s", scanReg - 1, raw, raw, extra));
            }
            return;
        }

        String decoded;
        if (raw == 0) {
            decoded = "0 (нулевая — регистр не тот?)";
        } else {
            double temp = raw / 100.0;
            if (temp > -55 && temp < 150) {
                decoded = String.format("raw=%d → %.2f °C (×100, подтверждено полем)", raw, temp);
            } else {
                decoded = String.format("raw=%d → значение странное (%.2f °C по ×100)", raw, temp);
            }
        }
        SwingUtilities.invokeLater(() -> readbackLabel.setText("Рег 12: " + decoded));
    }

    // ─── скан чтения регистров (только 0x03, безопасно для камеры) ─────────

    private void toggleScan() {
        if (scanning) {
            stopScan(null);
            return;
        }
        startScan();
    }

    private void startScan() {
        if (!service.isConnected()) {
            appendLog("Сначала откройте порт");
            return;
        }
        int from, to;
        try {
            from = Integer.parseInt(scanFromField.getText().trim());
            to = Integer.parseInt(scanToField.getText().trim());
        } catch (NumberFormatException e) {
            appendLog("Диапазон должен быть числами");
            return;
        }
        if (from < 0 || to < from || to > 65535) {
            appendLog("Диапазон: от 0 до 65535, от ≤ до");
            return;
        }
        service.setPollingEnabled(false, this::pollOnce);
        scanning = true;
        scanReg = from;
        scanTo = to;
        scanButton.setText("Стоп скана");
        appendLog(String.format("Скан чтения рег %d..%d (только чтение, 0x03)", from, to));
        scanTimer = new Timer(250, e -> scanTick());
        scanTimer.start();
    }

    private void scanTick() {
        if (scanReg > scanTo) {
            stopScan("Скан завершён");
            return;
        }
        service.sendRequest(BotoModbusUtil.buildReadHoldingRequest(SLAVE_ID, scanReg, 1));
        scanReg++;
    }

    private void stopScan(String doneMsg) {
        if (scanTimer != null) scanTimer.stop();
        scanning = false;
        scanButton.setText("Сканировать (чтение)");
        if (service.isConnected()) service.setPollingEnabled(true, this::pollOnce);
        if (doneMsg != null) appendLog(doneMsg);
    }

    private void appendLog(String line) {
        logArea.append(line + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
        return l;
    }

    public void shutdown() {
        service.shutdown();
    }
}