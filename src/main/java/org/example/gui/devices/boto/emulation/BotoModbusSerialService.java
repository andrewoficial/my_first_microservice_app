package org.example.gui.devices.boto.emulation;

import com.fazecast.jSerialComm.SerialPort;
import lombok.extern.slf4j.Slf4j;
import org.example.device.protBoto.BotoModbusUtil;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * COM-транспорт эмулятора BOTO (Modbus RTU, 9600 8N1).
 * Принимает Modbus-запросы, обрабатывает через {@link BotoModbusResponder},
 * возвращает ответы.
 */
@Slf4j
public final class BotoModbusSerialService {

    public static final int DEFAULT_BAUD = 9600;

    private final BotoModbusResponder responder;
    private SerialPort port;
    private volatile boolean running = false;
    private Thread readerThread;
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private final List<Consumer<String>> logListeners = new CopyOnWriteArrayList<>();

    private static final int READ_TIMEOUT_MS = 50;
    private static final long FRAME_TIMEOUT_MS = 120;
    private byte[] lastFrame = null;
    private long lastFrameTime = 0;

    public BotoModbusSerialService(BotoModbusResponder responder) {
        this.responder = responder;
    }

    public boolean openPort(String portName) {
        if (port != null && port.isOpen()) closePort();
        port = SerialPort.getCommPort(portName);
        port.setBaudRate(DEFAULT_BAUD);
        port.setNumDataBits(8);
        port.setNumStopBits(SerialPort.ONE_STOP_BIT);
        port.setParity(SerialPort.NO_PARITY);
        port.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, READ_TIMEOUT_MS, 0);

        if (!port.openPort()) {
            log.error("BotoEmu: не удалось открыть порт {}", portName);
            return false;
        }
        running = true;
        startReader();
        log.info("BotoEmu: порт открыт {} @ 9600 8N1", portName);
        fireLog("Порт открыт: " + portName + " @ 9600 8N1");
        return true;
    }

    public boolean isConnected() { return running && port != null && port.isOpen(); }

    public void closePort() {
        running = false;
        readerThread = null;
        if (port != null && port.isOpen()) port.closePort();
        port = null;
        synchronized (buffer) { buffer.reset(); }
    }

    public void addLogListener(Consumer<String> l) { logListeners.add(l); }
    public void removeLogListener(Consumer<String> l) { logListeners.remove(l); }

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
                            synchronized (buffer) { buffer.reset(); }
                        }
                        synchronized (buffer) { buffer.write(single[0]); }
                        lastByteAt = now;
                    } else if (collecting && (now - lastByteAt > FRAME_TIMEOUT_MS)) {
                        processBuffer();
                        collecting = false;
                    }
                } catch (Exception e) {
                    if (running) log.error("BotoEmu read error", e);
                }
            }
        }, "BotoEmu-Reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    private void processBuffer() {
        byte[] data;
        synchronized (buffer) {
            data = buffer.toByteArray();
            buffer.reset();
        }
        if (data.length < 4) return;

        int frameLen = BotoModbusUtil.expectedRequestFrameLength(data);
        if (frameLen <= 0 || data.length < frameLen) return;

        byte[] frame = Arrays.copyOf(data, frameLen);

        long now = System.currentTimeMillis();
        if (lastFrame != null && Arrays.equals(lastFrame, frame) && (now - lastFrameTime) < 200) {
            return;
        }
        lastFrame = frame;
        lastFrameTime = now;

        fireLog("RX << " + BotoModbusUtil.bytesToHex(data));
        byte[] response = responder.processRequest(frame);
        if (response != null && response.length > 0) {
            port.writeBytes(response, response.length);
            fireLog("TX >> " + BotoModbusUtil.bytesToHex(response));
        } else {
            fireLog("TX >> (нет ответа)");
        }
    }

    private void fireLog(String line) {
        for (Consumer<String> l : logListeners) {
            try { l.accept(line); } catch (Exception ignored) {}
        }
    }
}
