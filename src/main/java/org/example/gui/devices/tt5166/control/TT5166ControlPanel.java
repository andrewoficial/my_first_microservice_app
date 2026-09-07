package org.example.gui.devices.tt5166.control;

import com.fazecast.jSerialComm.SerialPort;
import lombok.extern.slf4j.Slf4j;
import org.example.device.protTt5166.TT5166CommandRegistry;
import org.example.gui.utilites.GuiUtilities;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.Locale;

/**
 * Панель управления климатической камерой TT5166 (Modbus RTU, 38400 8E1).
 */
@Slf4j
public class TT5166ControlPanel extends JPanel {

    private final TT5166CommandRegistry reg = new TT5166CommandRegistry();
    private final TT5166ModbusCommunicationService service = new TT5166ModbusCommunicationService();

    private final JComboBox<String> portCombo = new JComboBox<>();
    private final JButton refreshBtn = new JButton("Обновить");
    private final JButton openBtn = new JButton("Подключить");
    private final JButton closeBtn = new JButton("Отключить");
    private final JLabel statusLabel = new JLabel("Отключено");

    private final JLabel tempScreen = screenLabel("-- °C");
    private final JLabel setpointScreen = screenLabel("-- °C");
    private final JLabel humScreen = screenLabel("-- %");
    private final JLabel humSetpointScreen = screenLabel("-- %");
    private final JSpinner setpointSpinner = new JSpinner(new SpinnerNumberModel(25.0, -100.0, 200.0, 0.5));
    private final JSpinner humSpinner = new JSpinner(new SpinnerNumberModel(50.0, 0.0, 100.0, 1.0));
    private final JButton setSetpointBtn = new JButton("Записать уставку темп.");
    private final JButton setHumBtn = new JButton("Записать уставку влаги");
    private final JCheckBox pollingCheck = new JCheckBox("Автоопрос (1 с)", true);
    private final JButton queryDataBtn = new JButton("Запросить данные");
    private final JButton queryStateBtn = new JButton("Запросить состояние");
    private final JButton queryFaultBtn = new JButton("Запросить ошибку");
    private final JButton startBtn = new JButton("СТАРТ");
    private final JButton stopBtn = new JButton("СТОП");
    private final JTextArea logArea = new JTextArea();

    private static final Color LAMP_OFF = new Color(70, 70, 70);
    private static final Color LAMP_RUN = new Color(0, 180, 0);
    private static final Color LAMP_MODE = new Color(255, 170, 0);
    private static final Color LAMP_FAULT = new Color(255, 60, 60);

    private final LampIndicator runLamp = new LampIndicator();
    private final LampIndicator modeLamp = new LampIndicator();
    private final LampIndicator faultLamp = new LampIndicator();
    private final JLabel faultInfoLabel = label("Ошибок нет");

    private volatile boolean currentOn = false;
    private final java.util.concurrent.atomic.AtomicInteger pollCounter = new java.util.concurrent.atomic.AtomicInteger();

