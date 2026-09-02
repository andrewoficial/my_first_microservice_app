package org.example.gui.devices.binder.camera.control;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Панель управления климатической камерой Binder (TCP-клиент).
 * Подключение к host:port, задание уставки (SetT), периодический опрос текущей
 * температуры (GetT) с влажностной авто-логикой (SetHC). «Экран задано/текущая»,
 * график и hex-дебаг.
 */
public class BinderControlPanel {

    private static final Color GREEN = new Color(0, 160, 0);
    private static final Color RED = new Color(200, 40, 40);
    private static final Color GRAY = Color.DARK_GRAY;
    private static final Color ORANGE = new Color(200, 120, 0);

    private final BinderCommunicationService service = new BinderCommunicationService();

    private JPanel mainPanel;

    private JTextField hostField;
    private JSpinner portSpinner;
    private JSpinner slaveSpinner;
    private JButton connectBtn;
    private JButton disconnectBtn;
    private JLabel statusLabel;
    private JLabel comLamp;

    private JSpinner targetSpinner;
    private JButton setBtn;

    private final JLabel setpointScreen = screenLabel();
    private final JLabel measuredScreen = screenLabel();
    private JLabel humidityLamp;

    private final JTextArea logArea = new JTextArea();
    private final List<String> logLines = new CopyOnWriteArrayList<>();
    private static final int MAX_LOG = 300;

    private double lastTarget = Double.NaN;

    public BinderControlPanel() {
        buildUi();
        service.addTemperatureListener(t -> SwingUtilities.invokeLater(() -> {
            measuredScreen.setText(String.format(Locale.US, "%.2f °C", t));
            chartData.addMeasured(t);
        }));
        service.addHumidityListener(on -> SwingUtilities.invokeLater(() -> setHumidityLamp(on)));
        service.addStatusListener(s -> SwingUtilities.invokeLater(() -> statusLabel.setText(s)));
        service.addLogListener(line -> SwingUtilities.invokeLater(() -> addLog(line)));
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    private void buildUi() {
        mainPanel = new JPanel(new BorderLayout(8, 8));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Соединение и уставка",
                TitledBorder.LEFT, TitledBorder.TOP));

        // строка подключения
        hostField = new JTextField("127.0.0.1", 10);
        portSpinner = new JSpinner(new SpinnerNumberModel(10001, 1, 65535, 1));
        slaveSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 247, 1));
        connectBtn = new JButton("Подключиться");
        disconnectBtn = new JButton("Отключиться");
        disconnectBtn.setEnabled(false);
        comLamp = lamp(GRAY);
        statusLabel = new JLabel("Не подключено");

        JPanel connRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        connRow.add(new JLabel("host:"));
        connRow.add(hostField);
        connRow.add(new JLabel("port:"));
        connRow.add(portSpinner);
        connRow.add(new JLabel("slave:"));
        connRow.add(slaveSpinner);
        connRow.add(connectBtn);
        connRow.add(disconnectBtn);
        connRow.add(comLamp);
        connRow.add(statusLabel);
        north.add(connRow);

        // строка уставки
        targetSpinner = new JSpinner(new SpinnerNumberModel(25.0, -40.0, 120.0, 0.5));
        setBtn = new JButton("Задать уставку (SetT)");
        humidityLamp = lamp(GRAY);
        JLabel humidityLbl = new JLabel("Влажность:");
        JPanel setRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        setRow.add(new JLabel("Уставка, °C:"));
        setRow.add(targetSpinner);
        setRow.add(setBtn);
        setRow.add(humidityLbl);
        setRow.add(humidityLamp);
        north.add(setRow);

        // центр: экраны + график + лог
        JPanel center = new JPanel(new BorderLayout(8, 8));
        JPanel screens = new JPanel(new GridLayout(1, 2, 12, 0));
        screens.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        screens.add(screenBox("ЭКРАН ЗАДАНО", setpointScreen));
        screens.add(screenBox("ЭКРАН ТЕКУЩАЯ", measuredScreen));
        center.add(screens, BorderLayout.NORTH);
        center.add(new ControlChart(chartData), BorderLayout.CENTER);

        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Дебаг (hex TX/RX)"));
        logScroll.setPreferredSize(new Dimension(400, 150));
        center.add(logScroll, BorderLayout.SOUTH);

        mainPanel.add(north, BorderLayout.NORTH);
        mainPanel.add(center, BorderLayout.CENTER);

        connectBtn.addActionListener(e -> connect());
        disconnectBtn.addActionListener(e -> disconnect());
        setBtn.addActionListener(e -> applySetpoint());

        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
    }

    private void connect() {
        String host = hostField.getText().trim();
        int port = ((Number) portSpinner.getValue()).intValue();
        int slave = ((Number) slaveSpinner.getValue()).intValue();
        connectBtn.setEnabled(false);
        statusLabel.setText("Подключение…");
        comLamp.setForeground(ORANGE);
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return service.connect(host, port, slave);
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        connectBtn.setEnabled(false);
                        disconnectBtn.setEnabled(true);
                        comLamp.setForeground(GREEN);
                        service.setPollingEnabled(true);
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
        service.setPollingEnabled(false);
        service.disconnect();
        connectBtn.setEnabled(true);
        disconnectBtn.setEnabled(false);
        comLamp.setForeground(GRAY);
        statusLabel.setText("Не подключено");
    }

    private void applySetpoint() {
        if (!service.isConnected()) {
            statusLabel.setText("Нет соединения — подключитесь к эмулятору/камере");
            return;
        }
        double target = ((Number) targetSpinner.getValue()).doubleValue();
        lastTarget = target;
        setpointScreen.setText(String.format(Locale.US, "%.2f °C", target));
        chartData.addTarget(target);
        service.setTargetTemperature((float) target);
    }

    private void setHumidityLamp(boolean on) {
        humidityLamp.setForeground(on ? GREEN : GRAY);
        humidityLamp.setToolTipText(on ? "Влажность ВКЛ" : "Влажность ВЫКЛ");
    }

    private void addLog(String line) {
        logLines.add(line);
        while (logLines.size() > MAX_LOG) {
            logLines.remove(0);
        }
        logArea.setText(String.join("\n", logLines));
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    // ─── UI helpers ───────────────────────────────────────────────────────

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
            BinderControlPanel panel = new BinderControlPanel();
            JFrame frame = new JFrame("Binder — Управление камерой (TCP)");
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            frame.setContentPane(panel.getMainPanel());
            frame.pack();
            frame.setSize(1000, 680);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
