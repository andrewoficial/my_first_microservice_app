package org.example.device.protEdwardsD397;

import com.fazecast.jSerialComm.SerialPort;
import lombok.extern.slf4j.Slf4j;
import org.example.utilites.MyUtilities;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@Slf4j
public class EdwardsCommunicationService {
    private SerialPort port;
    private volatile boolean running = false;
    private Thread readerThread;
    private final List<Consumer<byte[]>> listeners = new CopyOnWriteArrayList<>();
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    private static final int READ_TIMEOUT_MS = 50;
    private static final long PACKET_TIMEOUT_MS = 800;

    public boolean openPort(String portName, int baudRate) {
        if (port != null && port.isOpen()) closePort();

        port = SerialPort.getCommPort(portName);
        port.setBaudRate(baudRate);
        port.setNumDataBits(8);
        port.setNumStopBits(1);
        port.setParity(SerialPort.NO_PARITY);
        port.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, READ_TIMEOUT_MS, 0);

        if (port.openPort()) {
            running = true;
            startReader();
            log.info("Edwards port opened: " + portName + " @ " + baudRate);
            return true;
        }
        log.error("Failed to open Edwards port: " + portName);
        return false;
    }

    public void closePort() {
        running = false;
        if (readerThread != null) {
            try { readerThread.interrupt(); readerThread.join(1000); } catch (InterruptedException ignored) {}
        }
        if (port != null && port.isOpen()) port.closePort();
        port = null;
        synchronized (buffer) { buffer.reset(); }
    }

    private void startReader() {
        readerThread = new Thread(() -> {
            byte[] single = new byte[1];
            long startTime = 0;
            boolean collecting = false;

            while (running && port != null && port.isOpen()) {
                try {
                    int r = port.readBytes(single, 1);
                    long now = System.currentTimeMillis();

                    if (r > 0) {
                        if (!collecting) {
                            collecting = true;
                            startTime = now;
                            buffer.reset();
                        }
                        buffer.write(single[0]);
                        if (single[0] == '\r') {
                            processBuffer(startTime);
                            collecting = false;
                        }
                    } else if (collecting && (now - startTime > READ_TIMEOUT_MS * 3)) {
                        processBuffer(startTime);
                        collecting = false;
                    }
                } catch (Exception e) {
                    log.error("Read error", e);
                }
            }
        });
        readerThread.setDaemon(true);
        readerThread.start();
    }

    private void processBuffer(long startTime) {
        byte[] data;
        synchronized (buffer) {
            data = buffer.toByteArray();
            buffer.reset();
        }
        if (data.length == 0) return;
        if (data[data.length - 1] == '\r') data = Arrays.copyOf(data, data.length - 1);

        for (Consumer<byte[]> l : listeners) l.accept(data);
    }

    public void addResponseListener(Consumer<byte[]> listener) { listeners.add(listener); }
    public void removeResponseListener(Consumer<byte[]> listener) { listeners.remove(listener); }

    public void sendRequest(byte[] data) {
        if (port == null || !port.isOpen()) return;

        byte[] toSend = data;
        if (data.length == 0 || data[data.length - 1] != '\r') {
            toSend = new byte[data.length + 1];
            System.arraycopy(data, 0, toSend, 0, data.length);
            toSend[data.length] = '\r';
        }
        log.info("Edwards TX: " + MyUtilities.bytesToHexString(toSend));
        port.writeBytes(toSend, toSend.length);
    }

    public void sendCommand(String cmd) {
        sendRequest(cmd.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
    }

    public void shutdown() {
        closePort();
        listeners.clear();
    }
}