    public TT5166ControlPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Управление TT5166 (Modbus RTU, 38400 8E1)",
                TitledBorder.LEFT, TitledBorder.TOP));

        add(createLeftPanel(), BorderLayout.WEST);
        add(createCenterPanel(), BorderLayout.CENTER);

        humScreen.setForeground(new Color(60, 150, 255));
        humSetpointScreen.setForeground(new Color(60, 150, 255));

        service.addStatusListener(s -> SwingUtilities.invokeLater(() -> statusLabel.setText(s)));
        service.addLogListener(l -> SwingUtilities.invokeLater(() -> appendLog(l)));
        service.addResponseListener(frame -> SwingUtilities.invokeLater(() -> handleResponse(frame)));

        refreshBtn.addActionListener(e -> refreshPorts());
        openBtn.addActionListener(e -> openPort());
        closeBtn.addActionListener(e -> closePort());
        setSetpointBtn.addActionListener(e -> writeSetpoint());
        setHumBtn.addActionListener(e -> writeHumidity());
        queryDataBtn.addActionListener(e -> queryData());
        queryStateBtn.addActionListener(e -> queryState());
        queryFaultBtn.addActionListener(e -> queryFault());
        startBtn.addActionListener(e -> sendStart(true));
        stopBtn.addActionListener(e -> sendStart(false));
        pollingCheck.addActionListener(e -> service.setPollingEnabled(pollingCheck.isSelected(), this::pollTick));

        GuiUtilities.darkenInputs(this);
    }

    // ─── Modbus helpers ─────────────────────────────────────────────

    private void queryData() {
        byte[] frame = reg.buildReadRegisters((byte) 0x01, 0x0000, 6);
        service.sendRequest(frame);
        appendLog("Запрос данных (рег 0x0000, 6)");
    }

    private void queryFault() {
        byte[] frame = reg.buildReadRegisters((byte) 0x01, 0x001B, 1);
        service.sendRequest(frame);
        appendLog("Запрос ошибки (рег 0x001B)");
    }

    private void queryState() {
        byte[] frame = reg.buildReadRegisters((byte) 0x01, 0x0018, 2);
        service.sendRequest(frame);
        appendLog("Запрос состояния (рег 0x0018, 2)");
    }

    private void pollTick() {
        if (!service.isConnected()) return;
        switch (pollCounter.getAndIncrement() % 4) {
            case 0: queryData(); break;
            case 1: queryState(); break;
            case 2: queryData(); break;
            default: queryFault(); break;
        }
    }

    private void writeSetpoint() {
        double v = ((Number) setpointSpinner.getValue()).doubleValue();
        short raw = (short) Math.round(v * 10);
        service.sendRequest(reg.buildWriteRegister((byte) 0x01, 0x0026, raw));
        appendLog("Запись уставки темп.: " + String.format(Locale.US, "%.1f", v) + " °C (raw " + raw + ")");
    }

    private void writeHumidity() {
        double v = ((Number) humSpinner.getValue()).doubleValue();
        short raw = (short) Math.round(v * 10);
        service.sendRequest(reg.buildWriteRegister((byte) 0x01, 0x0027, raw));
        appendLog("Запись уставки влаги: " + String.format(Locale.US, "%.1f", v) + " % (raw " + raw + ")");
    }

    private void sendStart(boolean on) {
        service.sendRequest(reg.buildForceCoil((byte) 0x01, on ? 0x0000 : 0x0001, true));
        appendLog(on ? "Команда СТАРТ" : "Команда СТОП");
        // Немедленный опрос состояния, чтобы бит работы (H0018) обновился сразу.
        javax.swing.Timer t = new javax.swing.Timer(150, e -> { pollCounter.set(1); queryState(); });
        t.setRepeats(false);
        t.start();
    }

    private void handleResponse(byte[] frame) {
        if (frame == null || frame.length < 4) return;
        int func = frame[1] & 0xFF;
        if (func == 0x03) {
            if (frame[2] == 12) {
                org.example.services.AnswerValues vals = reg.parseDataResponse(frame);
                if (vals != null) {
                    double tempPv = vals.getValues()[0];
                    double tempSv = vals.getValues()[1];
                    double humPv  = vals.getValues()[3];
                    double humSv  = vals.getValues()[4];
                    tempScreen.setText(String.format(Locale.US, "%.2f °C", tempPv));
                    tempScreen.setForeground(new Color(0, 140, 0));
                    setpointScreen.setText(String.format(Locale.US, "%.1f °C", tempSv));
                    setpointScreen.setForeground(new Color(0, 140, 0));
                    humScreen.setText(String.format(Locale.US, "%.1f %%", humPv));
                    humSetpointScreen.setText(String.format(Locale.US, "%.1f %%", humSv));
                    chartData.addTarget(tempSv);
                    chartData.addMeasured(tempPv);
                    chartData.addHumTarget(humSv);
                    chartData.addHumMeasured(humPv);
                    appendLog(String.format(Locale.US,
                            "PV %.2f °C / SV %.1f °C / влаж. %.1f %% (SV %.1f %%)", tempPv, tempSv, humPv, humSv));
                }
            } else if (frame[2] == 4) {
                org.example.services.AnswerValues vals = reg.parseStateResponse(frame);
                if (vals != null) {
                    boolean running = ((int) vals.getValues()[0] & 0x0001) == 1;
                    boolean prog   = ((int) vals.getValues()[1] & 0x0001) == 1;
                    runLamp.setLampColor(running ? LAMP_RUN : LAMP_OFF);
                    modeLamp.setLampColor(prog ? LAMP_MODE : LAMP_OFF);
                    currentOn = running;
                    appendLog("Состояние: " + (running ? "работает" : "остановлена")
                            + ", режим: " + (prog ? "программный" : "фиксированный"));
                }
            } else if (frame[2] == 2) {
                org.example.services.AnswerValues vals = reg.parseFaultResponse(frame);
                if (vals != null) {
                    int code = (int) vals.getValues()[0];
                    boolean fault = code != 0;
                    faultLamp.setLampColor(fault ? LAMP_FAULT : LAMP_OFF);
                    faultInfoLabel.setText(fault
                            ? "Код ошибки: " + code + " — " + vals.getUnits()[0]
                            : "Ошибок нет");
                    appendLog("Код ошибки: " + code + " — " + vals.getUnits()[0]);
                }
            }
        } else if (func == 0x05 || func == 0x06) {
            org.example.services.AnswerValues vals = reg.parseAckResponse(frame);
            if (vals != null && vals.getValues()[0] > 0) {
                if (func == 0x05) {
                    currentOn = !currentOn;
                    runLamp.setLampColor(currentOn ? LAMP_RUN : LAMP_OFF);
                    appendLog("Состояние: " + (currentOn ? "ВКЛ" : "ВЫКЛ"));
                } else {
                    appendLog("Уставка записана (reg)");
                }
            }
        }
    }

    // ─── Connection ─────────────────────────────────────────────────

    private void openPort() {
        String selected = (String) portCombo.getSelectedItem();
        if (selected == null || selected.contains("Нет")) {
            JOptionPane.showMessageDialog(this, "Выберите COM-порт");
            return;
        }
        String portName = selected.split(" — ")[0].trim();
        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() { return service.open(portName); }
            @Override protected void done() {
                try {
                    if (get()) {
                        openBtn.setEnabled(false);
                        closeBtn.setEnabled(true);
                        pollCounter.set(0);
                        service.setPollingEnabled(pollingCheck.isSelected(), TT5166ControlPanel.this::pollTick);
                    }
                } catch (Exception ex) { appendLog("Ошибка: " + ex.getMessage()); }
            }
        }.execute();
    }

    private void closePort() {
        service.setPollingEnabled(false, null);
        service.close();
        openBtn.setEnabled(true);
        closeBtn.setEnabled(false);
        tempScreen.setText("-- °C");
        tempScreen.setForeground(new Color(160, 160, 160));
        setpointScreen.setText("-- °C");
        setpointScreen.setForeground(new Color(160, 160, 160));
        humScreen.setText("-- %");
        humScreen.setForeground(new Color(60, 150, 255));
        humSetpointScreen.setText("-- %");
        humSetpointScreen.setForeground(new Color(60, 150, 255));
        runLamp.setLampColor(LAMP_OFF);
        modeLamp.setLampColor(LAMP_OFF);
        faultLamp.setLampColor(LAMP_OFF);
        faultInfoLabel.setText("Ошибок нет");
    }

    // ─── UI ─────────────────────────────────────────────────────────

    private JPanel createLeftPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        p.setPreferredSize(new Dimension(320, 0));

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
        p.add(sectionLabel("Управление"));

        p.add(label("Уставка темп., °C (0x0026)"));
        p.add(fullWidth(setpointSpinner));
        p.add(Box.createVerticalStrut(4));
        setSetpointBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(setSetpointBtn);
        p.add(Box.createVerticalStrut(8));

        p.add(label("Уставка влаги, % (0x0027)"));
        p.add(fullWidth(humSpinner));
        p.add(Box.createVerticalStrut(4));
        setHumBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(setHumBtn);
        p.add(Box.createVerticalStrut(8));

        JPanel runRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        runRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        startBtn.setBackground(new Color(0, 140, 0));
        stopBtn.setBackground(new Color(180, 60, 60));
        runRow.add(startBtn);
        runRow.add(stopBtn);
        p.add(runRow);
        p.add(Box.createVerticalStrut(8));

        queryDataBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(queryDataBtn);
        p.add(Box.createVerticalStrut(4));
        queryStateBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(queryStateBtn);
        p.add(Box.createVerticalStrut(4));
        queryFaultBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(queryFaultBtn);
        p.add(Box.createVerticalStrut(4));

        pollingCheck.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(pollingCheck);

        p.add(Box.createVerticalStrut(12));
        p.add(sectionLabel("Статус / ошибки"));
        p.add(flagRow(runLamp, "Работает (H0018 бит 0)"));
        p.add(flagRow(modeLamp, "Программный режим (H0019 бит 0)"));
        p.add(flagRow(faultLamp, "Ошибка (рег 0x001B)"));
        faultInfoLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, 11));
        p.add(faultInfoLabel);

        p.add(Box.createVerticalGlue());
        return p;
    }

    private JPanel createCenterPanel() {
        JPanel center = new JPanel(new BorderLayout(8, 8));
        center.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel screens = new JPanel(new GridLayout(1, 4, 8, 0));
        tempScreen.setHorizontalAlignment(SwingConstants.CENTER);
        setpointScreen.setHorizontalAlignment(SwingConstants.CENTER);
        humScreen.setHorizontalAlignment(SwingConstants.CENTER);
        humSetpointScreen.setHorizontalAlignment(SwingConstants.CENTER);
        screens.add(panelBox("ТЕКУЩАЯ ТЕМПЕРАТУРА", tempScreen));
        screens.add(panelBox("ТЕКУЩАЯ ВЛАЖНОСТЬ", humScreen));
        screens.add(panelBox("УСТАВКА ТЕМПЕРАТУРЫ", setpointScreen));
        screens.add(panelBox("УСТАВКА ВЛАЖНОСТИ", humSetpointScreen));
        center.add(screens, BorderLayout.NORTH);
        center.add(new ControlChart(chartData), BorderLayout.CENTER);

        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Лог обмена данными (hex TX/RX)"));
        logScroll.setPreferredSize(new Dimension(400, 110));
        center.add(logScroll, BorderLayout.SOUTH);

        return center;
    }

    private static JPanel panelBox(String title, JLabel value) {
        JPanel box = new JPanel(new BorderLayout());
        box.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.DARK_GRAY), title),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        box.add(value, BorderLayout.CENTER);
        return box;
    }

