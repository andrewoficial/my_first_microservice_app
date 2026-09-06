package org.example.gui.devices.testa.control;

import org.example.gui.utilites.GuiUtilities;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Панель управления климатической камерой Testa (UDP).
 * Работает с настоящей камерой (host:1300) и с эмулятором. Полный разбор статуса
 * (температура, влажность, свет, аварии) + команды: уставка, запуск/стоп, свет.
 */
public class TestaControlPanel {

    private static final Color GREEN = new Color(0, 160, 0);
    private static final Color RED = new Color(200, 40, 40);
    private static final Color GRAY = Color.DARK_GRAY;
    private static final Color ORANGE = new Color(200, 120, 0);

    private static final String[] ALARM_NAMES = {
            "Мало воды в парогенераторе", null, null, null, null, null, null, null
    };

    private final TestaCommunicationService service = new TestaCommunicationService();

    private JPanel mainPanel;
    private JTextField hostField;
    private JSpinner portSpinner;
    private JButton connectBtn;
    private JButton disconnectBtn;
    private JLabel comLamp;
    private JLabel statusLabel;

    private JLabel modeLabel = new JLabel("—");
    private JLabel alarmState = new JLabel("аварии: нет");
    private volatile double lastHumActual = Double.NaN;
    private volatile double lastHumSet = Double.NaN;
    private boolean lightDesired = false;

    // Температура
    private JSpinner targetSpinner;
    private JCheckBox maxSpeedCheck;
    private JSpinner rampSpinner;
    private JLabel setTempFromCamera = new JLabel("--");
    private JLabel curTempScreen;

    // Влажность
    private JCheckBox wetCheck;
    private JSpinner humiditySpinner;
    private JLabel humSetFromCamera = new JLabel("--");
    private JLabel humCurLabel = new JLabel("--");

    // Подсветка
    private JButton lightBtn = new JButton("Переключить");
    private JLabel lightState = new JLabel("выключен");

    // Управление
    private JButton paramBtn = new JButton("Задать параметры");
    private JButton startBtn = new JButton("▶ Старт");
    private JButton stopBtn = new JButton("■ Стоп");

    private final JTextArea logArea = new JTextArea();
    private final List<String> logLines = new CopyOnWriteArrayList<>();
    private static final int MAX_LOG = 300;

    public TestaControlPanel() {
        buildUi();
        service.addTemperatureListener(t -> SwingUtilities.invokeLater(() -> curTempScreen.setText(
                String.format(Locale.US, "%.2f °C", t))));
        service.addSetpointListener(sp -> SwingUtilities.invokeLater(() -> setTempFromCamera.setText(
                String.format(Locale.US, "%.2f °C", sp))));
        service.addHumidityActualListener(h -> SwingUtilities.invokeLater(() -> {
            lastHumActual = h;
            humCurLabel.setText(fmtHum(h));
        }));
        service.addHumiditySetpointListener(h -> SwingUtilities.invokeLater(() -> {
            lastHumSet = h;
            humSetFromCamera.setText(fmtHum(h));
        }));
        service.addLightListener(on -> SwingUtilities.invokeLater(() ->
                lightState.setText(on ? "включен" : "выключен")));
        service.addAlarmsListener(a -> SwingUtilities.invokeLater(() -> renderAlarms(a)));
        service.addStatusListener(s -> SwingUtilities.invokeLater(() -> statusLabel.setText(s)));
        service.addLogListener(line -> SwingUtilities.invokeLater(() -> addLog(line)));
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    private static String fmtHum(double v) {
        return Double.isNaN(v) ? "--" : String.format(Locale.US, "%.1f%%", v);
    }

    private void renderAlarms(int alarms) {
        List<String> on = new java.util.ArrayList<>();
        for (int b = 0; b < 8; b++) {
            if ((alarms & (1 << b)) != 0) {
                on.add((b < ALARM_NAMES.length && ALARM_NAMES[b] != null)
                        ? ALARM_NAMES[b] : ("бит " + b));
            }
        }
        alarmState.setText(on.isEmpty() ? "аварии: нет" : "⚠ " + String.join("; ", on));
        alarmState.setForeground(on.isEmpty() ? GRAY : RED);
    }

    private void buildUi() {
        mainPanel = new JPanel(new BorderLayout(8, 8));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Управление Testa (UDP)",
                TitledBorder.LEFT, TitledBorder.TOP));

