package org.example.gui.devices.testa.control;

import org.example.gui.utilites.GuiUtilities;

import javax.swing.*;
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
        service.addTemperatureListener(t -> SwingUtilities.invokeLater(() -> {
            curTempScreen.setText(String.format(Locale.US, "%.2f °C", t));
            chartData.addMeasured(t);
        }));
        service.addSetpointListener(sp -> SwingUtilities.invokeLater(() -> {
            setTempFromCamera.setText(String.format(Locale.US, "%.2f °C", sp));
            chartData.addTarget(sp);
        }));
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

        JScrollPane leftScroll = new JScrollPane(createLeftPanel());
        leftScroll.setBorder(BorderFactory.createEmptyBorder());
        leftScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        leftScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        mainPanel.add(leftScroll, BorderLayout.WEST);

        // центр: экраны + график + скромный лог
        JPanel center = new JPanel(new BorderLayout(8, 8));
        JPanel screens = new JPanel(new GridLayout(1, 2, 12, 0));
        screens.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        curTempScreen = screenLabel();
        screens.add(screenBox("ТЕКУЩАЯ ТЕМПЕРАТУРА", curTempScreen));
        screens.add(screenBox("УСТАВКА", setTempFromCameraScreen()));
        center.add(screens, BorderLayout.NORTH);
        center.add(new ControlChart(chartData), BorderLayout.CENTER);

        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Лог обмена данными (UDP TX/RX)"));
        logScroll.setPreferredSize(new Dimension(400, 110));
        center.add(logScroll, BorderLayout.SOUTH);

        mainPanel.add(center, BorderLayout.CENTER);

        connectBtn.addActionListener(e -> connect());
        disconnectBtn.addActionListener(e -> disconnect());
        paramBtn.addActionListener(e -> applyParameters());
        startBtn.addActionListener(e -> startTest());
        stopBtn.addActionListener(e -> stopTest());
        lightBtn.addActionListener(e -> toggleLight());

        GuiUtilities.darkenInputs(mainPanel);
    }

    private JPanel createLeftPanel() {
        hostField = new JTextField("127.0.0.1", 10);
        portSpinner = new JSpinner(new SpinnerNumberModel(1300, 1, 65535, 1));
        connectBtn = new JButton("Подключиться");
        disconnectBtn = new JButton("Отключиться");
        disconnectBtn.setEnabled(false);
        comLamp = lamp(GRAY);
        statusLabel = new JLabel("Не подключено");

        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        p.setPreferredSize(new Dimension(340, 0));

        p.add(sectionLabel("Соединение"));
        p.add(label("Камера host:"));
        p.add(fullWidth(hostField));
        JPanel hp = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        hp.setAlignmentX(Component.LEFT_ALIGNMENT);
        hp.add(new JLabel("port:"));
        hp.add(portSpinner);
        p.add(hp);
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        btns.setAlignmentX(Component.LEFT_ALIGNMENT);
        btns.add(connectBtn);
        btns.add(disconnectBtn);
        btns.add(comLamp);
        p.add(btns);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(statusLabel);

        p.add(Box.createVerticalStrut(12));
        p.add(sectionLabel("Температура"));
        targetSpinner = new JSpinner(new SpinnerNumberModel(25.0, -40.0, 120.0, 0.5));
        maxSpeedCheck = new JCheckBox("работать с максимальной скоростью", true);
        rampSpinner = new JSpinner(new SpinnerNumberModel(5.0, 0.1, 60.0, 0.5));
        rampSpinner.setEnabled(false);
        maxSpeedCheck.addActionListener(e -> rampSpinner.setEnabled(!maxSpeedCheck.isSelected()));
        p.add(label("Установка температуры (°C):"));
        p.add(fullWidth(targetSpinner));
        maxSpeedCheck.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(maxSpeedCheck);
        JPanel spRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        spRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        spRow.add(new JLabel("скорость выхода:"));
        spRow.add(rampSpinner);
        spRow.add(new JLabel("°C/мин"));
        p.add(spRow);

        p.add(Box.createVerticalStrut(12));
        p.add(sectionLabel("Влажность"));
        wetCheck = new JCheckBox("работа с влагой", true);
        humiditySpinner = new JSpinner(new SpinnerNumberModel(50.0, 0.0, 100.0, 1.0));
        wetCheck.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(wetCheck);
        p.add(label("Установка влажности, %RH:"));
        p.add(fullWidth(humiditySpinner));
        p.add(label("Установленная влажность:"));
        p.add(fullWidth(humSetFromCamera));
        p.add(label("Текущая влажность:"));
        p.add(fullWidth(humCurLabel));

        p.add(Box.createVerticalStrut(12));
        p.add(sectionLabel("Подсветка"));
        JPanel lr = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        lr.setAlignmentX(Component.LEFT_ALIGNMENT);
        lr.add(lightBtn);
        lr.add(new JLabel("состояние:"));
        lr.add(lightState);
        p.add(lr);

        p.add(Box.createVerticalStrut(12));
        p.add(sectionLabel("Управление"));
        JPanel cr = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        cr.setAlignmentX(Component.LEFT_ALIGNMENT);
        cr.add(paramBtn);
        cr.add(startBtn);
        cr.add(stopBtn);
        p.add(cr);
        JPanel mr = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        mr.setAlignmentX(Component.LEFT_ALIGNMENT);
        mr.add(new JLabel("режим:"));
        mr.add(modeLabel);
        p.add(mr);
        alarmState.setFont(new Font(Font.DIALOG, Font.BOLD, 13));
        alarmState.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(alarmState);

        p.add(Box.createVerticalGlue());
        return p;
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

    // ─── график ───────────────────────────────────────────────────────────

    private final LiveChart chartData = new LiveChart();

    private static final class ControlChart extends JPanel {
        ControlChart(LiveChart data) {
            super(new BorderLayout());
            org.jfree.data.xy.XYSeriesCollection ds = new org.jfree.data.xy.XYSeriesCollection();
            ds.addSeries(data.targetSeries);
            ds.addSeries(data.measSeries);
            org.jfree.chart.JFreeChart chart = org.jfree.chart.ChartFactory.createXYLineChart(
                    "Опрос камеры", "время, с", "°C", ds,
                    org.jfree.chart.plot.PlotOrientation.VERTICAL, true, true, false);
            add(new org.jfree.chart.ChartPanel(chart), BorderLayout.CENTER);
        }
    }

    private static final class LiveChart {
        private double t = 0;
        final org.jfree.data.xy.XYSeries targetSeries = new org.jfree.data.xy.XYSeries("Задано");
        final org.jfree.data.xy.XYSeries measSeries = new org.jfree.data.xy.XYSeries("Текущая");

        synchronized void addTarget(double v) {
            t += 0.1;
            add(targetSeries, v);
        }

        synchronized void addMeasured(double v) {
            t += 0.1;
            add(measSeries, v);
        }

        private void add(org.jfree.data.xy.XYSeries s, double v) {
            s.add(t, v);
            while (t > 600 && s.getItemCount() > 0 && t - s.getX(0).doubleValue() > 600) {
                s.remove(0);
            }
        }
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
