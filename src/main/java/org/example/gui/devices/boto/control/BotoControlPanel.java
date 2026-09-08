package org.example.gui.devices.boto.control;

import com.fazecast.jSerialComm.SerialPort;
import lombok.extern.slf4j.Slf4j;
import org.example.device.protBoto.BotoModbusUtil;
import org.example.gui.utilites.GuiUtilities;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.prefs.Preferences;

/**
 * Панель управления термокамерой BOTO (Modbus RTU, 9600 8N1).
 * Параметризуется: регистры, масштаб, наименование устройства.
 */
@Slf4j
public class BotoControlPanel extends JPanel {

    private static final String PREFS_NODE = "org/example/gui/devices/boto/control";
    private static final String LAST_PORT = "lastPort";
    private final AtomicBoolean loadingSettings = new AtomicBoolean(false);

    // Кандидаты на адреса регистров B-TH-800F (согласовано с BotoModbusResponder).
    // Запись в рег 18/19 на реальной камере эффекта не даёт (влага всегда поддерживается при
    // работе, подсветка включается с пуском) — здесь они используются ТОЛЬКО для отображения.
    private static final int REG_HUMI_EN = 18;       // кандидат: «поддержка влаги» (чтение статуса)
    private static final int REG_LIGHT = 19;         // кандидат: подсветка (чтение статуса)
    private static final int REG_ERROR_FLAGS = 20;   // битовая маска ошибок (биты 0..7)
    // TODO(подсветка): команда включения лампы так и не найдена — в камере стоит более новая
    // штатная панель (v4.1), наш протокол подогнан под v2.5, и перехватить кадр кнопки
    // «Подсветка» не удалось. Лампа управляется только штатной панелью камеры; здесь —
    // отображение статуса из блока реального времени (без отдельной кнопки записи).

    // Опрос реального времени — блоком 10..49 (40 регистров), как штатная программа B-TH-800F:
    // запрос 01 03 00 0A 00 28 (65 D6). При подключении последовательность запросов повторяет
    // штатную панель (снято с её трейса): RT → области программ с записью указателя программы
    // (01 10 1F AC 00 01 02 00 00 — рег 8108 = 0). В простое каждый тик: RT + чтение 8900×3.
    private static final int RT_BLOCK_COUNT = 40;          // рег 10..49 — «реальное время»
    private static final int REG_PROG_POINTER = 0x1FAC;    // 8108 — указатель программы (штатная пишет 0)
    private static final int REG_PROG_HEAD = 0x230C;       // 8972 — читает 18 рег (область проб)
    private static final int REG_PROG_DETAIL = 0x22C4;     // 8900 — читает 64, в простое 3 рег
    private static final int REG_PROG_TAIL = 0x2304;       // 8964 — читает 8 рег

    private static final String[] ERROR_NAMES = {
            "Перегрев", "Недогрев/датчик", "Датчик темп.", "Датчик влаги",
            "Мало воды", "Парогенератор", "Вентилятор", "Дверь открыта"
    };

    private final BotoModbusCommunicationService service;
    private final int tempReg;
    private final int setTempReg;
    private final int modReg;
    private final int modeReg;
    private final int tempScale;
    private final String deviceName;

    private final JComboBox<String> portCombo = new JComboBox<>();
    private final JButton refreshBtn = new JButton("Обновить");
    private final JButton openBtn = new JButton("Подключить");
    private final JButton closeBtn = new JButton("Отключить");
    private final JLabel statusLabel = new JLabel("Отключено");

    private final JLabel tempScreen = screenLabel("°C");
    private final JLabel humidityScreen = screenLabel("%");
    private final JLabel setpointScreen = screenLabel("°C");
    private final JLabel humiditySetpointScreen = screenLabel("%");
    private final JLabel modeLabel = new JLabel("—", SwingConstants.CENTER);
    private final JSpinner setpointSpinner = new JSpinner(new SpinnerNumberModel(25.0, 0.0, 400.0, 0.5));
    private final JCheckBox onCheckBox = new JCheckBox("ВКЛ", false);
    private final JButton setSetpointBtn = new JButton("Записать уставку");
    private final JButton toggleBtn = new JButton("ВКЛ / ВЫКЛ");
    private final JButton queryAllBtn = new JButton("Запросить показания");