        // Соединение
        hostField = new JTextField("127.0.0.1", 9);
        portSpinner = new JSpinner(new SpinnerNumberModel(1300, 1, 65535, 1));
        connectBtn = new JButton("Подключиться");
        disconnectBtn = new JButton("Отключиться");
        disconnectBtn.setEnabled(false);
        comLamp = lamp(GRAY);
        statusLabel = new JLabel("Не подключено");
        JPanel connRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        connRow.add(new JLabel("Камера host:"));
        connRow.add(hostField);
        connRow.add(new JLabel("port:"));
        connRow.add(portSpinner);
        connRow.add(connectBtn);
        connRow.add(disconnectBtn);
        connRow.add(comLamp);
        connRow.add(statusLabel);
        top.add(connRow);

        // Температура
        JPanel temp = group("Температура");
        targetSpinner = new JSpinner(new SpinnerNumberModel(25.0, -40.0, 120.0, 0.5));
        maxSpeedCheck = new JCheckBox("работать с максимальной скоростью", true);
        rampSpinner = new JSpinner(new SpinnerNumberModel(5.0, 0.1, 60.0, 0.5));
        rampSpinner.setEnabled(false);
        maxSpeedCheck.addActionListener(e -> rampSpinner.setEnabled(!maxSpeedCheck.isSelected()));
        temp.add(row("Установка температуры:", degIcon(), targetSpinner));
        temp.add(row("Установленная температура:", degIcon(), setTempFromCamera));
        JPanel spRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        spRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        spRow.add(maxSpeedCheck);
        spRow.add(new JLabel("скорость выхода на температуру:"));
        spRow.add(rampSpinner);
        spRow.add(new JLabel("°C/мин"));
        temp.add(spRow);
        top.add(temp);

        // Влажность
        JPanel hum = group("Влажность");
        wetCheck = new JCheckBox("работа с влагой", true);
        humiditySpinner = new JSpinner(new SpinnerNumberModel(50.0, 0.0, 100.0, 1.0));
        hum.add(row("Установка влажности:", wetCheck));
        hum.add(row("Установка влажности, %RH:", humiditySpinner));
        hum.add(row("Установленная влажность:", humSetFromCamera));
        hum.add(row("Текущая влажность:", humCurLabel));
        top.add(hum);

        // Подсветка
        JPanel light = group("Подсветка");
        JPanel lr = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        lr.setAlignmentX(Component.LEFT_ALIGNMENT);
        lr.add(lightBtn);
        lr.add(new JLabel("текущее состояние:"));
        lr.add(lightState);
        light.add(lr);
        top.add(light);

        // Управление / режим
        JPanel ctrl = group("Управление");
        JPanel cr = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        cr.setAlignmentX(Component.LEFT_ALIGNMENT);
        cr.add(paramBtn);
        cr.add(startBtn);
        cr.add(stopBtn);
        cr.add(new JLabel("текущий режим работы:"));
        cr.add(modeLabel);
        ctrl.add(cr);

        alarmState.setFont(new Font(Font.DIALOG, Font.BOLD, 13));
        ctrl.add(alarmState);
        top.add(ctrl);

        // Экран текущей температуры + лог
        JPanel center = new JPanel(new BorderLayout(8, 8));
        JPanel screens = new JPanel(new GridLayout(1, 2, 12, 0));
        screens.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        curTempScreen = screenLabel();
        screens.add(screenBox("ТЕКУЩАЯ ТЕМПЕРАТУРА", curTempScreen));
        screens.add(screenBox("УСТАВКА", setTempFromCameraScreen()));
        center.add(screens, BorderLayout.NORTH);

        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Дебаг (UDP TX/RX)"));
        logScroll.setPreferredSize(new Dimension(400, 180));
        center.add(logScroll, BorderLayout.CENTER);

        mainPanel.add(top, BorderLayout.NORTH);
        mainPanel.add(center, BorderLayout.CENTER);

        connectBtn.addActionListener(e -> connect());
        disconnectBtn.addActionListener(e -> disconnect());
        paramBtn.addActionListener(e -> applyParameters());
        startBtn.addActionListener(e -> startTest());
        stopBtn.addActionListener(e -> stopTest());
        lightBtn.addActionListener(e -> toggleLight());

