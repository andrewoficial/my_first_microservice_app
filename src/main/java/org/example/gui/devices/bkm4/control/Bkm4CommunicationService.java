package org.example.gui.devices.bkm4.control;

import com.fazecast.jSerialComm.SerialPort;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Коммуникационный сервис для БКМ-4 (RS-232C, 9600 8N1, ASCII/CR).
 * Открывает COM-порт, отправляет команды и периодически опрашивает фактический расход.
 */
@Slf4j
public class Bkm4CommunicationService {

    public static final int DEFAULT_BAUD = 9600;
    private static final int DEFAULT_POLL_MS = 1000;
    private static final int READ_TIMEOUT_MS = 50;

    private SerialPort port;
    private volatile boolean running = false;
    private Thread readerThread;
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    private final ScheduledExecutorService pollScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "Bkm4Client-Poll");
        t.setDaemon(true);
        return t;
    });
    private volatile boolean pollingEnabled = false;

    private final List<Consumer<Double>> flowListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<String>> logListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<String>> statusListeners = new CopyOnWriteArrayList<>();

    public synchronized boolean open(String portName, int baudRate) {
        close();
        int baud = baudRate > 0 ? baudRate : DEFAULT_BAUD;
        port = SerialPort.getCommPort(portName);
        port.setBaudRate(baud);
        port.setNumDataBits(8);
        port.setNumStopBits(SerialPort.ONE_STOP_BIT);
        port.setParity(SerialPort.NO_PARITY);
        port.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, READ_TIMEOUT_MS, 0);
        if (!port.openPort()) {
            log.error("Bkm4Client: не удалось открыть порт {}", portName);
            port = null;
            return false;
        }
        port.setBaudRate(baud);
        port.setNumDataBits(8);
        port.setNumStopBits(SerialPort.ONE_STOP_BIT);
        port.setParity(SerialPort.NO_PARITY);

        running = true;
        startReader();
        fireStatus("Подключено: " + portName + " @ " + baud + " 8N1");
        fireLog("Открыт " + portName + " @ " + baud + " 8N1");
        return true;
    }

    public boolean isConnected() {
        return running && port != null && port.isOpen();
    }

    public synchronized void close() {
        running = false;
        readerThread = null;
        if (port != null && port.isOpen()) {
            port.closePort();
        }
        port = null;
        synchronized (buffer) {
            buffer.reset();
        }
        setPollingEnabled(false);
        fireStatus("Отключено");
        fireLog("Порт закрыт");
    }

    public void addFlowListener(Consumer<Double> l) {
        flowListeners.add(l);
    }

    public void addLogListener(Consumer<String> l) {
        logListeners.add(l);
    }

    public void addStatusListener(Consumer<String> l) {
        statusListeners.add(l);
    }

    /**
     * Отправляет команду (напр. {@code &A?}, {@code &S1500}) с завершающим CR.
     */
    public void sendCommand(String cmd) {
        if (!isConnected() || cmd == null || cmd.isEmpty()) {
            return;
        }
        String frame = cmd.endsWith("\r") ? cmd : cmd + "\r";
        byte[] bytes = frame.getBytes(StandardCharsets.US_ASCII);
        int written = port.writeBytes(bytes, bytes.length);
        fireLog("→ " + cmd.trim());
        log.info("Bkm4Client TX ({}): {}", written, cmd.trim());
    }

    public void setPollingEnabled(boolean enable) {
        if (pollingEnabled == enable) {
            return;
        }
        pollingEnabled = enable;
        if (enable) {
            pollScheduler.scheduleWithFixedDelay(this::pollOnce, 0, DEFAULT_POLL_MS, TimeUnit.MILLISECONDS);
        } else {
            pollScheduler.shutdownNow();
        }
    }

    private void pollOnce() {
        if (!isConnected() || !pollingEnabled) {
            return;
        }
        sendCommand("&F?");
    }

    private void startReader() {
        readerThread = new Thread(() -> {
            byte[] single = new byte[1];
            long lastByteAt = 0;
            boolean collecting = false;
            while (running && port != null && port.isOpen()) {
                try {
                    int r = port.readBytes(single, 1);
                    long now = System.currentTimeMillis();
                    if (r > 0) {
                        if (!collecting) {
                            collecting = true;
                            synchronized (buffer) {
                                buffer.reset();
                            }
                        }
                        synchronized (buffer) {
                            buffer.write(single[0]);
                        }
                        lastByteAt = now;
                        if (single[0] == '\r' || single[0] == '\n') {
                            flushBuffer();
                            collecting = false;
                        }
                    } else if (collecting && (now - lastByteAt > 120)) {
                        flushBuffer();
                        collecting = false;
                    }
                } catch (Exception e) {
                    if (running) {
                        log.error("Bkm4Client read error", e);
                    }
                }
            }
        }, "Bkm4Client-Reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    private void flushBuffer() {
        byte[] data;
        synchronized (buffer) {
            data = buffer.toByteArray();
            buffer.reset();
        }
        if (data.length == 0) {
            return;
        }
        int end = data.length;
        while (end > 0 && (data[end - 1] == '\r' || data[end - 1] == '\n')) {
            end--;
        }
        if (end <= 0) {
            return;
        }
        String text = new String(data, 0, end, StandardCharsets.US_ASCII);
        fireLog("← " + text);
        handleAnswer(text);
    }

    private void handleAnswer(String text) {
        if (text.startsWith("@F")) {
            double flow = extractNumber(text.substring(2));
            for (Consumer<Double> l : flowListeners) {
                try {
                    l.accept(flow);
                } catch (Exception ex) {
                    log.warn("Bkm4Client flow listener error", ex);
                }
            }
        } else if (text.equalsIgnoreCase("@ERROR")) {
            fireStatus("@ERROR");
        }
    }

    private static double extractNumber(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isDigit(c) || c == '.' || c == ',' || c == '-') {
                sb.append(c);
            }
        }
        if (sb.length() == 0) {
            return 0;
        }
        try {
            return Double.parseDouble(sb.toString().replace(',', '.'));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void fireLog(String line) {
        for (Consumer<String> l : logListeners) {
            try {
                l.accept(line);
            } catch (Exception ex) {
                log.warn("Bkm4Client log listener error", ex);
            }
        }
    }

    private void fireStatus(String s) {
        for (Consumer<String> l : statusListeners) {
            try {
                l.accept(s);
            } catch (Exception ex) {
                log.warn("Bkm4Client status listener error", ex);
            }
        }
    }

    /**
     * Прервать фоновые потоки (вызывается при закрытии окна).
     */
    public void shutdown() {
        running = false;
        pollScheduler.shutdownNow();
        setPollingEnabled(false);
        close();
    }
}