package org.example.gui.devices.boto.control;

import com.fazecast.jSerialComm.SerialPort;
import lombok.extern.slf4j.Slf4j;
import org.example.device.protBoto.BotoModbusUtil;
import org.example.gui.utilites.GuiUtilities;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.Locale;

/**
 * Панель управления термокамерой BOTO (Modbus RTU, 9600 8N1).
 * Параметризуется: регистры, масштаб, наименование устройства.
 */
@Slf4j
public class BotoControlPanel extends JPanel {

    private final BotoModbusCommunicationService service;
    private final int tempReg;
    private final int setTempReg;
    private final int modReg;
    private final int tempScale;
    private final String deviceName;

    private final JComboBox<String> portCombo = new JComboBox<>();
    private final JButton refreshBtn = new JButton("Обновить");
    private final JButton openBtn = new JButton("Подключить");
    private final JButton closeBtn = new JButton("Отключить");
    private final JLabel statusLabel = new JLabel("Отключено");

    private final JLabel tempScreen = screenLabel();
    private final JLabel setpointScreen = screenLabel();
    private final JSpinner setpointSpinner = new JSpinner(new SpinnerNumberModel(25.0, 0.0, 400.0, 0.5));
    private final JCheckBox onCheckBox = new JCheckBox("ВКЛ", false);
    private final JButton setSetpointBtn = new JButton("Записать уставку");
    private final JButton toggleBtn = new JButton("ВКЛ / ВЫКЛ");
    private final JButton queryTempBtn = new JButton("Запросить температуру");
    private final JTextArea logArea = new JTextArea();

    public BotoControlPanel(String deviceName, int tempReg, int setTempReg, int modReg, int tempScale) {
        this.deviceName = deviceName;
        this.tempReg = tempReg;
        this.setTempReg = setTempReg;
        this.modReg = modReg;
        this.tempScale = tempScale;

        this.service = new BotoModbusCommunicationService();

        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Управление " + deviceName + " (Modbus RTU, 9600 8N1)",
                TitledBorder.LEFT, TitledBorder.TOP));

        add(createLeftPanel(), BorderLayout.WEST);
        add(createCenterPanel(), BorderLayout.CENTER);

        service.addStatusListener(s -> SwingUtilities.invokeLater(() -> statusLabel.setText(s)));
        service.addLogListener(l -> SwingUtilities.invokeLater(() -> appendLog(l)));
        service.addResponseListener(frame -> SwingUtilities.invokeLater(() -> handleResponse(frame)));

        refreshBtn.addActionListener(e -> refreshPorts());
        openBtn.addActionListener(e -> openPort());
        closeBtn.addActionListener(e -> closePort());
        setSetpointBtn.addActionListener(e -> writeSetpoint());
        toggleBtn.addActionListener(e -> toggleMod());
        queryTempBtn.addActionListener(e -> queryTemp());

        GuiUtilities.darkenInputs(this);
    }

    // ─── Modbus helpers ─────────────────────────────────────────────

    private void sendRead(int regAddr, int quantity) {
        byte[] frame = BotoModbusUtil.buildReadHoldingRequest(1, regAddr, quantity);
        if (frame != null) service.sendRequest(frame);
    }

    private void writeRegister(int regAddr, int value) {
        byte[] frame = BotoModbusUtil.buildWriteSingleRequest(1, regAddr, value);
        if (frame != null) service.sendRequest(frame);
    }

    private void handleResponse(byte[] frame) {
        if (frame == null || frame.length < 4) return;
        int func = frame[1] & 0xFF;
        if (func == 0x03 && frame.length >= 7) {
            int byteCount = frame[2] & 0xFF;
            if (byteCount == 2) {
                int regAddr = ((frame[3] & 0xFF) << 8) | (frame[4] & 0xFF);
                if (regAddr == tempReg) {
                    int raw = ((frame[5] & 0xFF) << 8) | (frame[6] & 0xFF);
                    double temp = raw / (double) tempScale;
                    tempScreen.setText(String.format(Locale.US, "%.2f °C", temp));
                    tempScreen.setForeground(new Color(0, 140, 0));
                    chartData.addMeasured(temp);
                    return;
                }
            }
        }
        if (func == 0x06 && frame.length >= 6) {
            int regAddr = ((frame[2] & 0xFF) << 8) | (frame[3] & 0xFF);
            if (regAddr == modReg) {
                int val = ((frame[4] & 0xFF) << 8) | (frame[5] & 0xFF);
                SwingUtilities.invokeLater(() -> onCheckBox.setSelected(val == 1));
                appendLog("Вкл/выкл → " + (val == 1 ? "ВКЛ" : "ВЫКЛ"));
            } else if (regAddr == setTempReg) {
                int val = ((frame[4] & 0xFF) << 8) | (frame[5] & 0xFF);
                double set = val / (double) tempScale;
                setpointScreen.setText(String.format(Locale.US, "%.2f °C", set));
                setpointScreen.setForeground(new Color(0, 140, 0));
                chartData.addTarget(set);
                appendLog("Уставка записана: " + String.format(Locale.US, "%.2f", set) + " °C");
            }
        }
    }

    // ─── Command actions ────────────────────────────────────────────

    private void queryTemp() {
        sendRead(tempReg, 1);
        appendLog("Запрос температуры (reg " + tempReg + ")");
    }

    private void writeSetpoint() {
        double setpoint = ((Number) setpointSpinner.getValue()).doubleValue();
        int raw = (int) Math.round(setpoint * tempScale);
        writeRegister(setTempReg, raw);
        appendLog("Запись уставки: " + String.format(Locale.US, "%.2f", setpoint) + " °C (raw " + raw + ")");
    }

    private void toggleMod() {
        int current = onCheckBox.isSelected() ? 0 : 1;
        writeRegister(modReg, current);
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
                        service.setPollingEnabled(true, () -> queryTemp());
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
    }

    private void pollOnce() {
        queryTemp();
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

        p.add(label("Уставка, °C"));
        p.add(fullWidth(setpointSpinner));
        p.add(Box.createVerticalStrut(4));
        setSetpointBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(setSetpointBtn);
        p.add(Box.createVerticalStrut(8));

        onCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(onCheckBox);
        toggleBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(toggleBtn);
        p.add(Box.createVerticalStrut(8));

        queryTempBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(queryTempBtn);

        p.add(Box.createVerticalGlue());
        return p;
    }

    private JPanel createCenterPanel() {
        JPanel center = new JPanel(new BorderLayout(8, 8));
        center.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel screens = new JPanel(new GridLayout(1, 2, 12, 0));
        tempScreen.setHorizontalAlignment(SwingConstants.CENTER);
        setpointScreen.setHorizontalAlignment(SwingConstants.CENTER);
        screens.add(panelBox("ТЕКУЩАЯ ТЕМПЕРАТУРА", tempScreen));
        screens.add(panelBox("УСТАВКА", setpointScreen));
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

    private static JLabel screenLabel() {
        JLabel l = new JLabel("-- °C", SwingConstants.CENTER);
        l.setFont(new Font(Font.MONOSPACED, Font.BOLD, 34));
        l.setForeground(new Color(160, 160, 160));
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
            JFrame f = new JFrame("BOTO Control Test");
            f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            f.setContentPane(new BotoControlPanel("BOTO", 10, 60, 63, 10));
            f.setSize(900, 600);
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}
