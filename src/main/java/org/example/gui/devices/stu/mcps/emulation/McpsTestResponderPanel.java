package org.example.gui.devices.stu.mcps.emulation;

import org.example.gui.devices.stu.mcps.AsyncLogger;
import org.example.gui.devices.stu.mcps.control.McpsCommunicationService;

import javax.swing.*;
import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class McpsTestResponderPanel extends JPanel {

    private final McpsCommunicationService service;
    private final AsyncLogger logger;

    private final JButton startBtn = new JButton("Старт слушателя");
    private final JButton stopBtn = new JButton("Стоп");
    private final JComboBox<String> portCombo = new JComboBox<>();

    private final OscilloscopePanel oscilloscopePanel = new OscilloscopePanel();
    private final JTextArea logArea = new JTextArea();
    private final JLabel[] channelStateLamps = new JLabel[8];
    private final JLabel[] channelInfoLabels = new JLabel[8];

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
    private final Timer refreshTimer = new Timer(50, e -> oscilloscopePanel.repaint());
    private final List<String> logLines = new CopyOnWriteArrayList<>();

    private final Map<Integer, List<StateEvent>> channelEvents = new ConcurrentHashMap<>();
    private final Map<Integer, List<Long>> channelPulseTimes = new ConcurrentHashMap<>();
    private final boolean[] channelOnState = new boolean[8];
    private final long[] channelPulseCount = new long[8];

    private String deviceMode = "Manual";   // <-- NEW: for @RDMD support

    private static final int CHANNEL_COUNT = 8;
    private static final int WINDOW_MS = 2000;
    private static final int MAX_LOG_LINES = 200;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final int METRICS_WINDOW = 20;

    private String lastLogMsg = "";
    private long lastLogTs = 0;

    public McpsTestResponderPanel(AsyncLogger logger) {
        this.logger = logger;
        this.service = new McpsCommunicationService(logger);

        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createTitledBorder("Тестовый Responder (симулятор устройства)"));

        JPanel top = createTopPanel();
        add(top, BorderLayout.NORTH);
        add(oscilloscopePanel, BorderLayout.CENTER);
        add(createBottomPanel(), BorderLayout.SOUTH);

        service.addResponseListener(this::handleIncomingCommand);

        startBtn.addActionListener(e -> startListening());
        stopBtn.addActionListener(e -> stopListening());
        stopBtn.setEnabled(false);

        for (int ch = 1; ch <= CHANNEL_COUNT; ch++) {
            channelEvents.put(ch, new CopyOnWriteArrayList<>());
            channelPulseTimes.put(ch, new CopyOnWriteArrayList<>());
            channelOnState[ch - 1] = false;
            channelPulseCount[ch - 1] = 0;
        }

        scheduler.scheduleAtFixedRate(this::updateMetricsUI, 200, 500, TimeUnit.MILLISECONDS);

        refreshTimer.start();
    }

    private JPanel createTopPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p.add(new JLabel("COM порт:"));
        refreshPorts();
        p.add(portCombo);
        p.add(startBtn);
        p.add(stopBtn);
        return p;
    }

    private JPanel createBottomPanel() {
        JPanel bottom = new JPanel(new GridLayout(1, 2, 10, 0));

        JPanel statePanel = new JPanel(new GridLayout(CHANNEL_COUNT, 1, 2, 2));
        statePanel.setBorder(BorderFactory.createTitledBorder("Состояние каналов"));
        for (int i = 0; i < CHANNEL_COUNT; i++) {
            int ch = i + 1;
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));

            channelStateLamps[i] = new JLabel("\u25CF");
            channelStateLamps[i].setForeground(Color.GRAY);
            channelStateLamps[i].setFont(new Font("Monospaced", Font.BOLD, 14));

            channelInfoLabels[i] = new JLabel("CH" + ch + ": ВЫКЛ | #0 | 0.0Гц | \u00B10мс");

            row.add(channelStateLamps[i]);
            row.add(channelInfoLabels[i]);
            statePanel.add(row);
        }

        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        logArea.setRows(8);
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Лог событий"));

        bottom.add(statePanel);
        bottom.add(logScroll);

        return bottom;
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
        if (port == null) return;

        if (service.openPort(port)) {
            startBtn.setEnabled(false);
            stopBtn.setEnabled(true);
            logger.info("Responder started on " + port);
            addLog("Слушатель запущен на порту " + port);
        }
    }

    private void stopListening() {
        service.closePort();
        startBtn.setEnabled(true);
        stopBtn.setEnabled(false);
        logger.info("Responder stopped");
        addLog("Слушатель остановлен");
    }

    /**
     * Обновлённый обработчик ВСЕХ команд протокола из "Протокол ASCII.docx".
     * Теперь @RO/@RPOU возвращают реальное состояние (синхронно с UI),
     * добавлены @RDMD, @RI, @RPIN.
     */
    private void handleIncomingCommand(String response) {
        long now = System.currentTimeMillis();
        String cmd = response.trim();

        try {
            if (cmd.startsWith("@WR")) {
                String rest = cmd.substring(3).trim();
                String[] parts = rest.split("[ ,]+");
                if (parts.length < 2) return;

                String chStr = parts[0];
                int ch = Integer.parseInt(chStr);
                if (ch < 1 || ch > CHANNEL_COUNT) {
                    addLog("Invalid CH in @WR: " + ch);
                    return;
                }
                int state = Integer.parseInt(parts[1]);
                int idx = ch - 1;

                if (state == 1) {
                    long duration = parts.length > 2 ? Long.parseLong(parts[2]) : -1L;
                    setChannelOn(ch, now, duration);
                    service.sendCommand("@WR" + chStr + " OK");
                } else if (state == 0) {
                    setChannelOff(ch, now);
                    service.sendCommand("@WR" + chStr + " OK");
                }
            } else if (cmd.startsWith("@RDMD")) {
                service.sendCommand("@RAMD " + deviceMode);
                addLog("Read mode → " + deviceMode);
            } else if (cmd.startsWith("@RI")) {
                String rest = cmd.substring(3).trim();
                String chStr = rest.split("[ ,]+")[0];
                // Входы IN не симулируются в текущей версии UI.
                // Всегда возвращаем 0. Если нужно — добавим массив состояний + чекбоксы.
                service.sendCommand("@RA" + chStr + " 0");
            } else if (cmd.startsWith("@RO")) {
                String rest = cmd.substring(3).trim();
                String chStr = rest.split("[ ,]+")[0];
                int ch = Integer.parseInt(chStr);
                String val = (ch >= 1 && ch <= CHANNEL_COUNT && channelOnState[ch - 1]) ? "1" : "0";
                service.sendCommand("@RA" + chStr + " " + val);
            } else if (cmd.startsWith("@RPIN")) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < CHANNEL_COUNT; i++) sb.append('0');
                service.sendCommand("@RAIN " + sb.toString());
                addLog("Read IN port");
            } else if (cmd.startsWith("@RPOU")) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < CHANNEL_COUNT; i++) {
                    sb.append(channelOnState[i] ? '1' : '0');
                }
                service.sendCommand("@RAOU " + sb.toString());
                addLog("Read OUT port: " + sb);
            } else {
                addLog("Unhandled command: " + cmd);
            }
        } catch (Exception ex) {
            addLog("Command parse error: " + ex.getMessage());
        }

        oscilloscopePanel.repaint();
    }

    private void setChannelOn(int ch, long now, long duration) {
        int idx = ch - 1;
        channelOnState[idx] = true;
        channelPulseCount[idx]++;

        List<Long> times = channelPulseTimes.get(ch);
        if (times != null) {
            times.add(now);
            if (times.size() > METRICS_WINDOW) times.remove(0);
        }

        addEvent(ch, new StateEvent(now, true));

        if (duration > 0) {
            addLog(String.format("CH%02d: ВКЛ на %d мс (импульс #%d)", ch, duration, channelPulseCount[idx]));
            scheduler.schedule(() -> {
                if (channelOnState[idx]) {
                    setChannelOff(ch, now + duration);
                }
            }, duration, TimeUnit.MILLISECONDS);
        } else {
            addLog(String.format("CH%02d: ВКЛ постоянно (импульс #%d)", ch, channelPulseCount[idx]));
        }
    }

    private void setChannelOff(int ch, long now) {
        int idx = ch - 1;
        if (!channelOnState[idx]) return;
        channelOnState[idx] = false;
        addEvent(ch, new StateEvent(now, false));
        addLog(String.format("CH%02d: ВЫКЛ", ch));
    }

    private void addEvent(int ch, StateEvent event) {
        List<StateEvent> events = channelEvents.get(ch);
        if (events != null) {
            events.add(event);
            long cutoff = System.currentTimeMillis() - WINDOW_MS - 1000;
            while (!events.isEmpty() && events.get(0).timestamp < cutoff) {
                events.remove(0);
            }
        }
    }

    private void updateMetricsUI() {
        SwingUtilities.invokeLater(() -> {
            for (int ch = 1; ch <= CHANNEL_COUNT; ch++) {
                int idx = ch - 1;
                boolean on = channelOnState[idx];
                double freq = calcFrequency(channelPulseTimes.get(ch));
                double jitter = calcJitter(channelPulseTimes.get(ch));
                channelStateLamps[idx].setForeground(on ? new Color(0, 220, 80) : Color.GRAY);
                channelInfoLabels[idx].setText(String.format("CH%02d: %s | #%d | %.1fГц | \u00B1%.0fмс",
                        ch, on ? "ВКЛ " : "ВЫКЛ", channelPulseCount[idx], freq, jitter));
            }
        });
    }

    private double calcFrequency(List<Long> times) {
        if (times == null || times.size() < 2) return 0;
        double sum = 0;
        for (int i = 1; i < times.size(); i++) {
            sum += times.get(i) - times.get(i - 1);
        }
        double avgMs = sum / (times.size() - 1);
        return avgMs > 0 ? 1000.0 / avgMs : 0;
    }

    private double calcJitter(List<Long> times) {
        if (times == null || times.size() < 3) return 0;
        double sum = 0;
        for (int i = 1; i < times.size(); i++) {
            sum += times.get(i) - times.get(i - 1);
        }
        double avgMs = sum / (times.size() - 1);
        double dev = 0;
        for (int i = 1; i < times.size(); i++) {
            dev += Math.abs((times.get(i) - times.get(i - 1)) - avgMs);
        }
        return dev / (times.size() - 1);
    }

    private void addLog(String msg) {
        long now = System.currentTimeMillis();
        if (msg.equals(lastLogMsg) && now - lastLogTs < 1000) return;
        lastLogMsg = msg;
        lastLogTs = now;

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

    public void shutdown() {
        stopListening();
        refreshTimer.stop();
        scheduler.shutdownNow();
    }

    // ================== Событие переключения состояния канала ==================
    private static class StateEvent {
        final long timestamp;
        final boolean on;

        StateEvent(long timestamp, boolean on) {
            this.timestamp = timestamp;
            this.on = on;
        }
    }

    // ================== Осциллограф ==================
    private class OscilloscopePanel extends JPanel {
        private static final int Y_MARGIN = 10;
        private static final Color BG = new Color(16, 16, 24);
        private static final Color GRID = new Color(36, 36, 48);
        private static final Color WAVE = new Color(0, 200, 100);

        OscilloscopePanel() {
            setPreferredSize(new Dimension(800, 400));
            setBackground(BG);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            long now = System.currentTimeMillis();
            long windowStart = now - WINDOW_MS;
            int channelH = (h - Y_MARGIN * 2) / CHANNEL_COUNT;

            drawGrid(g2, w, h, channelH, now, windowStart);

            for (int ch = 1; ch <= CHANNEL_COUNT; ch++) {
                int yBase = Y_MARGIN + (ch - 1) * channelH;
                int lowY = yBase + (int) (channelH * 0.72);
                int highY = yBase + (int) (channelH * 0.18);

                g2.setColor(new Color(180, 180, 200));
                g2.setFont(new Font("Monospaced", Font.BOLD, 11));
                g2.drawString("CH" + ch, 5, yBase + 13);

                List<StateEvent> events = channelEvents.get(ch);
                if (events == null || events.isEmpty()) {
                    g2.setColor(WAVE);
                    g2.drawLine(0, lowY, w, lowY);
                    continue;
                }

                boolean currentOn = false;
                for (StateEvent evt : events) {
                    if (evt.timestamp <= windowStart) {
                        currentOn = evt.on;
                    }
                }

                g2.setColor(WAVE);
                g2.setStroke(new BasicStroke(2));
                int prevX = 0;
                int prevY = currentOn ? highY : lowY;

                for (StateEvent evt : events) {
                    if (evt.timestamp < windowStart) continue;
                    int x = (int) ((evt.timestamp - windowStart) * w / WINDOW_MS);
                    if (x > w) break;

                    g2.drawLine(prevX, prevY, Math.min(x, w), prevY);

                    currentOn = evt.on;
                    int newY = currentOn ? highY : lowY;
                    if (x <= w) {
                        g2.drawLine(x, prevY, x, newY);
                    }

                    prevX = x;
                    prevY = newY;
                }

                if (prevX < w) {
                    g2.drawLine(prevX, prevY, w, prevY);
                }
            }
        }

        private void drawGrid(Graphics2D g2, int w, int h, int channelH, long now, long windowStart) {
            g2.setColor(GRID);
            g2.setStroke(new BasicStroke(1));

            for (int i = 0; i <= CHANNEL_COUNT; i++) {
                int y = Y_MARGIN + i * channelH;
                g2.drawLine(0, y, w, y);
            }

            int gridCols = WINDOW_MS / 200;
            for (int i = 1; i < gridCols; i++) {
                int x = i * w / gridCols;
                g2.drawLine(x, Y_MARGIN, x, h - Y_MARGIN);
            }

            g2.setFont(new Font("Monospaced", Font.PLAIN, 9));
            g2.setColor(Color.GRAY);
            for (int i = 0; i <= gridCols; i++) {
                int x = i * w / gridCols;
                long t = windowStart + i * 200L;
                String label = String.format("%.1fs", (t - now) / 1000.0);
                g2.drawString(label, x + 3, h - 3);
            }
        }
    }
}
