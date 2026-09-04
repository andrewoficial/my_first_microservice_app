package org.example.gui.devices.bkm4.emulation;

import com.fazecast.jSerialComm.SerialPort;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * COM-транспорт эмулятора БКМ-4 (ASCII, 9600 8N1, CR).
 * <p>
 * Обрабатывает входящие команды через {@link Bkm4Responder} и возвращает
 * ASCII-ответ с завершающим CR.
 */
@Slf4j
public final class Bkm4EmulationService {

    public static final int DEFAULT_BAUD = 9600;

    private final Bkm4Responder responder;

    private SerialPort port;
    private volatile boolean running = false;
    private Thread readerThread;
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private final List<Consumer<String>> responseListeners = new CopyOnWriteArrayList<>();

    private static final int READ_TIMEOUT_MS = 50;
    private static final long IDLE_FLUSH_MS = 120;

    public Bkm4EmulationService(Bkm4Responder responder) {
        this.responder = responder;
    }

    public boolean openPort(String portName, int baudRate) {
        if (port != null && port.isOpen()) {
            closePort();
        }
        int baud = baudRate > 0 ? baudRate : DEFAULT_BAUD;

        port = SerialPort.getCommPort(portName);
        port.setBaudRate(baud);
        port.setNumDataBits(8);
        port.setNumStopBits(SerialPort.ONE_STOP_BIT);
        port.setParity(SerialPort.NO_PARITY);
        port.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, READ_TIMEOUT_MS, 0);

        if (!port.openPort()) {
            log.error("Bkm4Emu: не удалось открыть порт {}", portName);
            return false;
        }
        port.setBaudRate(baud);
        port.setNumDataBits(8);
        port.setNumStopBits(SerialPort.ONE_STOP_BIT);
        port.setParity(SerialPort.NO_PARITY);

        running = true;
        startReader();
        log.info("Bkm4Emu: порт открыт {} @ {} 8N1", portName, baud);
        return true;
    }

    public boolean isConnected() {
        return running && port != null && port.isOpen();
    }

    public void closePort() {
        running = false;
        readerThread = null;
        if (port != null && port.isOpen()) {
            port.closePort();
        }
        port = null;
        synchronized (buffer) {
            buffer.reset();
        }
    }

    public void addResponseListener(Consumer<String> listener) {
        responseListeners.add(listener);
    }

    public void removeResponseListener(Consumer<String> listener) {
        responseListeners.remove(listener);
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
                    } else if (collecting && (now - lastByteAt > IDLE_FLUSH_MS)) {
                        flushBuffer();
                        collecting = false;
                    }
                } catch (Exception e) {
                    if (running) {
                        log.error("Bkm4Emu read error", e);
                    }
                }
            }
        }, "Bkm4Emu-Reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    /**
     * Отправляет ответ эмулятора в порт с завершающим CR.
     */
    private void sendResponse(String response) {
        if (!isConnected()) {
            return;
        }
        String frame = response != null && response.endsWith("\r")
                ? response
                : (response == null ? "@ERROR" : response) + "\r";
        byte[] bytes = frame.getBytes(StandardCharsets.US_ASCII);
        int written = port.writeBytes(bytes, bytes.length);
        log.info("Bkm4Emu TX ({}): {}", written, frame.trim());
        for (Consumer<String> l : responseListeners) {
            try {
                l.accept("← " + frame.trim());
            } catch (Exception ex) {
                log.warn("Bkm4Emu listener error: {}", ex.getMessage());
            }
        }
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
        if (text.isEmpty()) {
            return;
        }
        log.info("Bkm4Emu RX: {}", text);
        for (Consumer<String> l : responseListeners) {
            try {
                l.accept("→ " + text);
            } catch (Exception ex) {
                log.warn("Bkm4Emu listener error: {}", ex.getMessage());
            }
        }
        sendResponse(responder.processCommand(text));
    }
}