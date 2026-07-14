package org.example.device.protSimpleHex;

import com.fazecast.jSerialComm.SerialPort;
import lombok.extern.slf4j.Slf4j;
import org.example.utilites.MyUtilities;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@Slf4j
public class SimpleHexCommunicationService {
    private SerialPort port;
    private volatile boolean running = false;
    private Thread readerThread;
    private final List<Consumer<byte[]>> listeners = new CopyOnWriteArrayList<>();

    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    private static final int READ_TIMEOUT_MS = 100;
    private static final long PACKET_TIMEOUT_MS = 2000;

    public boolean openPort(String portName, int baudRate) {
        if (port != null && port.isOpen()) {
            closePort();
        }
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
            log.info("SimpleHex: Port " + portName + " opened at " + baudRate);
            return true;
        } else {
            log.error("SimpleHex: Failed to open port " + portName);
            return false;
        }
    }

    public void closePort() {
        running = false;
        if (readerThread != null) {
            try {
                readerThread.interrupt();
                readerThread.join(1000);
            } catch (InterruptedException ignored) {}
        }
        if (port != null && port.isOpen()) {
            port.closePort();
            log.info("SimpleHex: Port closed");
        }
        port = null;
        synchronized (buffer) {
            buffer.reset();
        }
    }

    private void startReader() {
        readerThread = new Thread(() -> {
            byte[] singleByte = new byte[1];
            long lastByteTime = 0;
            boolean collecting = false;

            while (running && port != null && port.isOpen()) {
                try {
                    int read = port.readBytes(singleByte, 1);
                    long now = System.currentTimeMillis();

                    if (read > 0) {
                        if (!collecting) {
                            collecting = true;
                            buffer.reset();
                        }
                        buffer.write(singleByte[0]);
                        lastByteTime = now;
                    } else {
                        if (collecting && (now - lastByteTime > PACKET_TIMEOUT_MS)) {
                            deliverBuffer();
                            collecting = false;
                        }
                    }
                } catch (Exception e) {
                    log.error("SimpleHex: Error reading from port", e);
                }
            }
        });
        readerThread.setDaemon(true);
        readerThread.start();
    }

    private void deliverBuffer() {
        byte[] data;
        synchronized (buffer) {
            data = buffer.toByteArray();
            buffer.reset();
        }
        if (data.length == 0) return;
        log.info("SimpleHex received: " + MyUtilities.bytesToHexString(data));
        for (Consumer<byte[]> listener : listeners) {
            listener.accept(data);
        }
    }

    public void addResponseListener(Consumer<byte[]> listener) {
        listeners.add(listener);
    }

    public void removeResponseListener(Consumer<byte[]> listener) {
        listeners.remove(listener);
    }

    public boolean isRunning() {
        return running;
    }

    public void shutdown() {
        closePort();
        listeners.clear();
    }

    public void sendRequest(byte[] data) {
        log.info("SimpleHex sending: " + MyUtilities.bytesToHexString(data));
        if (port != null && port.isOpen()) {
            port.writeBytes(data, data.length);
        } else {
            log.warn("SimpleHex: Port is not open, cannot send request");
        }
    }
}
