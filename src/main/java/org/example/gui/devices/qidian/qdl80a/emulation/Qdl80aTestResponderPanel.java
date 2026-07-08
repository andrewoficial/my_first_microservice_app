package org.example.gui.devices.qidian.qdl80a.emulation;

import org.example.gui.devices.stu.mcps.AsyncLogger;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.*;

public class Qdl80aTestResponderPanel extends JPanel {

    private final AsyncLogger logger;
    private final ModbusSerialService serialService;
    private final ModbusResponder responder;

    private final JComboBox<String> portCombo = new JComboBox<>();
    private final JButton startBtn = new JButton("Старт");
    private final JButton stopBtn = new JButton("Стоп");
    private final JButton clearBtn = new JButton("Очистить график");
    private final JButton genSineBtn = new JButton("Синус");
    private final JButton genRandomBtn = new JButton("Случайный");
    private final JButton genStopBtn = new JButton("Остановить ген.");

    // Новые элементы для адреса и частоты
    private final JTextField addressField = new JTextField("1", 5);
    private final JButton setAddressBtn = new JButton("Уст. адрес");
    private final JComboBox<Integer> baudCombo = new JComboBox<>(new Integer[]{1200,2400,4800,9600,19200,38400,57600,115200});
    private final JButton setBaudBtn = new JButton("Уст. скорость");
    private final JTextField offsetField = new JTextField("0", 6);
    private final JButton setOffsetBtn = new JButton("Уст. смещение");
    private final JButton setUnitBtn = new JButton("Уст. единицы");
    private final JButton setDecimalBtn = new JButton("Уст. знаки");
    private final JComboBox<String> unitCombo = new JComboBox<>(new String[]{"МПа","кПа","Па","бар","мбар","кгс/см²","PSI","мH₂O","ммH₂O","дюйм H₂O",
            "фут H₂O","ммHg","дюйм Hg","атм","Торр","м","см","мм","кг","°C","pH","°F","пусто"});
    private final JComboBox<Integer> decimalCombo = new JComboBox<>(new Integer[]{0,1,2,3,4});
    private final JSlider freqSlider = new JSlider(1, 50, 5); // 0.1..5.0 Гц с шагом 0.1
    private final JLabel freqLabel = new JLabel("0.5 Гц");

    private final JSlider valueSlider = new JSlider(-32768, 32767, 0);
    private final JLabel valueLabel = new JLabel("0");

    private final OscilloscopePanel oscilloscopePanel = new OscilloscopePanel(2000); // 2 сек окно
    private final JTextArea logArea = new JTextArea();

    private final JLabel addressStatus = new JLabel("Адрес: 1");
    private final JLabel baudStatus = new JLabel("Скорость: 9600");
    private final JLabel unitStatus = new JLabel("Единицы: кПа");
    private final JLabel decimalStatus = new JLabel("Десят.: 1");
    private final JLabel offsetStatus = new JLabel("Смещение: 0");

    private final List<String> logLines = new CopyOnWriteArrayList<>();
    private static final int MAX_LOG_LINES = 200;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private ScheduledFuture<?> generatorTask;
    private volatile boolean generatorRunning = false;
    private volatile double currentPV = 0.0;
    private long startTime = System.currentTimeMillis();

    // Переменная для хранения текущей частоты
    private volatile double frequency = 0.5; // Гц

    public Qdl80aTestResponderPanel(AsyncLogger logger) {
        this.logger = logger;
        this.responder = new ModbusResponder();
        this.serialService = new ModbusSerialService(this::handleIncomingData, logger);

        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createTitledBorder("Эмулятор QDL80A (Modbus RTU)"));

        JPanel leftPanel = createLeftPanel();
        JScrollPane leftScroll = new JScrollPane(leftPanel);
        leftScroll.setPreferredSize(new Dimension(210, 0));
        leftScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        leftScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        leftScroll.setBorder(null);
        add(leftScroll, BorderLayout.WEST);
        add(oscilloscopePanel, BorderLayout.CENTER);
        add(createBottomPanel(), BorderLayout.SOUTH);

        initListeners();
        refreshPorts();
        stopBtn.setEnabled(false);
        genStopBtn.setEnabled(false);

        // Таймер для перерисовки осциллографа
        new Timer(50, e -> oscilloscopePanel.repaint()).start();