private static JLabel screenLabel(String text) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(new Font(Font.MONOSPACED, Font.BOLD, 34));
        l.setForeground(new Color(160, 160, 160));
        return l;
    }

    private static JPanel flagRow(LampIndicator lamp, String txt) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel l = new JLabel(txt);
        l.setFont(new Font(Font.DIALOG, Font.PLAIN, 11));
        row.add(lamp);
        row.add(l);
        return row;
    }

    private static final class LampIndicator extends JPanel {
        private Color lampColor = LAMP_OFF;
        private static final int LAMP_SIZE = 12;

        LampIndicator() {
            setOpaque(false);
            setPreferredSize(new Dimension(LAMP_SIZE, LAMP_SIZE));
            setMinimumSize(new Dimension(LAMP_SIZE, LAMP_SIZE));
            setMaximumSize(new Dimension(LAMP_SIZE, LAMP_SIZE));
        }

        void setLampColor(Color color) {
            this.lampColor = color;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int size = Math.min(getWidth(), getHeight());
            int x = (getWidth() - size) / 2;
            int y = (getHeight() - size) / 2;
            g2.setColor(lampColor);
            g2.fillOval(x + 1, y + 1, size - 2, size - 2);
            g2.setColor(lampColor.darker());
            g2.drawOval(x + 1, y + 1, size - 2, size - 2);
            g2.dispose();
        }
    }

    // ─── график ───────────────────────────────────────────────────────────

    private final LiveChart chartData = new LiveChart();

    private static final class ControlChart extends JPanel {
        ControlChart(LiveChart data) {
            super(new BorderLayout());
            org.jfree.data.xy.XYSeriesCollection tempDs = new org.jfree.data.xy.XYSeriesCollection();
            tempDs.addSeries(data.targetSeries);
            tempDs.addSeries(data.measSeries);
            org.jfree.data.xy.XYSeriesCollection humDs = new org.jfree.data.xy.XYSeriesCollection();
            humDs.addSeries(data.humTargetSeries);
            humDs.addSeries(data.humMeasSeries);
            org.jfree.chart.JFreeChart chart = org.jfree.chart.ChartFactory.createXYLineChart(
                    "Опрос камеры", "время, с", "°C", tempDs,
                    org.jfree.chart.plot.PlotOrientation.VERTICAL, true, true, false);
            org.jfree.chart.plot.XYPlot plot = chart.getXYPlot();
            org.jfree.chart.axis.NumberAxis humAxis = new org.jfree.chart.axis.NumberAxis("%");
            humAxis.setAutoRangeIncludesZero(true);
            humAxis.setUpperBound(100);
            plot.setRangeAxis(1, humAxis);
            plot.setDataset(1, humDs);
            plot.mapDatasetToRangeAxis(1, 1);
            org.jfree.chart.renderer.xy.XYLineAndShapeRenderer r1 =
                    (org.jfree.chart.renderer.xy.XYLineAndShapeRenderer) plot.getRenderer(0);
            org.jfree.chart.renderer.xy.XYLineAndShapeRenderer r2 =
                    new org.jfree.chart.renderer.xy.XYLineAndShapeRenderer(true, false);
            r2.setSeriesPaint(0, new Color(60, 150, 255));
            r2.setSeriesPaint(1, new Color(30, 100, 200));
            r1.setSeriesShapesVisible(0, false);
            r1.setSeriesShapesVisible(1, false);
            r2.setSeriesShapesVisible(0, false);
            r2.setSeriesShapesVisible(1, false);
            plot.setRenderer(1, r2);
            add(new org.jfree.chart.ChartPanel(chart), BorderLayout.CENTER);
        }
    }

    private static final class LiveChart {
        private double t = 0;
        final org.jfree.data.xy.XYSeries targetSeries = new org.jfree.data.xy.XYSeries("Темп. задано");
        final org.jfree.data.xy.XYSeries measSeries = new org.jfree.data.xy.XYSeries("Темп. текущая");
        final org.jfree.data.xy.XYSeries humTargetSeries = new org.jfree.data.xy.XYSeries("Влаж. задано");
        final org.jfree.data.xy.XYSeries humMeasSeries = new org.jfree.data.xy.XYSeries("Влаж. текущая");

        synchronized void addTarget(double v) { t += 0.1; add(targetSeries, v); }
        synchronized void addMeasured(double v) { t += 0.1; add(measSeries, v); }
        synchronized void addHumTarget(double v) { t += 0.1; add(humTargetSeries, v); }
        synchronized void addHumMeasured(double v) { t += 0.1; add(humMeasSeries, v); }

        private void add(org.jfree.data.xy.XYSeries s, double v) {
            s.add(t, v);
            while (t > 600 && s.getItemCount() > 0 && t - s.getX(0).doubleValue() > 600) {
                s.remove(0);
            }
        }
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

    private void refreshPorts() {
        portCombo.removeAllItems();
        for (SerialPort p : SerialPort.getCommPorts()) {
            portCombo.addItem(p.getSystemPortName() + " — " + p.getDescriptivePortName());
        }
        if (portCombo.getItemCount() == 0) portCombo.addItem("Нет доступных портов");
    }

    private static final int MAX_LOG = 300;
    private final StringBuilder logBuf = new StringBuilder();

    private void appendLog(String line) {
        logBuf.append(line).append("\n");
        if (logBuf.length() > 4000) logBuf.delete(0, logBuf.length() - 3000);
        logArea.setText(logBuf.toString());
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    public void shutdown() { service.shutdown(); }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("TT5166 Control Test");
            f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            f.setContentPane(new TT5166ControlPanel());
            f.setSize(900, 600);
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}