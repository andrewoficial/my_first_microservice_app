package org.example.gui.devices.tt5166.control;

import com.fazecast.jSerialComm.SerialPort;
import lombok.extern.slf4j.Slf4j;
import org.example.gui.devices.tt5166.emulation.TT5166ModbusUtil;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * COM-транспорт клиента термокамеры TT5166 (Modbus RTU, 38400 8E1).
 * Отправляет запросы и получает ответы.
 */
@Slf4j
public class TT5166ModbusCommunicationService {

    public static final int DEFAULT_BAUD = 38400;
    private static final int READ_TIMEOUT_MS = 50;
    private static final long FRAME_TIMEOUT_MS = 120;

    private SerialPort port;
    private volatile boolean running = false;
    private Thread readerThread;
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    private final List<Consumer<byte[]>> responseListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<String>> logListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<String>> statusListeners = new CopyOnWriteArrayList<>();

    private final ScheduledExecutorService pollScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "TT5166Client-Poll"); t.setDaemon(true); return t;
    });
    private volatile boolean pollingEnabled = false;
    private volatile Runnable pollAction;

    public synchronized boolean open(String portName) {
        close();
        port = SerialPort.getCommPort(portName);
        port.setBaudRate(DEFAULT_BAUD);
        port.setNumDataBits(8);
        port.setNumStopBits(SerialPort.ONE_STOP_BIT);
        port.setParity(SerialPort.EVEN_PARITY);
        port.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, READ_TIMEOUT_MS, 0);
        if (!port.openPort()) { port = null; return false; }
        running = true;
        startReader();
        fireStatus("Подключено: " + portName);
        fireLog("Открыт " + portName + " @ 38400 8E1");
        return true;
    }

    public boolean isConnected() { return running && port != null && port.isOpen(); }

    public synchronized void close() {
        running = false;
        setPollingEnabled(false);
        readerThread = null;
        if (port != null && port.isOpen()) port.closePort();
        port = null;
        synchronized (buffer) { buffer.reset(); }
        fireStatus("Отключено");
    }

    public void addResponseListener(Consumer<byte[]> l) { responseListeners.add(l); }
    public void addLogListener(Consumer<String> l) { logListeners.add(l); }
    public void addStatusListener(Consumer<String> l) { statusListeners.add(l); }

    public void setPollingEnabled(boolean enable, Runnable action) {
        this.pollAction = action;
        setPollingEnabled(enable);
    }

    private void setPollingEnabled(boolean enable) {
        if (pollingEnabled == enable) return;
        pollingEnabled = enable;
        if (enable && pollAction != null) {
            pollScheduler.scheduleWithFixedDelay(pollAction, 0, 1000, TimeUnit.MILLISECONDS);
        } else {
            pollScheduler.shutdownNow();
        }
    }

    public void sendRequest(byte[] data) {
        if (!isConnected()) return;
        port.writeBytes(data, data.length);
        fireLog("← " + TT5166ModbusUtil.bytesToHex(data));
    }

    public void shutdown() { running = false; pollScheduler.shutdownNow(); close(); }

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
                        if (!collecting) { collecting = true; synchronized (buffer) { buffer.reset(); } }
                        synchronized (buffer) { buffer.write(single[0]); }
                        lastByteAt = now;
                    } else if (collecting && (now - lastByteAt > FRAME_TIMEOUT_MS)) {
                        processBuffer();
                        collecting = false;
                    }
                } catch (Exception e) { if (running) log.error("TT5166Client read error", e); }
            }
        }, "TT5166Client-Reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    private void processBuffer() {
        byte[] data;
        synchronized (buffer) { data = buffer.toByteArray(); buffer.reset(); }
        if (data.length < 4) return;
        int frameLen = TT5166ModbusUtil.expectedFrameLength(data);
        if (frameLen <= 0 || data.length < frameLen) return;
        byte[] frame = Arrays.copyOf(data, frameLen);
        fireLog("→ " + TT5166ModbusUtil.bytesToHex(frame));
        for (Consumer<byte[]> l : responseListeners) {
            try { l.accept(frame); } catch (Exception ignored) {}
        }
    }

    private void fireLog(String line) {
        for (Consumer<String> l : logListeners) { try { l.accept(line); } catch (Exception ignored) {} }
    }
    private void fireStatus(String s) {
        for (Consumer<String> l : statusListeners) { try { l.accept(s); } catch (Exception ignored) {} }
    }
}