        // Обновление статуса каждые 500 мс
        scheduler.scheduleAtFixedRate(this::updateStatus, 200, 500, TimeUnit.MILLISECONDS);
    }

    private JPanel createLeftPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // ---- COM порт ----
        JLabel comLabel = new JLabel("COM порт");
        comLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(comLabel);
        p.add(Box.createVerticalStrut(2));
        portCombo.setMaximumSize(new Dimension(180, 25));
        portCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(portCombo);
        p.add(Box.createVerticalStrut(3));
        JPanel comBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        comBtns.add(startBtn);
        comBtns.add(stopBtn);
        comBtns.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(comBtns);

        p.add(Box.createVerticalStrut(8));

        // ---- Адрес ----
        JLabel addrLabel = new JLabel("Адрес устройства");
        addrLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(addrLabel);
        p.add(Box.createVerticalStrut(2));
        JPanel addrRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        addressField.setPreferredSize(new Dimension(50, 25));
        addressField.setMaximumSize(new Dimension(50, 25));
        addrRow.add(addressField);
        addrRow.add(setAddressBtn);
        addrRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(addrRow);

        p.add(Box.createVerticalStrut(8));

        // ---- Скорость ----
        JLabel baudLabel = new JLabel("Скорость (бод)");
        baudLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(baudLabel);
        p.add(Box.createVerticalStrut(2));
        baudCombo.setSelectedItem(9600);
        baudCombo.setMaximumSize(new Dimension(120, 25));
        baudCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(baudCombo);
        p.add(Box.createVerticalStrut(3));
        JPanel baudRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        baudRow.add(setBaudBtn);
        baudRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(baudRow);

        p.add(Box.createVerticalStrut(8));

        // ---- Смещение нуля ----
        JLabel offsetLabel = new JLabel("Смещение нуля");
        offsetLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(offsetLabel);
        p.add(Box.createVerticalStrut(2));
        JPanel offsetRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        offsetField.setPreferredSize(new Dimension(70, 25));
        offsetField.setMaximumSize(new Dimension(70, 25));
        offsetRow.add(offsetField);
        offsetRow.add(setOffsetBtn);
        offsetRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(offsetRow);

        p.add(Box.createVerticalStrut(8));

        // ---- Единицы ----
        JLabel unitLabel = new JLabel("Единицы измерения");
        unitLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(unitLabel);
        p.add(Box.createVerticalStrut(2));
        unitCombo.setSelectedIndex(1);
        unitCombo.setMaximumSize(new Dimension(140, 25));
        unitCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(unitCombo);
        p.add(Box.createVerticalStrut(3));
        JPanel unitRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        unitRow.add(setUnitBtn);
        unitRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(unitRow);

        p.add(Box.createVerticalStrut(8));

        // ---- Десятичные знаки ----
        JLabel decimalLabel = new JLabel("Десятичных знаков");
        decimalLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(decimalLabel);
        p.add(Box.createVerticalStrut(2));
        decimalCombo.setSelectedIndex(1);
        decimalCombo.setMaximumSize(new Dimension(80, 25));
        decimalCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(decimalCombo);
        p.add(Box.createVerticalStrut(3));
        JPanel decimalRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        decimalRow.add(setDecimalBtn);
        decimalRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(decimalRow);

        p.add(Box.createVerticalStrut(8));

        // ---- PV (значение) ----
        JLabel pvLabel = new JLabel("PV (значение)");
        pvLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(pvLabel);
        p.add(Box.createVerticalStrut(2));
        valueSlider.setPreferredSize(new Dimension(180, 20));
        valueSlider.setMaximumSize(new Dimension(180, 20));
        valueSlider.setAlignmentX(Component.LEFT_ALIGNMENT);
        valueSlider.setMajorTickSpacing(5000);
        valueSlider.setMinorTickSpacing(1000);
        valueSlider.setPaintTicks(true);
        p.add(valueSlider);
        p.add(Box.createVerticalStrut(2));
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(valueLabel);

        p.add(Box.createVerticalStrut(8));

        // ---- Частота генератора ----
        JLabel freqLabelHdr = new JLabel("Частота (Гц)");
        freqLabelHdr.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(freqLabelHdr);
        p.add(Box.createVerticalStrut(2));
        freqSlider.setPreferredSize(new Dimension(180, 20));
        freqSlider.setMaximumSize(new Dimension(180, 20));
        freqSlider.setAlignmentX(Component.LEFT_ALIGNMENT);
        freqSlider.setMajorTickSpacing(10);
        freqSlider.setMinorTickSpacing(2);
        freqSlider.setPaintTicks(true);
        p.add(freqSlider);
        p.add(Box.createVerticalStrut(2));
        freqLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(freqLabel);

        p.add(Box.createVerticalStrut(8));

        // ---- Генераторы ----
        JPanel genRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        genRow.add(genSineBtn);
        genRow.add(genRandomBtn);
        genRow.add(genStopBtn);
        genRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(genRow);
        p.add(Box.createVerticalStrut(3));
        clearBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(clearBtn);

        // Растяжка вниз, чтобы всё было сверху
        p.add(Box.createVerticalGlue());

        return p;
    }

    private JPanel createBottomPanel() {
        JPanel bottom = new JPanel(new GridLayout(1, 2, 10, 0));

        JPanel statusPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        statusPanel.setBorder(BorderFactory.createTitledBorder("Состояние эмулятора"));
        statusPanel.add(addressStatus);
        statusPanel.add(baudStatus);
        statusPanel.add(unitStatus);
        statusPanel.add(decimalStatus);
        statusPanel.add(offsetStatus);

        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        logArea.setRows(8);
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Лог событий"));

        bottom.add(statusPanel);
        bottom.add(logScroll);

        return bottom;
    }

    private void initListeners() {
        startBtn.addActionListener(e -> startListening());
        stopBtn.addActionListener(e -> stopListening());
        clearBtn.addActionListener(e -> {
            oscilloscopePanel.clearData();
            addLog("График очищен");
        });

        genSineBtn.addActionListener(e -> startGenerator("sine"));
        genRandomBtn.addActionListener(e -> startGenerator("random"));
        genStopBtn.addActionListener(e -> stopGenerator());

        valueSlider.addChangeListener(e -> {
            int val = valueSlider.getValue();
            currentPV = val;
            responder.setPv(val);
            valueLabel.setText(String.valueOf(val));
            addDataPoint(val);
            addLog("Установлено PV = " + val);
        });

        // Установка адреса
        setAddressBtn.addActionListener(e -> {
            try {
                int newAddr = Integer.parseInt(addressField.getText().trim());
                responder.setAddress(newAddr);
                addLog("Адрес изменён на " + newAddr);
                addressStatus.setText("Адрес: " + newAddr);
            } catch (NumberFormatException ex) {
                addLog("Ошибка: неверный формат адреса");
            } catch (IllegalArgumentException ex) {
                addLog("Ошибка: " + ex.getMessage());
            }
        });

        // Установка скорости
        setBaudBtn.addActionListener(e -> {
            int baud = (Integer) baudCombo.getSelectedItem();
            int code;
            switch (baud) {
                case 1200: code = 0; break;
                case 2400: code = 1; break;
                case 4800: code = 2; break;
                case 9600: code = 3; break;
                case 19200: code = 4; break;
                case 38400: code = 5; break;
                case 57600: code = 6; break;
                case 115200: code = 7; break;
                default: code = 3;
            }
            try {
                responder.setBaudRateCode(code);
                addLog("Скорость изменена на " + baud);
                baudStatus.setText("Скорость: " + baud);
            } catch (IllegalArgumentException ex) {
                addLog("Ошибка: " + ex.getMessage());
            }
        });

        // Установка смещения нуля
        setOffsetBtn.addActionListener(e -> {
            try {
                int offset = Integer.parseInt(offsetField.getText().trim());
                responder.setZeroOffset(offset);
                addLog("Смещение нуля = " + offset);
            } catch (NumberFormatException ex) {
                addLog("Ошибка: неверный формат смещения");
            } catch (IllegalArgumentException ex) {
                addLog("Ошибка: " + ex.getMessage());
            }
        });

        // Изменение частоты через слайдер
        freqSlider.addChangeListener(e -> {
            int val = freqSlider.getValue();
            frequency = val / 10.0; // от 0.1 до 5.0
            freqLabel.setText(String.format("%.1f Гц", frequency));
        });

        // Установка единиц
        setUnitBtn.addActionListener(e -> {
            int idx = unitCombo.getSelectedIndex();
            responder.setUnitCode(idx);
            String[] names = {"МПа","кПа","Па","бар","мбар","кгс/см²","PSI","мH₂O","ммH₂O","дюйм H₂O",
                    "фут H₂O","ммHg","дюйм Hg","атм","Торр","м","см","мм","кг","°C","pH","°F","пусто"};
            unitStatus.setText("Единицы: " + names[idx]);
            addLog("Единицы: " + names[idx]);
        });

        // Установка десятичных знаков
        setDecimalBtn.addActionListener(e -> {
            int dp = (Integer) decimalCombo.getSelectedItem();
            responder.setDecimalPoints(dp);
            decimalStatus.setText("Десят.: " + dp);
            addLog("Десят. знаков: " + dp);
        });
    }

    private void refreshPorts() {
        portCombo.removeAllItems();
        com.fazecast.jSerialComm.SerialPort[] ports = com.fazecast.jSerialComm.SerialPort.getCommPorts();
        for (com.fazecast.jSerialComm.SerialPort p : ports) {
            portCombo.addItem(p.getSystemPortName());
        }
    }

    private void startListening() {
        String port = (String) portCombo.getSelectedItem();
        if (port == null) {
            addLog("[ERR] Не выбран порт");
            return;
        }

        logger.info("Старт эмулятора на " + port);
        if (serialService.openPort(port)) {
            startBtn.setEnabled(false);
            stopBtn.setEnabled(true);
            logger.info("Эмулятор запущен на " + port);
            addLog("[OK] Слушатель запущен на порту " + port);
        } else {
            logger.error("Не удалось открыть порт " + port);
            addLog("[ERR] Не удалось открыть порт " + port);
        }
    }

    private void stopListening() {
        logger.info("Остановка эмулятора");
        serialService.closePort();
        startBtn.setEnabled(true);
        stopBtn.setEnabled(false);
        logger.info("Эмулятор остановлен");
        addLog("[STOP] Слушатель остановлен");
    }

    private void startGenerator(String type) {
        if (generatorRunning) return;
        generatorRunning = true;
        genSineBtn.setEnabled(false);
        genRandomBtn.setEnabled(false);
        genStopBtn.setEnabled(true);
        startTime = System.currentTimeMillis();

        // Увеличим частоту обновления данных до 50 мс для более плавного графика
        generatorTask = scheduler.scheduleAtFixedRate(() -> {
            if (!generatorRunning) return;
            double value;
            long now = System.currentTimeMillis();
            double t = (now - startTime) / 1000.0;
            if ("sine".equals(type)) {
                // Используем текущую частоту
                value = 10000 * Math.sin(2 * Math.PI * frequency * t);
            } else {
                // Случайное блуждание
                value = currentPV + (Math.random() - 0.5) * 1000;
                if (value > 32767) value = 32767;
                if (value < -32768) value = -32768;
            }
            int intVal = (int) Math.round(value);
            SwingUtilities.invokeLater(() -> {
                currentPV = intVal;
                responder.setPv(intVal);
                valueSlider.setValue(intVal);
                valueLabel.setText(String.valueOf(intVal));
                addDataPoint(intVal);
            });
        }, 0, 50, TimeUnit.MILLISECONDS); // 20 раз в секунду
    }

    private void stopGenerator() {
        if (generatorTask != null) {
            generatorTask.cancel(false);
            generatorTask = null;
        }
        generatorRunning = false;
        genSineBtn.setEnabled(true);
        genRandomBtn.setEnabled(true);
        genStopBtn.setEnabled(false);
        addLog("Генерация остановлена");
    }

    private void addDataPoint(double value) {
        oscilloscopePanel.addDataPoint(value);
    }

    private void updateStatus() {
        SwingUtilities.invokeLater(() -> {
            addressStatus.setText("Адрес: " + responder.getAddress());
            baudStatus.setText("Скорость: " + responder.getBaudRateString());
            String[] names = {"МПа","кПа","Па","бар","мбар","кгс/см²","PSI","мH₂O","ммH₂O","дюйм H₂O",
                    "фут H₂O","ммHg","дюйм Hg","атм","Торр","м","см","мм","кг","°C","pH","°F","пусто"};
            int uc = responder.getUnitCode();
            unitStatus.setText("Единицы: " + (uc >= 0 && uc < names.length ? names[uc] : String.valueOf(uc)));
            decimalStatus.setText("Десят.: " + responder.getDecimalPoints());
            offsetStatus.setText("Смещение: " + responder.getZeroOffset());
        });
    }

    private byte[] handleIncomingData(byte[] request) {
        String hex = bytesToHex(request);
        logger.info("Запрос: " + hex);
        addLog(">> " + hex);

        byte[] response = responder.processRequest(request);
        if (response != null) {
            String respHex = bytesToHex(response);
            logger.info("Ответ: " + respHex);
            addLog("<< " + respHex);
        } else {
            logger.warn("NULL для запроса: " + hex);
            addLog("[ERR] " + hex);
        }
        return response;
    }

    private void addLog(String msg) {
        String line = "[" + LocalTime.now().format(TIME_FMT) + "] " + msg;
        logLines.add(line);
        if (logLines.size() > MAX_LOG_LINES) {
            logLines.remove(0);
        }
        SwingUtilities.invokeLater(() -> {
            logArea.setText(String.join("\n", logLines));
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }

    public void shutdown() {
        stopListening();
        stopGenerator();
        scheduler.shutdownNow();
    }

    // ================== Класс осциллографа (без изменений) ==================
    // ... (оставлен как был) ...
    private static class OscilloscopePanel extends JPanel {
        private final int windowMs;
        private final List<DataPoint> data = new CopyOnWriteArrayList<>();
        private double minVal = -1000, maxVal = 1000;

        OscilloscopePanel(int windowMs) {
            this.windowMs = windowMs;
            setPreferredSize(new Dimension(800, 400));
            setBackground(new Color(16, 16, 24));
        }

        synchronized void addDataPoint(double value) {
            data.add(new DataPoint(System.currentTimeMillis(), value));
            long cutoff = System.currentTimeMillis() - windowMs - 500;
            data.removeIf(dp -> dp.timestamp < cutoff);

            if (!data.isEmpty()) {
                double min = data.stream().mapToDouble(dp -> dp.value).min().orElse(-1000);
                double max = data.stream().mapToDouble(dp -> dp.value).max().orElse(1000);
                double range = max - min;
                if (range < 1) range = 1;
                minVal = min - range * 0.1;
                maxVal = max + range * 0.1;
            }
            repaint();
        }

        synchronized void clearData() {
            data.clear();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            long now = System.currentTimeMillis();
            long start = now - windowMs;

            g2.setColor(new Color(36, 36, 48));
            for (int i = 0; i < 5; i++) {
                int y = h * i / 5;
                g2.drawLine(0, y, w, y);
            }
            for (int i = 0; i < 10; i++) {
                int x = w * i / 10;
                g2.drawLine(x, 0, x, h);
            }

            if (data.isEmpty()) {
                g2.setColor(Color.GRAY);
                g2.drawString("Нет данных", 10, 20);
                return;
            }

            g2.setColor(new Color(0, 200, 100));
            g2.setStroke(new BasicStroke(2));

            double range = maxVal - minVal;
            if (range == 0) range = 1;

            DataPoint prev = null;
            for (DataPoint dp : data) {
                if (dp.timestamp < start) continue;
                int x = (int) ((dp.timestamp - start) * w / (double) windowMs);
                if (x > w) break;
                int y = (int) (h - (dp.value - minVal) / range * (h - 20) - 10);
                y = Math.min(h - 5, Math.max(5, y));
                if (prev != null) {
                    int prevX = (int) ((prev.timestamp - start) * w / (double) windowMs);
                    int prevY = (int) (h - (prev.value - minVal) / range * (h - 20) - 10);
                    prevY = Math.min(h - 5, Math.max(5, prevY));
                    g2.drawLine(prevX, prevY, x, y);
                }
                prev = dp;
            }

            if (!data.isEmpty()) {
                DataPoint last = data.get(data.size() - 1);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Monospaced", Font.BOLD, 12));
                String valStr = String.format("%.1f", last.value);
                g2.drawString(valStr, w - 80, 20);
            }
        }

        private static class DataPoint {
            final long timestamp;
            final double value;
            DataPoint(long ts, double val) { timestamp = ts; value = val; }
        }
    }
}