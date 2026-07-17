package org.example.gui.devices.arduino.feeboard.emulation;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

/**
 * Serial bridge for FeeBoard emulator (listens on a COM port, answers with CR).
 */
@Slf4j
public class FeeBoardSerialService {

    private final Function<String, String> commandHandler;
    private SerialPort port;
    private volatile boolean running = false;
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    private final SerialPortDataListener listener = new SerialPortDataListener() {
        @Override
        public int getListeningEvents() {
            return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
        }

        @Override
        public void serialEvent(SerialPortEvent event) {
            if (event.getEventType() == SerialPort.LISTENING_EVENT_DATA_AVAILABLE) {
                readAvailableData();
            }
        }
    };

    public FeeBoardSerialService(Function<String, String> commandHandler) {
        this.commandHandler = commandHandler;
    }

    private void readAvailableData() {
        if (port == null || !port.isOpen()) {
            return;
        }
        int available = port.bytesAvailable();
        if (available <= 0) {
            return;
        }
        byte[] chunk = new byte[available];
        int read = port.readBytes(chunk, available);
        if (read <= 0) {
            return;
        }
        synchronized (buffer) {
            buffer.write(chunk, 0, read);
        }
        processBuffer();
    }

    private void processBuffer() {
        byte[] data;
        synchronized (buffer) {
            data = buffer.toByteArray();
        }
        int crIndex = -1;
        for (int i = 0; i < data.length; i++) {
            if (data[i] == '\r' || data[i] == '\n') {
                crIndex = i;
                break;
            }
        }
        if (crIndex < 0) {
            return;
        }
        String cmd = new String(data, 0, crIndex, StandardCharsets.US_ASCII).trim();
        synchronized (buffer) {
            buffer.reset();
            if (data.length > crIndex + 1) {
                buffer.write(data, crIndex + 1, data.length - crIndex - 1);
            }
        }
        if (!cmd.isEmpty()) {
            log.info("FeeBoard emu RX: {}", cmd);
            String response = commandHandler.apply(cmd);
            if (response != null && !response.isEmpty()) {
                sendResponse(response);
            }
        }
    }

    private void sendResponse(String response) {
        if (port == null || !port.isOpen()) {
            return;
        }
        if (!response.endsWith("\r")) {
            response = response + "\r";
        }
        byte[] bytes = response.getBytes(StandardCharsets.US_ASCII);
        port.writeBytes(bytes, bytes.length);
        String preview = response.trim();
        log.info("FeeBoard emu TX: {}", preview.length() > 80 ? preview.substring(0, 80) + "…" : preview);
    }

    public synchronized boolean openPort(String portName, int baudRate) {
        if (port != null && port.isOpen()) {
            closePort();
        }
        int baud = baudRate > 0 ? baudRate : 57600;
        port = SerialPort.getCommPort(portName);
        port.setBaudRate(baud);
        port.setNumDataBits(8);
        port.setNumStopBits(1);
        port.setParity(SerialPort.NO_PARITY);
        port.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 100, 0);

        if (port.openPort()) {
            // re-apply after open (Windows / jSerialComm)
            port.setBaudRate(baud);
            port.setNumDataBits(8);
            port.setNumStopBits(1);
            port.setParity(SerialPort.NO_PARITY);
            port.addDataListener(listener);
            running = true;
            synchronized (buffer) {
                buffer.reset();
            }
            log.info("FeeBoard emu port open: {} @ {}", portName, port.getBaudRate());
            return true;
        }
        return false;
    }

    public synchronized void closePort() {
        if (port != null && port.isOpen()) {
            port.removeDataListener();
            port.closePort();
        }
        running = false;
        port = null;
        synchronized (buffer) {
            buffer.reset();
        }
    }

    public boolean isRunning() {
        return running;
    }
}
