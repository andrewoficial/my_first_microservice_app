package org.example.gui.sbpStuMcps;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Автономный быстрый сервис взаимодействия с COM-портом по протоколу SPB_STU_MCPS (ASCII, 9600 8N1).
 * 
 * Ключевые особенности (по требованиям):
 * - Собственный слушатель порта (не shared с основным приложением)
 * - Минимальные блокировки, нет синхронного логирования в hot-path
 * - Поддержка интервала отправки ~10мс (для периодических импульсов)
 * - Очередь отправки + dedicated sender thread
 * - Парсинг ответов по строкам (CR+LF)
 * - Уведомление слушателей о входящих ответах
 */
public final class McpsCommunicationService {

    private static final int BAUD_RATE = 9600;
    private static final int DATA_BITS = 8;
    private static final int STOP_BITS = SerialPort.ONE_STOP_BIT;
    private static final int PARITY = SerialPort.NO_PARITY;

    private final AsyncLogger logger;
    private SerialPort serialPort;
    private volatile boolean connected = false;

    private final LinkedBlockingQueue<String> sendQueue = new LinkedBlockingQueue<>(512);
    private Thread sendThread;
    private volatile boolean sendRunning = false;

    private final ExecutorService receiveParser = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "McpsReceiveParser");
        t.setDaemon(true);
        return t;
    });

    private final List<Consumer<String>> responseListeners = new CopyOnWriteArrayList<>();
    private final StringBuilder receiveBuffer = new StringBuilder(256);

    private Consumer<String> connectionStatusListener;

    public McpsCommunicationService(AsyncLogger logger) {
        this.logger = logger;
    }

    public boolean openPort(String portDescriptor) {
        if (connected) {
            closePort();
        }

        serialPort = SerialPort.getCommPort(portDescriptor);
        serialPort.setBaudRate(BAUD_RATE);
        serialPort.setNumDataBits(DATA_BITS);
        serialPort.setNumStopBits(STOP_BITS);
        serialPort.setParity(PARITY);
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 100, 0);

        if (!serialPort.openPort()) {
            logger.error("Не удалось открыть порт: " + portDescriptor);
            return false;
        }

        // Настройка listener'а (собственный, не shared)
        serialPort.addDataListener(new SerialPortDataListener() {
            @Override
            public int getListeningEvents() {
                return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
            }

            @Override
            public void serialEvent(SerialPortEvent event) {
                if (event.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE) return;

                byte[] data = new byte[serialPort.bytesAvailable()];
                int numRead = serialPort.readBytes(data, data.length);
                if (numRead > 0) {
                    String chunk = new String(data, 0, numRead, StandardCharsets.US_ASCII);
                    receiveBuffer.append(chunk);
                    processBuffer();
                }
            }
        });

        startSenderThread();

        connected = true;
        logger.info("Порт открыт: " + portDescriptor + " @ " + BAUD_RATE + " 8N1");
        if (connectionStatusListener != null) {
            connectionStatusListener.accept("CONNECTED:" + portDescriptor);
        }
        return true;
    }

    private void processBuffer() {
        int idx;
        while ((idx = receiveBuffer.indexOf("\r\n")) >= 0) {
            String line = receiveBuffer.substring(0, idx).trim();
            receiveBuffer.delete(0, idx + 2);

            if (!line.isEmpty()) {
                final String response = line;
                receiveParser.submit(() -> {
                    notifyResponseListeners(response);
                    logger.debug("RX: " + response);
                });
            }
        }
        // Защита от переполнения буфера
        if (receiveBuffer.length() > 512) {
            receiveBuffer.delete(0, receiveBuffer.length() - 256);
        }
    }

    private void notifyResponseListeners(String response) {
        for (Consumer<String> listener : responseListeners) {
            try {
                listener.accept(response);
            } catch (Exception e) {
                logger.warn("Ошибка в listener ответа: " + e.getMessage());
            }
        }
    }

    private void startSenderThread() {
        if (sendThread != null && sendThread.isAlive()) return;

        sendRunning = true;
        sendThread = new Thread(() -> {
            while (sendRunning) {
                try {
                    String cmd = sendQueue.poll(50, TimeUnit.MILLISECONDS);
                    if (cmd != null && serialPort != null && serialPort.isOpen()) {
                        byte[] bytes = (cmd + "\r\n").getBytes(StandardCharsets.US_ASCII);
                        serialPort.writeBytes(bytes, bytes.length);
                        logger.debug("TX: " + cmd);
                        // Небольшая пауза для RS-485 (можно убрать если устройство быстрое)
                        // Thread.sleep(2);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.error("Ошибка отправки: " + e.getMessage());
                }
            }
        }, "McpsSender");
        sendThread.setDaemon(true);
        sendThread.start();
    }

    /**
     * Отправка команды (неблокирующая, через очередь).
     * Команда должна быть без CR LF — они добавятся автоматически.
     */
    public void sendCommand(String command) {
        if (!connected || command == null || command.isBlank()) return;
        if (!sendQueue.offer(command)) {
            logger.warn("Очередь отправки переполнена, команда отброшена: " + command);
        }
    }

    /**
     * Удобные методы формирования команд по протоколу
     */
    public void writeOutput(int channel, boolean on, int durationMs) {
        // @WR<CH> <b>,<time>   CH = 01..15 , time=0 для постоянного уровня
        String ch = String.format("%02d", channel);
        String cmd;
        if (on) {
            cmd = String.format("@WR%s 1,%d", ch, durationMs);
        } else {
            cmd = String.format("@WR%s 0", ch);
        }
        sendCommand(cmd);
    }

    public void readOutput(int channel) {
        String ch = String.format("%02d", channel);
        sendCommand("@RO" + ch);
    }

    public void readAllOutputs() {
        sendCommand("@RPOU");
    }

    public void readMode() {
        sendCommand("@RDMD");
    }

    public void readAllInputs() {
        sendCommand("@RPIN");
    }

    public void addResponseListener(Consumer<String> listener) {
        responseListeners.add(listener);
    }

    public void removeResponseListener(Consumer<String> listener) {
        responseListeners.remove(listener);
    }

    public void setConnectionStatusListener(Consumer<String> listener) {
        this.connectionStatusListener = listener;
    }

    public boolean isConnected() {
        return connected && serialPort != null && serialPort.isOpen();
    }

    public void closePort() {
        sendRunning = false;
        if (sendThread != null) {
            try { sendThread.join(300); } catch (InterruptedException ignored) {}
        }
        if (serialPort != null) {
            serialPort.closePort();
        }
        connected = false;
        receiveBuffer.setLength(0);
        sendQueue.clear();
        logger.info("Порт закрыт");
        if (connectionStatusListener != null) {
            connectionStatusListener.accept("DISCONNECTED");
        }
    }

    public void shutdown() {
        closePort();
        receiveParser.shutdownNow();
        logger.shutdown();
    }
}
