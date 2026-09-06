package org.example.gui.devices.binder.camera.emulation;

import org.example.gui.utilites.GuiUtilities;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Панель эмулятора климатической камеры Binder (TCP).
 * «Экран задано» / «Экран текущая», настройки выхода на режим, график и hex-лог.
 */
public class BinderEmulationPanel extends JPanel {

    private final BinderEmulator emulator = new BinderEmulator();
    private final BinderServerService server = new BinderServerService(emulator);

    private final JSpinner portSpinner = new JSpinner(new SpinnerNumberModel(10001, 1, 65535, 1));
    private final JSpinner slaveSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 247, 1));
    private final JButton startBtn = new JButton("Старт");
    private final JButton stopBtn = new JButton("Стоп");

    private final JSpinner setpointSpinner = new JSpinner(new SpinnerNumberModel(25.0, -40.0, 120.0, 1.0));
    private final JButton applyBtn = new JButton("Применить уставку");

    private final JSpinner rampSpinner = new JSpinner(new SpinnerNumberModel(5.0, 0.1, 30.0, 0.1));
    private final JSpinner overshootSpinner = new JSpinner(new SpinnerNumberModel(3.0, 0.0, 25.0, 0.1));
    private final JSpinner fluctAmpSpinner = new JSpinner(new SpinnerNumberModel(0.4, 0.0, 10.0, 0.1));
    private final JSpinner fluctPeriodSpinner = new JSpinner(new SpinnerNumberModel(20.0, 1.0, 300.0, 1.0));
    private final JCheckBox humidityCb = new JCheckBox("Управление влажностью", false);

    private final JLabel setpointScreen = screenLabel();
    private final JLabel measuredScreen = screenLabel();
    private final JLabel statusLabel = new JLabel("Эмулятор остановлен");
    private final JTextArea logArea = new JTextArea();

    private final javax.swing.Timer simTimer;
    private long lastTickNanos = System.nanoTime();

    private final List<String> logLines = new CopyOnWriteArrayList<>();
    private static final int MAX_LOG = 300;

    public BinderEmulationPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Эмулятор Binder (TCP 10001)",
                TitledBorder.LEFT, TitledBorder.TOP));

        add(createLeftPanel(), BorderLayout.WEST);
        add(createCenterPanel(), BorderLayout.CENTER);

        startBtn.addActionListener(e -> start());
        stopBtn.addActionListener(e -> stop());
        stopBtn.setEnabled(false);
        applyBtn.addActionListener(e -> applySetpoint());
        humidityCb.addActionListener(e -> emulator.setHumidityControl(humidityCb.isSelected()));
        bindDynamicsSpinners();

        server.addLogListener(this::addLog);
        server.addConnectionListener(conn -> SwingUtilities.invokeLater(() ->
                statusLabel.setText(conn ? "Клиент подключён" : "Ожидание клиента")));

        simTimer = new javax.swing.Timer(100, e -> advanceSim());
        simTimer.start();
        refreshScreens();
        syncHumidityFromEmulator();

        GuiUtilities.darkenInputs(this);
    }

    private void bindDynamicsSpinners() {
        rampSpinner.addChangeListener(e -> emulator.setRampRateDegPerMin(num(rampSpinner)));
        overshootSpinner.addChangeListener(e -> emulator.setOvershootDeg(num(overshootSpinner)));
        fluctAmpSpinner.addChangeListener(e -> emulator.setFluctuationAmpDeg(num(fluctAmpSpinner)));
        fluctPeriodSpinner.addChangeListener(e -> emulator.setFluctuationPeriodSec(num(fluctPeriodSpinner)));
    }

    private void advanceSim() {
        long now = System.nanoTime();
        double dtSec = (now - lastTickNanos) / 1_000_000_000.0;
        lastTickNanos = now;
        emulator.advance(Math.min(dtSec, 0.5));
        refreshScreens();
    }

    private void refreshScreens() {
        double set = emulator.getSetpoint();
        double meas = emulator.getMeasuredTemperature();
        setpointScreen.setText(String.format(Locale.US, "%.2f °C", set));
        measuredScreen.setText(String.format(Locale.US, "%.2f °C", meas));
        measuredScreen.setForeground(set < meas - 0.3 ? new Color(200, 90, 0)
                : set > meas + 0.3 ? new Color(0, 90, 180) : new Color(0, 140, 0));
        chartData.addPoint(set, meas);
        syncHumidityFromEmulator();
    }

    private void syncHumidityFromEmulator() {
        boolean on = emulator.isHumidityControlOn();
        if (humidityCb.isSelected() != on) {
            humidityCb.setSelected(on);
        }
    }

    private void applySetpoint() {
        emulator.setSetpoint((float) num(setpointSpinner));
        addLog("Уставка вручную → " + String.format(Locale.US, "%.2f", num(setpointSpinner)) + " °C");
    }

    private void start() {
        int port = ((Number) portSpinner.getValue()).intValue();
        emulator.setFluctuationPeriodSec(num(fluctPeriodSpinner));
        if (server.start(port)) {
            startBtn.setEnabled(false);
            stopBtn.setEnabled(true);
            statusLabel.setText("Эмулятор слушает порт " + port);
            emulator.setSetpoint((float) num(setpointSpinner));
            addLog("Сервер запущен на порту " + port);
        } else {
            statusLabel.setText("Ошибка запуска сервера");
        }
    }

    private void stop() {
        server.stop();
        startBtn.setEnabled(true);
        stopBtn.setEnabled(false);
        statusLabel.setText("Эмулятор остановлен");
    }

    // ─── UI построение ────────────────────────────────────────────────────

    private JPanel createLeftPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        p.setPreferredSize(new Dimension(300, 0));

        p.add(label("TCP порт (камера-сервер)"));
        p.add(portSpinner);
        p.add(Box.createVerticalStrut(4));
        p.add(label("slaveId"));
        p.add(slaveSpinner);
        p.add(Box.createVerticalStrut(6));

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        btns.setAlignmentX(Component.LEFT_ALIGNMENT);
        btns.add(startBtn);
        btns.add(stopBtn);
        p.add(btns);
        p.add(Box.createVerticalStrut(4));

        p.add(new JLabel("Статус:"));
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(statusLabel);

        p.add(Box.createVerticalStrut(16));
        p.add(sectionLabel("Выход на режим"));
        p.add(label("Скорость выхода, °C/мин"));
        p.add(rampSpinner);
        p.add(Box.createVerticalStrut(4));
        p.add(label("Оверхед (перелёт), °C"));
        p.add(overshootSpinner);
        p.add(Box.createVerticalStrut(4));
        p.add(label("Размер флуктуации на полке, °C"));
        p.add(fluctAmpSpinner);
        p.add(Box.createVerticalStrut(4));
        p.add(label("Скорость флуктуации (период), с"));
        p.add(fluctPeriodSpinner);

        p.add(Box.createVerticalStrut(16));
        p.add(sectionLabel("Уставка (вручную)"));
        p.add(setpointSpinner);
        p.add(Box.createVerticalStrut(4));
        applyBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(applyBtn);
        p.add(Box.createVerticalStrut(10));
        humidityCb.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(humidityCb);

        p.add(Box.createVerticalGlue());
        for (Component c : p.getComponents()) {
            if (c instanceof JComponent jc && !(c instanceof JPanel)) {
                jc.setAlignmentX(Component.LEFT_ALIGNMENT);
                jc.setMaximumSize(new Dimension(Integer.MAX_VALUE, jc.getPreferredSize().height));
            }
        }
        return p;
    }

    private JPanel createCenterPanel() {
        JPanel center = new JPanel(new BorderLayout(8, 8));

        JPanel screens = new JPanel(new GridLayout(1, 2, 12, 0));
        screens.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        screens.add(screenBox("ЭКРАН ЗАДАНО", setpointScreen));
        screens.add(screenBox("ЭКРАН ТЕКУЩАЯ", measuredScreen));

        center.add(screens, BorderLayout.NORTH);
        center.add(new ChartPanel2(chartData), BorderLayout.CENTER);

        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Дебаг (hex RX/TX)"));
        logScroll.setPreferredSize(new Dimension(400, 160));
        center.add(logScroll, BorderLayout.SOUTH);
        return center;
    }

    private static JPanel screenBox(String title, JLabel value) {
        JPanel box = new JPanel(new BorderLayout());
        box.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.DARK_GRAY), title),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        value.setHorizontalAlignment(SwingConstants.CENTER);
        box.add(value, BorderLayout.CENTER);
        return box;
    }

    private static JLabel screenLabel() {
        JLabel l = new JLabel("-- °C", SwingConstants.CENTER);
        l.setFont(new Font(Font.MONOSPACED, Font.BOLD, 34));
        l.setForeground(new Color(0, 140, 0));
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
        Font f = l.getFont().deriveFont(Font.BOLD);
        l.setFont(f);
        return l;
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

    private static double num(JSpinner s) {
        return ((Number) s.getValue()).doubleValue();
    }

    public void shutdown() {
        simTimer.stop();
        server.stop();
    }

    // ─── график ───────────────────────────────────────────────────────────

    private final LiveChartData chartData = new LiveChartData();

    @SuppressWarnings("unchecked")
    private static final class ChartPanel2 extends JPanel {
        ChartPanel2(LiveChartData data) {
            super(new BorderLayout());
            org.jfree.data.xy.XYSeriesCollection ds = new org.jfree.data.xy.XYSeriesCollection();
            ds.addSeries(data.setSeries);
            ds.addSeries(data.measSeries);
            org.jfree.chart.JFreeChart chart = org.jfree.chart.ChartFactory.createXYLineChart(
                    "Выход на режим", "время, с", "°C", ds,
                    org.jfree.chart.plot.PlotOrientation.VERTICAL, true, true, false);
            add(new org.jfree.chart.ChartPanel(chart), BorderLayout.CENTER);
        }
    }

    private static final class LiveChartData {
        private double t = 0;
        final org.jfree.data.xy.XYSeries setSeries =
                new org.jfree.data.xy.XYSeries("Задано");
        final org.jfree.data.xy.XYSeries measSeries =
                new org.jfree.data.xy.XYSeries("Текущая");

        void addPoint(double set, double meas) {
            t += 0.1;
            setSeries.add(t, set);
            measSeries.add(t, meas);
            if (t > 600) {
                trim(setSeries, t);
                trim(measSeries, t);
            }
        }

        void trim(org.jfree.data.xy.XYSeries s, double now) {
            while (s.getItemCount() > 0
                    && now - s.getX(0).doubleValue() > 600) {
                s.remove(0);
            }
        }
    }
}