        GuiUtilities.darkenInputs(mainPanel);
    }

    private JLabel setTempFromCameraScreen() {
        JLabel l = new JLabel("--", SwingConstants.CENTER);
        l.setFont(new Font(Font.MONOSPACED, Font.BOLD, 26));
        l.setForeground(GREEN);
        service.addSetpointListener(sp -> SwingUtilities.invokeLater(() ->
                l.setText(String.format(Locale.US, "%.2f °C", sp))));
        return l;
    }

    private void connect() {
        String host = hostField.getText().trim();
        int port = ((Number) portSpinner.getValue()).intValue();
        connectBtn.setEnabled(false);
        statusLabel.setText("Подключение…");
        comLamp.setForeground(ORANGE);
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return service.connect(host, port);
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        connectBtn.setEnabled(false);
                        disconnectBtn.setEnabled(true);
                        comLamp.setForeground(GREEN);
                        service.setPollingEnabled(true);
                        announceToCamera();
                    } else {
                        connectBtn.setEnabled(true);
                        disconnectBtn.setEnabled(false);
                        comLamp.setForeground(RED);
                    }
                } catch (Exception ex) {
                    connectBtn.setEnabled(true);
                    comLamp.setForeground(RED);
                }
            }
        }.execute();
    }

    private void disconnect() {
        service.disconnect();
        connectBtn.setEnabled(true);
        disconnectBtn.setEnabled(false);
        comLamp.setForeground(GRAY);
        statusLabel.setText("Не подключено");
        modeLabel.setText("—");
    }

    private void announceToCamera() {
        service.setTargetTemperature(((Number) targetSpinner.getValue()).doubleValue());
    }

    private void applyParameters() {
        if (!service.isConnected()) {
            statusLabel.setText("Нет соединения");
            return;
        }
        double temp = ((Number) targetSpinner.getValue()).doubleValue();
        service.setTargetTemperature(temp);
    }

    private void startTest() {
        if (!service.isConnected()) {
            statusLabel.setText("Нет соединения");
            return;
        }
        double temp = ((Number) targetSpinner.getValue()).doubleValue();
        double ramp = maxSpeedCheck.isSelected()
                ? 0.0 : ((Number) rampSpinner.getValue()).doubleValue();
        double hum = ((Number) humiditySpinner.getValue()).doubleValue();
        service.startTest(temp, ramp, hum, wetCheck.isSelected());
        modeLabel.setText("Работа");
    }

    private void stopTest() {
        service.stopTest();
        modeLabel.setText("Остановлен");
    }

    private void toggleLight() {
        lightDesired = !lightDesired;
        service.setLight(lightDesired);
    }

    private void addLog(String line) {
        logLines.add(line);
        while (logLines.size() > MAX_LOG) {
            logLines.remove(0);
        }
        logArea.setText(String.join("\n", logLines));
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    // ─── helpers ─────────────────────────────────────────────────────────

    private static JPanel group(String title) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), title, TitledBorder.LEFT, TitledBorder.TOP));
        return p;
    }

    private static JPanel row(String text, Component... components) {
        JPanel r = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 1));
        r.setAlignmentX(Component.LEFT_ALIGNMENT);
        r.add(new JLabel(text));
        for (Component c : components) {
            r.add(c);
        }
        return r;
    }

    private static JLabel degIcon() {
        JLabel l = new JLabel("°C");
        l.setFont(new Font(Font.DIALOG, Font.PLAIN, 13));
        return l;
    }

    private static JLabel lamp(Color c) {
        JLabel l = new JLabel("●");
        l.setForeground(c);
        l.setHorizontalAlignment(SwingConstants.CENTER);
        return l;
    }

    private static JLabel screenLabel() {
        JLabel l = new JLabel("-- °C", SwingConstants.CENTER);
        l.setFont(new Font(Font.MONOSPACED, Font.BOLD, 34));
        l.setForeground(GREEN);
        return l;
    }

    private static JPanel screenBox(String title, JLabel value) {
        JPanel box = new JPanel(new BorderLayout());
        box.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.DARK_GRAY), title),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        box.add(value, BorderLayout.CENTER);
        return box;
    }

    /** Standalone-запуск панели управления. */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            TestaControlPanel panel = new TestaControlPanel();
            JFrame frame = new JFrame("Testa — Управление камерой (UDP)");
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            frame.setContentPane(panel.getMainPanel());
            frame.pack();
            frame.setSize(900, 640);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
