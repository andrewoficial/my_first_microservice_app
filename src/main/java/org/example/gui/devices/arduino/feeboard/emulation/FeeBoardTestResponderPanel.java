package org.example.gui.devices.arduino.feeboard.emulation;

import com.fazecast.jSerialComm.SerialPort;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Панель тестов: COM-слушатель + слайдеры температуры/тока/и т.д.
 */
public class FeeBoardTestResponderPanel extends JPanel {

    private final FeeBoardResponder responder = new FeeBoardResponder();
    private final FeeBoardSerialService serialService;

    private final JComboBox<String> portCombo = new JComboBox<>();
    private final JComboBox<Integer> baudCombo = new JComboBox<>();
    private final JButton startBtn = new JButton("Старт");
    private final JButton stopBtn = new JButton("Стоп");
    private final JButton refreshBtn = new JButton("Обновить");

    private final JSpinner tempSpinner = new JSpinner(new SpinnerNumberModel(25.3, -40.0, 125.0, 0.1));
    private final JSpinner currentSpinner = new JSpinner(new SpinnerNumberModel(0.15, 0.0, 500.0, 0.01));
    private final JSpinner humiditySpinner = new JSpinner(new SpinnerNumberModel(40.0, 0.0, 100.0, 1.0));
    private final JSpinner pressureSpinner = new JSpinner(new SpinnerNumberModel(759.4, 500.0, 900.0, 0.1));
    private final JSpinner voltsSpinner = new JSpinner(new SpinnerNumberModel(3.30, 0.0, 30.0, 0.01));
    private final JCheckBox powerCb = new JCheckBox("Питание потребителя (FPWR)", false);

    private final JTextArea logArea = new JTextArea();
    private final List<String> logLines = new CopyOnWriteArrayList<>();
    private static final int MAX_LOG = 200;

    public FeeBoardTestResponderPanel() {
        this.serialService = new FeeBoardSerialService(this::handleCommand);

        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Эмулятор ARD_FEE_BRD_METER",
                TitledBorder.LEFT, TitledBorder.TOP));

        add(createLeftPanel(), BorderLayout.WEST);
        add(createLogPanel(), BorderLayout.CENTER);

        baudCombo.addItem(9600);
        baudCombo.addItem(57600);
        baudCombo.addItem(115200);
        baudCombo.setSelectedItem(57600);

        refreshPorts();
        stopBtn.setEnabled(false);

        startBtn.addActionListener(e -> start());
        stopBtn.addActionListener(e -> stop());
        refreshBtn.addActionListener(e -> refreshPorts());

        tempSpinner.addChangeListener(e ->
                responder.setTemperatureC(((Number) tempSpinner.getValue()).doubleValue()));
        currentSpinner.addChangeListener(e ->
                responder.setCurrentMa(((Number) currentSpinner.getValue()).doubleValue()));
        humiditySpinner.addChangeListener(e ->
                responder.setHumidityPct(((Number) humiditySpinner.getValue()).doubleValue()));
        pressureSpinner.addChangeListener(e ->
                responder.setPressureMmHg(((Number) pressureSpinner.getValue()).doubleValue()));
        voltsSpinner.addChangeListener(e ->
                responder.setConsumerVolts(((Number) voltsSpinner.getValue()).doubleValue()));
        powerCb.addActionListener(e -> responder.setFeePowerOn(powerCb.isSelected()));

        setInputFieldsBlackBackground();
    }

    private JPanel createLeftPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        p.setPreferredSize(new Dimension(280, 0));

        p.add(label("COM порт"));
        p.add(portCombo);
        p.add(Box.createVerticalStrut(4));
        p.add(label("Baud (8N1)"));
        p.add(baudCombo);
        p.add(Box.createVerticalStrut(6));

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        btns.setAlignmentX(Component.LEFT_ALIGNMENT);
        btns.add(refreshBtn);
        btns.add(startBtn);
        btns.add(stopBtn);
        p.add(btns);

        p.add(Box.createVerticalStrut(16));
        p.add(label("Температура, °C"));
        p.add(tempSpinner);
        p.add(Box.createVerticalStrut(8));
        p.add(label("Ток (mA)"));
        p.add(currentSpinner);
        p.add(Box.createVerticalStrut(8));
        p.add(label("Влажность, %"));
        p.add(humiditySpinner);
        p.add(Box.createVerticalStrut(8));
        p.add(label("Давление, mmHg"));
        p.add(pressureSpinner);
        p.add(Box.createVerticalStrut(8));
        p.add(label("Напряжение потребителя, V"));
        p.add(voltsSpinner);
        p.add(Box.createVerticalStrut(10));
        powerCb.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(powerCb);

        p.add(Box.createVerticalGlue());
        for (Component c : p.getComponents()) {
            if (c instanceof JComponent jc && !(c instanceof JPanel)) {
                jc.setAlignmentX(Component.LEFT_ALIGNMENT);
                jc.setMaximumSize(new Dimension(Integer.MAX_VALUE, jc.getPreferredSize().height));
            }
        }
        return p;
    }

    private static JLabel label(String t) {
        JLabel l = new JLabel(t);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JScrollPane createLogPanel() {
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setBorder(BorderFactory.createTitledBorder("Лог команд"));
        return scroll;
    }

    private void refreshPorts() {
        portCombo.removeAllItems();
        for (SerialPort p : SerialPort.getCommPorts()) {
            portCombo.addItem(p.getSystemPortName());
        }
    }

    private void start() {
        String port = (String) portCombo.getSelectedItem();
        if (port == null) {
            return;
        }
        int baud = (int) baudCombo.getSelectedItem();
        if (serialService.openPort(port, baud)) {
            startBtn.setEnabled(false);
            stopBtn.setEnabled(true);
            addLog("Слушатель запущен на " + port + " @ " + baud + " 8N1");
        } else {
            JOptionPane.showMessageDialog(this, "Не удалось открыть " + port);
        }
    }

    private void stop() {
        serialService.closePort();
        startBtn.setEnabled(true);
        stopBtn.setEnabled(false);
        addLog("Слушатель остановлен");
    }

    private String handleCommand(String cmd) {
        addLog("→ " + cmd);
        String resp = responder.processCommand(cmd);
        if (resp != null) {
            String preview = resp.replace('\r', ' ').replace('\n', ' ').trim();
            addLog("← " + (preview.length() > 100 ? preview.substring(0, 100) + "…" : preview));
            // sync checkbox if FPWR toggled by command
            SwingUtilities.invokeLater(() -> powerCb.setSelected(responder.isFeePowerOn()));
        }
        return resp;
    }

    private void addLog(String line) {
        SwingUtilities.invokeLater(() -> {
            logLines.add(line);
            while (logLines.size() > MAX_LOG) {
                logLines.remove(0);
            }
            logArea.setText(String.join("\n", logLines));
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void setInputFieldsBlackBackground() {
        setSpinnerBlack(tempSpinner);
        setSpinnerBlack(currentSpinner);
        setSpinnerBlack(humiditySpinner);
        setSpinnerBlack(pressureSpinner);
        setSpinnerBlack(voltsSpinner);
    }

    private static void setSpinnerBlack(JSpinner s) {
        JComponent editor = s.getEditor();
        if (editor instanceof JSpinner.DefaultEditor de) {
            de.getTextField().setBackground(Color.BLACK);
            de.getTextField().setForeground(Color.WHITE);
        }
    }

    public void shutdown() {
        serialService.closePort();
    }
}
