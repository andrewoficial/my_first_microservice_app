package org.example.gui.devices.esp32.kantser.emu.ble;

import com.fazecast.jSerialComm.SerialPort;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Автономный COM-транспорт для ESP_KANTSER_BLE_EMU (ASCII, CR).
 * Стандарт платы: 115200 8N1.
 * <p>
 * Multiline mode (HELP/ADST?): lines accumulate until idle gap, delivered as one block with {@code \n}.
 */
@Slf4j
public final class KantserBleCommunicationService {

    public static final int DEFAULT_BAUD = 115200;
    public static final int DEFAULT_PARITY = SerialPort.NO_PARITY;

    private SerialPort port;
    private volatile boolean running = false;
    private volatile int activeBaud = DEFAULT_BAUD;
    private volatile boolean multilineMode = false;
    private Thread readerThread;
    private final List<Consumer<String>> responseListeners = new CopyOnWriteArrayList<>();
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private Consumer<String> connectionStatusListener;

    private static final int READ_TIMEOUT_MS = 50;
    private static final long IDLE_FLUSH_MS = 280;
    private static final long MULTILINE_IDLE_FLUSH_MS = 800;

    public boolean openPort(String portName, int baudRate) {
        return openPort(portName, baudRate, DEFAULT_PARITY);
    }

    public boolean openPort(String portName, int baudRate, int parity) {
        if (port != null && port.isOpen()) {
            closePort();
        }

        int baud = baudRate > 0 ? baudRate : DEFAULT_BAUD;

        port = SerialPort.getCommPort(portName);
        port.setBaudRate(baud);
        port.setNumDataBits(8);
        port.setNumStopBits(SerialPort.ONE_STOP_BIT);
        port.setParity(parity);
        port.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, READ_TIMEOUT_MS, 0);

        if (!port.openPort()) {
            log.error("KantserBle: не удалось открыть порт {}", portName);
            return false;
        }

        port.setBaudRate(baud);
        port.setNumDataBits(8);
        port.setNumStopBits(SerialPort.ONE_STOP_BIT);
        port.setParity(parity);

        activeBaud = port.getBaudRate() > 0 ? port.getBaudRate() : baud;
        if (activeBaud != baud) {
            log.warn("KantserBle: запрошен baud {}, фактически {}", baud, activeBaud);
        }

        running = true;
        startReader();
        log.info("KantserBle: порт открыт {} @ {} 8{}1 (requested {})",
                portName, activeBaud, parity == SerialPort.EVEN_PARITY ? "E" : "N", baud);
        if (connectionStatusListener != null) {
            connectionStatusListener.accept("CONNECTED:" + portName + ":" + activeBaud);
        }
        return true;
    }

    public int getActiveBaud() {
        return activeBaud;
    }

    /**
     * When true, CR/LF do not complete a frame — whole block is flushed after idle silence
     * (for HELP multi-line dumps).
     */
    public void setMultilineMode(boolean multilineMode) {
        boolean wasMultiline = this.multilineMode;
        this.multilineMode = multilineMode;
        if (wasMultiline && !multilineMode) {
            synchronized (buffer) {
                if (buffer.size() > 0) {
                    flushBuffer();
                }
            }
        }
    }

    public boolean isMultilineMode() {
        return multilineMode;
    }

    public void closePort() {
        running = false;
        multilineMode = false;
        if (readerThread != null) {
            try {
                readerThread.interrupt();
                readerThread.join(1000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
        if (port != null && port.isOpen()) {
            port.closePort();
        }
        port = null;
        synchronized (buffer) {
            buffer.reset();
        }
        if (connectionStatusListener != null) {
            connectionStatusListener.accept("DISCONNECTED");
        }
    }

    public boolean isConnected() {
        return running && port != null && port.isOpen();
    }

    public void setConnectionStatusListener(Consumer<String> listener) {
        this.connectionStatusListener = listener;
    }

    public void addResponseListener(Consumer<String> listener) {
        responseListeners.add(listener);
    }

    public void removeResponseListener(Consumer<String> listener) {
        responseListeners.remove(listener);
    }

    public void sendCommand(String cmd) {
        if (!isConnected() || cmd == null || cmd.isEmpty()) {
            return;
        }
        String frame = cmd.endsWith("\r") ? cmd : cmd + "\r";
        byte[] bytes = frame.getBytes(StandardCharsets.US_ASCII);
        int written = port.writeBytes(bytes, bytes.length);
        log.info("KantserBle TX ({}): {}", written, cmd.trim());
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
                    long idleLimit = multilineMode ? MULTILINE_IDLE_FLUSH_MS : IDLE_FLUSH_MS;

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
                        // Single-line frames complete on CR/LF; multiline waits for idle
                        if (!multilineMode && (single[0] == '\r' || single[0] == '\n')) {
                            flushBuffer();
                            collecting = false;
                        }
                    } else if (collecting && (now - lastByteAt > idleLimit)) {
                        flushBuffer();
                        collecting = false;
                    }
                } catch (Exception e) {
                    if (running) {
                        log.error("KantserBle read error", e);
                    }
                }
            }
        }, "KantserBle-Reader");
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
        String text;
        if (multilineMode) {
            text = new String(data, StandardCharsets.ISO_8859_1)
                    .replace("\r\n", "\n")
                    .replace('\r', '\n')
                    .trim();
        } else {
            // strip trailing CR/LF only
            int end = data.length;
            while (end > 0 && (data[end - 1] == '\r' || data[end - 1] == '\n')) {
                end--;
            }
            if (end <= 0) {
                return;
            }
            text = new String(data, 0, end, StandardCharsets.ISO_8859_1);
        }
        if (text.isEmpty()) {
            return;
        }
        String logPreview = text.length() > 120 ? text.substring(0, 120) + "…" : text.replace('\n', '|');
        log.info("KantserBle RX: {}", logPreview);
        for (Consumer<String> l : responseListeners) {
            try {
                l.accept(text);
            } catch (Exception ex) {
                log.warn("KantserBle listener error: {}", ex.getMessage());
            }
        }
    }
}
