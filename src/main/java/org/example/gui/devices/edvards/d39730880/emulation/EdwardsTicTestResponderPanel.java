package org.example.gui.devices.edvards.d39730880.emulation;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.concurrent.*;

public class EdwardsTicTestResponderPanel extends JPanel {

    private final EdwardsResponder responder = new EdwardsResponder();
    private final EdwardsSerialService serialService;

    private final JComboBox<String> portCombo = new JComboBox<>();
    private final JButton startBtn = new JButton("Старт");
    private final JButton stopBtn = new JButton("Стоп");

    private final JSlider turboSlider = new JSlider(0, 7, 0);
    private final JLabel turboLabel = new JLabel("Turbo State: 0");

    private final JSlider gauge1Slider = new JSlider(0, 10000, 1234);
    private final JLabel gauge1Label = new JLabel("Gauge 1: 1.234e-3");

    private final JSlider psTempSlider = new JSlider(0, 100, 35);
    private final JLabel psTempLabel = new JLabel("PS Temp: 35.0 °C");

    private final JSlider internalTempSlider = new JSlider(0, 100, 32);
    private final JLabel internalTempLabel = new JLabel("Internal Temp: 32.0 °C");

    private final JTextArea logArea = new JTextArea();
    private final List<String> logLines = new CopyOnWriteArrayList<>();

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public EdwardsTicTestResponderPanel() {
        this.serialService = new EdwardsSerialService(this::handleCommand);

        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createTitledBorder("Эмулятор Edwards TIC (D397)"));

        add(createControlPanel(), BorderLayout.WEST);
        add(createLogPanel(), BorderLayout.CENTER);

        initListeners();
        refreshPorts();
        stopBtn.setEnabled(false);

        scheduler.scheduleAtFixedRate(this::updateLabels, 500, 500, TimeUnit.MILLISECONDS);
    }

    private JPanel createControlPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        p.setPreferredSize(new Dimension(280, 0));

        p.add(new JLabel("COM порт"));
        p.add(portCombo);
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        btns.add(startBtn);
        btns.add(stopBtn);
        p.add(btns);

        p.add(Box.createVerticalStrut(15));
        p.add(new JLabel("Turbo Pump State"));
        turboSlider.setMajorTickSpacing(1);
        turboSlider.setPaintTicks(true);
        p.add(turboSlider);
        p.add(turboLabel);

        p.add(Box.createVerticalStrut(15));
        p.add(new JLabel("Gauge 1 Pressure"));
        gauge1Slider.setMajorTickSpacing(2000);
        gauge1Slider.setPaintTicks(true);
        p.add(gauge1Slider);
        p.add(gauge1Label);

        p.add(Box.createVerticalStrut(15));
        p.add(new JLabel("PS Temperature (°C)"));
        psTempSlider.setMajorTickSpacing(20);
        psTempSlider.setPaintTicks(true);
        p.add(psTempSlider);
        p.add(psTempLabel);

        p.add(Box.createVerticalStrut(15));
        p.add(new JLabel("Internal Temperature (°C)"));
        internalTempSlider.setMajorTickSpacing(20);
        internalTempSlider.setPaintTicks(true);
        p.add(internalTempSlider);
        p.add(internalTempLabel);

        p.add(Box.createVerticalGlue());
        return p;
    }

    private JScrollPane createLogPanel() {
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setBorder(BorderFactory.createTitledBorder("Лог команд"));
        return scroll;
    }

    private void initListeners() {
        startBtn.addActionListener(e -> start());
        stopBtn.addActionListener(e -> stop());

        turboSlider.addChangeListener(e -> {
            int state = turboSlider.getValue();
            responder.setTurboState(state);
            turboLabel.setText("Turbo State: " + state);
        });

        gauge1Slider.addChangeListener(e -> {
            double val = gauge1Slider.getValue() / 1000.0;
            responder.setGaugeValue(0, val);
            gauge1Label.setText(String.format("Gauge 1: %.3e", val));
        });

        psTempSlider.addChangeListener(e -> {
            responder.setTemperature(psTempSlider.getValue(), internalTempSlider.getValue());
            psTempLabel.setText(String.format("PS Temp: %d °C", psTempSlider.getValue()));
        });

        internalTempSlider.addChangeListener(e -> {
            responder.setTemperature(psTempSlider.getValue(), internalTempSlider.getValue());
            internalTempLabel.setText(String.format("Internal Temp: %d °C", internalTempSlider.getValue()));
        });
    }

    private void refreshPorts() {
        portCombo.removeAllItems();
        for (com.fazecast.jSerialComm.SerialPort p : com.fazecast.jSerialComm.SerialPort.getCommPorts()) {
            portCombo.addItem(p.getSystemPortName());
        }
    }

    private void start() {
        String port = (String) portCombo.getSelectedItem();
        if (port == null) { addLog("Порт не выбран"); return; }
        if (serialService.openPort(port, 9600)) {
            startBtn.setEnabled(false);
            stopBtn.setEnabled(true);
            addLog("Эмулятор запущен на " + port);
        } else addLog("Не удалось открыть порт");
    }

    private void stop() {
        serialService.closePort();
        startBtn.setEnabled(true);
        stopBtn.setEnabled(false);
        addLog("Эмулятор остановлен");
    }

    private String handleCommand(String cmd) {
        String response = responder.processCommand(cmd);
        SwingUtilities.invokeLater(() -> {
            addLog(">> " + cmd);
            if (response != null) addLog("<< " + response.trim());
        });
        return response;
    }

    private void addLog(String msg) {
        logLines.add(msg);
        if (logLines.size() > 150) logLines.remove(0);
        SwingUtilities.invokeLater(() -> {
            logArea.setText(String.join("\n", logLines));
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void updateLabels() {
        SwingUtilities.invokeLater(() -> {
            turboLabel.setText("Turbo State: " + responder.getTurboState());
            double g1 = responder.getGaugeValue(0);
            gauge1Label.setText(String.format("Gauge 1: %.3e", g1));
            psTempLabel.setText(String.format("PS Temp: %d °C", psTempSlider.getValue()));
            internalTempLabel.setText(String.format("Internal Temp: %d °C", internalTempSlider.getValue()));
        });
    }

    public void shutdown() {
        serialService.closePort();
        scheduler.shutdownNow();
    }
}