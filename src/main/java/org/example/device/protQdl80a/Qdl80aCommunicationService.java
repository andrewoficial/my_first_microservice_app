package org.example.device.protQdl80a;

import com.fazecast.jSerialComm.SerialPort;
import lombok.extern.slf4j.Slf4j;
import org.example.utilites.MyUtilities;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@Slf4j
public class Qdl80aCommunicationService {
    private SerialPort port;
    private volatile boolean running = false;
    private Thread readerThread;
    private final List<Consumer<byte[]>> listeners = new CopyOnWriteArrayList<>();
    private final Object lock = new Object();

    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    private static final int READ_TIMEOUT_MS = 100;      // таймаут на чтение одного байта
    private static final long PACKET_TIMEOUT_MS = 1000;  // максимальное время накопления пакета

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
            log.info("Port " + portName + " opened at " + baudRate);
            return true;
        } else {
            log.error("Failed to open port " + portName);
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
            log.info("Port closed");
        }
        port = null;
        synchronized (buffer) {
            buffer.reset();
        }
    }

    private void startReader() {
        readerThread = new Thread(() -> {
            byte[] singleByte = new byte[1];
            long packetStartTime = 0;
            boolean collecting = false;

            while (running && port != null && port.isOpen()) {
                try {
                    int read = port.readBytes(singleByte, 1);
                    long now = System.currentTimeMillis();

                    if (read > 0) {
                        if (!collecting) {
                            collecting = true;
                            packetStartTime = now;
                            buffer.reset();
                        }
                        buffer.write(singleByte[0]);
                        // Если прошло больше PACKET_TIMEOUT_MS с момента первого байта, считаем пакет завершённым
                        if (now - packetStartTime >= PACKET_TIMEOUT_MS) {
                            processBuffer(packetStartTime);
                            collecting = false;
                        }
                    } else {
                        // Если таймаут и мы собирали пакет, и с последнего байта прошло больше 50 мс
                        if (collecting && (now - packetStartTime > READ_TIMEOUT_MS * 2)) {
                            processBuffer(packetStartTime);
                            collecting = false;
                        }
                    }
                } catch (Exception e) {
                    log.error("Error reading from port", e);
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

        long elapsed = System.currentTimeMillis() - startTime;

        int frameLen = getCompleteFrameLength(data);
        if (frameLen > 0 && data.length >= frameLen) {
            byte[] frame = Arrays.copyOf(data, frameLen);
            logPacketDetails(frame);
            for (Consumer<byte[]> listener : listeners) {
                listener.accept(frame);
            }
        } else {
            log.warn("Неполный пакет (" + data.length + "): " + bytesToHex(data));
        }
    }

    private void logPacketDetails(byte[] packet) {
        if (packet.length < 4) {
            log.warn("Короткий пакет (" + packet.length + "): " + bytesToHex(packet));
            return;
        }
        int len = packet.length;
        byte[] data = Arrays.copyOfRange(packet, 0, len - 2);
        short receivedCRC = (short)((packet[len-1] & 0xFF) << 8 | (packet[len-2] & 0xFF));
        short calcCRC = Qdl80aCommandRegistry.calculateCRC(data);
        if (calcCRC == receivedCRC) {
            log.info(bytesToHex(packet) + " CRC OK");
        } else {
            log.warn(bytesToHex(packet) + " CRC err: получено 0x" + String.format("%04X", receivedCRC) + ", ожидалось 0x" + String.format("%04X", calcCRC));
        }
    }

    private int getCompleteFrameLength(byte[] data) {
        if (data.length < 3) return 0;
        int function = data[1] & 0xFF;
        if (function == 0x03 || function == 0x04) {
            if (data.length < 3) return 0;
            int byteCount = data[2] & 0xFF;
            int totalLen = 3 + byteCount + 2;
            if (data.length >= totalLen) return totalLen;
            else return 0;
        } else if (function == 0x06) {
            if (data.length >= 8) return 8;
            else return 0;
        } else if ((function & 0x80) != 0) {
            if (data.length >= 5) return 5;
            else return 0;
        }
        return 0;
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
        log.info("Отправка: " + MyUtilities.bytesToHexString(data));
        if (port != null && port.isOpen()) {
            port.writeBytes(data, data.length);
        } else {
            log.warn("Port is not open, cannot send request");
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02X ", b));
        return sb.toString().trim();
    }
}