    private final JSpinner humiditySetpointSpinner = new JSpinner(new SpinnerNumberModel(50.0, 0.0, 100.0, 1.0));
    private final JButton humiSetpointBtn = new JButton("Записать уставку влажности");
    private final JLabel humidityStatusLabel = new JLabel("поддержка влаги: —");
    private final JLabel lightStatusLabel = new JLabel("подсветка: —");
    private final JLabel alarmsLabel = new JLabel("ошибки: нет");
    private final JTextArea logArea = new JTextArea();

    public BotoControlPanel(String deviceName, int tempReg, int setTempReg, int modReg, int tempScale) {
        this(deviceName, tempReg, setTempReg, modReg, 31, tempScale);
    }

    public BotoControlPanel(String deviceName, int tempReg, int setTempReg, int modReg, int modeReg, int tempScale) {
        this.deviceName = deviceName;
        this.tempReg = tempReg;
        this.setTempReg = setTempReg;
        this.modReg = modReg;
        this.modeReg = modeReg;
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
        queryAllBtn.addActionListener(e -> queryAll());
        humiSetpointBtn.addActionListener(e -> writeHumiditySetpoint());

        portCombo.addItemListener(e -> saveSelectedPort());

        refreshPorts();
        loadLastSettings();

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

    /** Запись нескольких регистров (0x10); штатная B-TH-800F пишет так указатель программы. */
    private void writeMultiRegister(int regAddr, int... values) {
        byte[] frame = BotoModbusUtil.buildWriteMultipleRequest(1, regAddr, values);
        if (frame != null) service.sendRequest(frame);
    }

    private void handleResponse(byte[] frame) {
        if (frame == null || frame.length < 4) return;
        int func = frame[1] & 0xFF;
        if (func == 0x03) {
            // В график идёт ТОЛЬКО блок реального времени (рег 10..49, 80 байт данных).
            // Ответы на чтения областей программ (рег 8108/8964/8972/8900) других размеров
            // в график не попадают — иначе нули от них портят график.
            if ((frame[2] & 0xFF) == RT_BLOCK_COUNT * 2) {
                int[] values = BotoModbusUtil.parseReadResponse(frame);
                applyReadValues(values);
            }
            return;
        }
        if (func == 0x06 && frame.length >= 6) {
            // Ответ 0x06 — это эхо запроса: адрес регистра + записанное значение.
            int regAddr = ((frame[2] & 0xFF) << 8) | (frame[3] & 0xFF);
            if (regAddr == modReg) {
                int val = ((frame[4] & 0xFF) << 8) | (frame[5] & 0xFF);
                onCheckBox.setSelected(val == 1);
                appendLog("Вкл/выкл → " + (val == 1 ? "ВКЛ" : "ВЫКЛ"));
            } else if (regAddr == setTempReg) {
                int val = ((frame[4] & 0xFF) << 8) | (frame[5] & 0xFF);
                double set = val / (double) tempScale;
                setpointScreen.setText(String.format(Locale.US, "%.2f °C", set));
                setpointScreen.setForeground(new Color(0, 140, 0));
                chartData.addTarget(set);
                appendLog("Уставка записана: " + String.format(Locale.US, "%.2f", set) + " °C");
            } else if (regAddr == setTempReg + 1) {
                int val = ((frame[4] & 0xFF) << 8) | (frame[5] & 0xFF);
                double set = val / (double) tempScale;
                humiditySetpointScreen.setText(String.format(Locale.US, "%.2f %%", set));
                humiditySetpointScreen.setForeground(new Color(0, 140, 0));
                appendLog("Уставка влажности записана: " + String.format(Locale.US, "%.2f", set) + " %");
            }
        }
    }

    /**
     * Разбор блока реального времени (рег 10..49, запрос как у штатной B-TH-800F):
     * +0 TEMP_PV, +1/+2 TEMP_SP (рег 12 = зеркало), +3 MV темп., +4 HUMI_PV, +5/+6 HUMI_SP,
     * +7 MV влаги. Режим (камера вкл/выкл) — режим-регистр {@code modeReg} (31), в блоке.
     */
    private void applyReadValues(int[] values) {
        if (values == null || values.length < 6) return;
        double temp = values[0] / (double) tempScale;
        double setT = values[1] / (double) tempScale;
        double hum = values[4] / (double) tempScale;
        double setH = values[5] / (double) tempScale;
        tempScreen.setText(String.format(Locale.US, "%.2f °C", temp));
        tempScreen.setForeground(new Color(0, 140, 0));
        setpointScreen.setText(String.format(Locale.US, "%.2f °C", setT));
        setpointScreen.setForeground(new Color(0, 140, 0));
        humidityScreen.setText(String.format(Locale.US, "%.2f %%", hum));
        humidityScreen.setForeground(new Color(0, 140, 0));
        humiditySetpointScreen.setText(String.format(Locale.US, "%.2f %%", setH));
        humiditySetpointScreen.setForeground(new Color(0, 140, 0));
        chartData.addMeasured(temp);
        chartData.addTarget(setT);

        int modeIdx = modeReg - tempReg;
        if (values.length > modeIdx) {
            boolean on = values[modeIdx] == 1;
            onCheckBox.setSelected(on);
            modeLabel.setText(on ? "ВКЛ" : "ВЫКЛ");
            modeLabel.setForeground(on ? new Color(0, 140, 0) : new Color(180, 60, 0));
        }

        int humiEnIdx = REG_HUMI_EN - tempReg;
        int lightIdx = REG_LIGHT - tempReg;
        int errIdx = REG_ERROR_FLAGS - tempReg;
        if (values.length > errIdx) {
            // На реальной камере запись в рег 18/19 эффекта не даёт: влага поддерживается при
            // работе всегда, подсветка включается с пуском. Показываем, что возвращает камера.
            humidityStatusLabel.setText("поддержка влаги: " + (values[humiEnIdx] == 1 ? "вкл" : "выкл"));
            lightStatusLabel.setText("подсветка: " + (values[lightIdx] == 1 ? "вкл" : "выкл"));
            renderErrors(values[errIdx]);
        }
    }

    /** Вывод флагов ошибок (рег 20) — как на панели Testa: текст, красный при аварии. */
    private void renderErrors(int flags) {
        List<String> on = new ArrayList<>();
        for (int b = 0; b < ERROR_NAMES.length; b++) {
            if ((flags & (1 << b)) != 0) on.add(ERROR_NAMES[b]);
        }
        alarmsLabel.setText(on.isEmpty() ? "ошибки: нет" : "⚠ " + String.join("; ", on));
        alarmsLabel.setForeground(on.isEmpty() ? new Color(160, 160, 160) : new Color(200, 40, 40));
    }

    // ─── Command actions ────────────────────────────────────────────

    private void queryAll() {
        sendRead(tempReg, RT_BLOCK_COUNT);   // рег 10..49 — как штатная программа (01 03 00 0A 00 28)
        appendLog("Запрос реального времени (рег 10..49): T, влажность, режим, пределы");
    }

    /**
     * Точная последовательность подключения штатной панели B-TH-800F (по её трейсу):
     * RT → рег 8972×18 → указатель=0 → RT → рег 8108×1 → указатель=0 →
     * рег 8900×64 → рег 8964×8 → указатель=0.
     */
    private void exploreRegisters() {
        appendLog("Имитация подключения штатной B-TH-800F (RT + области программ)");
        queryAll();                                // (1) RT 10..49
        sendRead(REG_PROG_HEAD, 18);               // (2) рег 8972..8989
        writeMultiRegister(REG_PROG_POINTER, 0);   // (3) указатель программы = 0
        queryAll();                                // (4) RT 10..49
        sendRead(REG_PROG_POINTER, 1);             // (5) рег 8108
        writeMultiRegister(REG_PROG_POINTER, 0);   // (6)
        sendRead(REG_PROG_DETAIL, 64);             // (7) рег 8900..8963
        sendRead(REG_PROG_TAIL, 8);                // (8) рег 8964..8971
        writeMultiRegister(REG_PROG_POINTER, 0);   // (9)
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

    private void writeHumiditySetpoint() {
        double setpoint = ((Number) humiditySetpointSpinner.getValue()).doubleValue();
        int raw = (int) Math.round(setpoint * tempScale);
        writeRegister(setTempReg + 1, raw);
        appendLog("Уставка влажности: " + String.format(Locale.US, "%.2f", setpoint)
                + " % (рег " + (setTempReg + 1) + ", raw " + raw + ")");
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
                        BotoControlPanel.this.exploreRegisters();   // строго до опроса — порядок как у штатной
                        service.setPollingEnabled(true, BotoControlPanel.this::pollOnce);
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
        resetScreens();
    }

    private void resetScreens() {
        tempScreen.setText("-- °C");
        tempScreen.setForeground(new Color(160, 160, 160));
        humidityScreen.setText("-- %");
        humidityScreen.setForeground(new Color(160, 160, 160));
        modeLabel.setText("—");
        modeLabel.setForeground(new Color(160, 160, 160));
    }

    private void pollOnce() {
        queryAll();                        // RT 10..49
        sendRead(REG_PROG_DETAIL, 3);      // штатная в том же тике читает 8900×3
    }

    // ─── UI ─────────────────────────────────────────────────────────

    private JScrollPane createLeftPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

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

        p.add(label("Режим работы"));
        modeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        modeLabel.setHorizontalAlignment(SwingConstants.LEFT);
        modeLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 18));
        p.add(modeLabel);
        p.add(Box.createVerticalStrut(8));

        queryAllBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(queryAllBtn);

        p.add(Box.createVerticalStrut(12));
        p.add(sectionLabel("Влажность"));
        humidityStatusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        humidityStatusLabel.setForeground(new Color(100, 100, 100));
        p.add(humidityStatusLabel);
        p.add(Box.createVerticalStrut(2));
        p.add(label("Уставка влажности, %"));
        p.add(fullWidth(humiditySetpointSpinner));
        p.add(Box.createVerticalStrut(4));
        humiSetpointBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(humiSetpointBtn);

        p.add(Box.createVerticalStrut(12));
        p.add(sectionLabel("Подсветка"));
        lightStatusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        lightStatusLabel.setForeground(new Color(100, 100, 100));
        p.add(lightStatusLabel);

        p.add(Box.createVerticalStrut(12));
        p.add(sectionLabel("Флаги ошибок"));
        alarmsLabel.setFont(new Font(Font.DIALOG, Font.BOLD, 12));
        alarmsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(alarmsLabel);

        p.add(Box.createVerticalGlue());

        JScrollPane scroller = new JScrollPane(p);
        scroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroller.setBorder(null);
        scroller.setPreferredSize(new Dimension(320, 0));
        return scroller;
    }

    private JPanel createCenterPanel() {
        JPanel center = new JPanel(new BorderLayout(8, 8));
        center.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel screens = new JPanel(new GridLayout(2, 2, 12, 8));
        tempScreen.setHorizontalAlignment(SwingConstants.CENTER);
        humidityScreen.setHorizontalAlignment(SwingConstants.CENTER);
        setpointScreen.setHorizontalAlignment(SwingConstants.CENTER);
        humiditySetpointScreen.setHorizontalAlignment(SwingConstants.CENTER);
        screens.add(panelBox("ТЕКУЩАЯ ТЕМПЕРАТУРА", tempScreen));
        screens.add(panelBox("ТЕКУЩАЯ ВЛАЖНОСТЬ", humidityScreen));
        screens.add(panelBox("УСТАВКА ТЕМПЕРАТУРЫ", setpointScreen));
        screens.add(panelBox("УСТАВКА ВЛАЖНОСТИ", humiditySetpointScreen));
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

    private static JLabel screenLabel(String unit) {
        JLabel l = new JLabel("-- " + unit, SwingConstants.CENTER);
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

    private void loadLastSettings() {
        loadingSettings.set(true);
        try {
            String lastPort = Preferences.userRoot().node(PREFS_NODE).get(LAST_PORT, "");
            if (!lastPort.isEmpty()) {
                for (int i = 0; i < portCombo.getItemCount(); i++) {
                    String item = portCombo.getItemAt(i);
                    if (item != null && item.startsWith(lastPort)) {
                        portCombo.setSelectedIndex(i);
                        break;
                    }
                }
            }
        } finally {
            loadingSettings.set(false);
        }
    }

    private void saveSelectedPort() {
        if (loadingSettings.get()) return;
        Object sel = portCombo.getSelectedItem();
        if (sel == null || sel.toString().contains("Нет")) return;
        String portName = sel.toString().split(" — ")[0].trim();
        Preferences.userRoot().node(PREFS_NODE).put(LAST_PORT, portName);
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
            f.setContentPane(new BotoControlPanel("BOTO", 10, 60, 63, 31, 10));
            f.setSize(900, 600);
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}
