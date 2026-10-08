package org.example.gui.devices.owon.spe3051.control;

import com.fazecast.jSerialComm.SerialPort;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * COM-транспорт панели управления OWON SPE3051 (ASCII/SCPI протокол).
 *
 * <p>Параметры связи соответствуют {@code OWON_SPE3051}: 9600 бод, 8 бит данных,
 * чётность EVEN, 1 стоп-бит, конец строки {@code CR}. Прибор отвечает
 * ASCII-строками, поэтому поток разбирается на строки по {@code CR}/{@code LF}
 * (с таймаут-добивкой, если терминатор не пришёл).
 */
@Slf4j
public class OwonSpe3051CommunicationService {

    /** Скорость по умолчанию для OWON SPE3051 (см. {@code OWON_SPE3051}). */
    public static final int DEFAULT_BAUD = 115200;

    private static final int READ_TIMEOUT_MS = 20;
    private static final long LINE_TIMEOUT_MS = 80;
    private static final int MAX_LINE = 4096;

    private SerialPort port;
    private volatile boolean running = false;
    private Thread readerThread;
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    private final List<Consumer<String>> lineListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<String>> logListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<String>> statusListeners = new CopyOnWriteArrayList<>();

    public synchronized boolean open(String portName) {
        close();
        port = SerialPort.getCommPort(portName);
        port.setBaudRate(DEFAULT_BAUD);
        port.setNumDataBits(8);
        port.setNumStopBits(SerialPort.ONE_STOP_BIT);
        port.setParity(SerialPort.NO_PARITY);
        port.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, READ_TIMEOUT_MS, 0);
        if (!port.openPort()) {
            port = null;
            return false;
        }
        running = true;
        startReader();
        fireStatus("Подключено: " + portName);
        fireLog("Открыт " + portName + " @ " + DEFAULT_BAUD + " 8N1, CR+LF");
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
        fireStatus("Отключено");
    }

    public void addLineListener(Consumer<String> listener) {
        lineListeners.add(listener);
    }

    public void addLogListener(Consumer<String> listener) {
        logListeners.add(listener);
    }

    public void addStatusListener(Consumer<String> listener) {
        statusListeners.add(listener);
    }

    public void send(byte[] data) {
        if (!isConnected() || data == null || data.length == 0) {
            return;
        }
        port.writeBytes(data, data.length);
        fireLog("<< " + ascii(data));
    }

    public void shutdown() {
        close();
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
                            processLine();
                            collecting = false;
                        } else if (buffer.size() >= MAX_LINE) {
                            processLine();
                            collecting = false;
                        }
                    } else if (collecting && (now - lastByteAt > LINE_TIMEOUT_MS)) {
                        processLine();
                        collecting = false;
                    }
                } catch (Exception e) {
                    if (running) {
                        log.error("OwonSpe3051 client read error", e);
                    }
                }
            }
        }, "OwonSpe3051-Reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    private void processLine() {
        byte[] data;
        synchronized (buffer) {
            data = buffer.toByteArray();
            buffer.reset();
        }
        String line = new String(data, StandardCharsets.US_ASCII).trim();
        if (line.isEmpty()) {
            return;
        }
        fireLog(">> " + line);
        for (Consumer<String> listener : lineListeners) {
            try {
                listener.accept(line);
            } catch (Exception ignored) {
                // один плохой слушатель не должен ронять разбор потока
            }
        }
    }

    public static String ascii(byte[] bytes) {
        if (bytes == null) {
            return "";
        }
        return new String(bytes, StandardCharsets.US_ASCII).replace("\r", "\\r").replace("\n", "\\n");
    }

    public static String hex(byte[] bytes) {
        if (bytes == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(bytes.length * 3);
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }

    private void fireLog(String line) {
        for (Consumer<String> listener : logListeners) {
            try {
                listener.accept(line);
            } catch (Exception ignored) {
                // лог не должен ломать обмен
            }
        }
    }

    private void fireStatus(String status) {
        for (Consumer<String> listener : statusListeners) {
            try {
                listener.accept(status);
            } catch (Exception ignored) {
                // статус не должен ломать обмен
            }
        }
    }
}
