package org.example.gui.devices.fnirsi.dps150.control;

import com.fazecast.jSerialComm.SerialPort;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * COM-транспорт панели управления FNIRSI DPS150 (бинарный протокол).
 *
 * <p>Кадры прибора: {@code F1/A1|B1|C1 | type | len | data… | cs}, ответы —
 * {@code F0 A1 | type | len | data… | cs}. Границы ответа определяются по
 * полю длины, поэтому поток разбирается на отдельные кадры детерминированно.
 */
@Slf4j
public class FnirsiDps150CommunicationService {

    /** Скорость по умолчанию для FNIRSI DPS150 (см. {@code FNIRSI_DPS150}). */
    public static final int DEFAULT_BAUD = 115200;

    private static final byte START_RECV = (byte) 0xF0;
    private static final int READ_TIMEOUT_MS = 20;
    private static final long FRAME_TIMEOUT_MS = 60;

    private SerialPort port;
    private volatile boolean running = false;
    private Thread readerThread;
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    private final List<Consumer<byte[]>> responseListeners = new CopyOnWriteArrayList<>();
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
        fireLog("Открыт " + portName + " @ " + DEFAULT_BAUD + " 8N1");
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

    public void addResponseListener(Consumer<byte[]> listener) {
        responseListeners.add(listener);
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
        fireLog("<< " + hex(data));
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
                    } else if (collecting && (now - lastByteAt > FRAME_TIMEOUT_MS)) {
                        processBuffer();
                        collecting = false;
                    }
                } catch (Exception e) {
                    if (running) {
                        log.error("FnirsiDps150 client read error", e);
                    }
                }
            }
        }, "FnirsiDps150-Reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    private void processBuffer() {
        byte[] data;
        synchronized (buffer) {
            data = buffer.toByteArray();
            buffer.reset();
        }
        int off = 0;
        while (off + 4 <= data.length) {
            if (data[off] != START_RECV) {
                off++;
                continue;
            }
            int len = data[off + 3] & 0xFF;
            int frameLen = 4 + len + 1;
            if (off + frameLen > data.length) {
                break;
            }
            byte[] frame = Arrays.copyOfRange(data, off, off + frameLen);
            fireLog(">> " + hex(frame));
            for (Consumer<byte[]> listener : responseListeners) {
                try {
                    listener.accept(frame);
                } catch (Exception ignored) {
                    // один плохой слушатель не должен ронять разбор потока
                }
            }
            off += frameLen;
        }
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
