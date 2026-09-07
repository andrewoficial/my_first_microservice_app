package org.example.gui.devices.binder.camera.control;

import org.example.gui.utilites.GuiUtilities;

import javax.swing.*;
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

    private final JLabel currentScreen = screenLabel();
    private final JLabel setpointScreen = screenLabel();
    private JLabel humidityLamp;

    private final JTextArea logArea = new JTextArea();
    private final List<String> logLines = new CopyOnWriteArrayList<>();
    private static final int MAX_LOG = 300;

    private double lastTarget = Double.NaN;

    public BinderControlPanel() {
        buildUi();
        service.addTemperatureListener(t -> SwingUtilities.invokeLater(() -> {
            currentScreen.setText(String.format(Locale.US, "%.2f °C", t));
            chartData.addMeasured(t);
        }));
        service.addHumidityListener(on -> SwingUtilities.invokeLater(() -> setHumidityLamp(on)));
        service.addStatusListener(s -> SwingUtilities.invokeLater(() -> statusLabel.setText(s)));
        service.addLogListener(line -> SwingUtilities.invokeLater(() -> addLog(line)));

        GuiUtilities.darkenInputs(mainPanel);
    }

    public JPanel getMainPanel() {
        return mainPanel;
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
        currentScreen.setHorizontalAlignment(SwingConstants.CENTER);
        setpointScreen.setHorizontalAlignment(SwingConstants.CENTER);
        screens.add(panelBox("ТЕКУЩАЯ ТЕМПЕРАТУРА", currentScreen));
        screens.add(panelBox("УСТАВКА", setpointScreen));
        center.add(screens, BorderLayout.NORTH);
        center.add(new ControlChart(chartData), BorderLayout.CENTER);

        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Лог обмена данными (hex TX/RX)"));
        logScroll.setPreferredSize(new Dimension(400, 110));
        center.add(logScroll, BorderLayout.SOUTH);

        mainPanel.add(center, BorderLayout.CENTER);

        connectBtn.addActionListener(e -> connect());
        disconnectBtn.addActionListener(e -> disconnect());
        setBtn.addActionListener(e -> applySetpoint());
    }

    private JPanel createLeftPanel() {
        hostField = new JTextField("127.0.0.1", 12);
        portSpinner = new JSpinner(new SpinnerNumberModel(10001, 1, 65535, 1));
        slaveSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 247, 1));
        connectBtn = new JButton("Подключиться");
        disconnectBtn = new JButton("Отключиться");
        disconnectBtn.setEnabled(false);
        comLamp = lamp(GRAY);
        statusLabel = new JLabel("Не подключено");
        targetSpinner = new JSpinner(new SpinnerNumberModel(25.0, -40.0, 120.0, 0.5));
        setBtn = new JButton("Задать уставку (SetT)");
        humidityLamp = lamp(GRAY);

        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        p.setPreferredSize(new Dimension(320, 0));

        p.add(sectionLabel("Соединение"));
        p.add(label("host:"));
        p.add(fullWidth(hostField));
        JPanel hp = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        hp.setAlignmentX(Component.LEFT_ALIGNMENT);
        hp.add(new JLabel("port:"));
        hp.add(portSpinner);
        hp.add(new JLabel("slave:"));
        hp.add(slaveSpinner);
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
        p.add(sectionLabel("Уставка"));
        p.add(label("Уставка, °C:"));
        p.add(fullWidth(targetSpinner));
        p.add(Box.createVerticalStrut(4));
        setBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(setBtn);

        p.add(Box.createVerticalStrut(12));
        p.add(sectionLabel("Влажность"));
        JPanel humRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        humRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        humRow.add(new JLabel("Влажность:"));
        humRow.add(humidityLamp);
        p.add(humRow);

        p.add(Box.createVerticalGlue());
        return p;
    }

    private static JPanel panelBox(String title, JLabel value) {
        JPanel box = new JPanel(new BorderLayout());
        box.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.DARK_GRAY), title),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        box.add(value, BorderLayout.CENTER);
        return box;
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
