package org.example.gui.devices.stu.mcps.control;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import org.example.gui.devices.stu.mcps.AsyncLogger;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Автономный быстрый сервис взаимодействия с COM-портом по протоколу SPB_STU_MCPS (ASCII, 8N1).
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

    private static final int DEFAULT_BAUD_RATE = 9600;
    private static final int DATA_BITS = 8;
    private static final int STOP_BITS = SerialPort.ONE_STOP_BIT;
    private static final int PARITY = SerialPort.NO_PARITY;

    private static final int SEND_QUEUE_CAPACITY = 512;

    private final AsyncLogger logger;
    private SerialPort serialPort;
    private volatile boolean connected = false;

    private final LinkedBlockingQueue<String> sendQueue = new LinkedBlockingQueue<>(SEND_QUEUE_CAPACITY);
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

    private volatile int minCommandIntervalMs = 0;
    private volatile boolean throttled = false;
    private volatile long lastThrottleTimeMs = 0;
    private Runnable throttleListener;

    public McpsCommunicationService(AsyncLogger logger) {
        this.logger = logger;
    }

    /**
     * Минимальный интервал между отправкой кадров на шину (мс).
     * 0 — троттлинг выключен.
     */
    public void setMinCommandIntervalMs(int ms) {
        this.minCommandIntervalMs = Math.max(0, ms);
    }

    /**
     * true, если транспорту пришлось притормозить отправку хотя бы раз
     * (был превышен темп кадров и сработал rate limit).
     */
    public boolean wasThrottled() {
        return throttled;
    }

    public long getLastThrottleTimeMs() {
        return lastThrottleTimeMs;
    }

    /**
     * Сбрасывает флаг срабатывания троттлинга (например, после того как
     * пользователь увидел и подтвердил индикацию).
     */
    public void resetThrottleFlag() {
        throttled = false;
    }

    /**
     * Колбек, вызываемый (в потоке sender'а) при каждом срабатывании
     * троттлинга на транспортном уровне.
     */
    public void setThrottleListener(Runnable listener) {
        this.throttleListener = listener;
    }

    public boolean openPort(String portDescriptor) {
        return openPort(portDescriptor, DEFAULT_BAUD_RATE);
    }

    public boolean openPort(String portDescriptor, int baudRate) {
        if (connected) {
            closePort();
        }

        int effectiveBaud = baudRate > 0 ? baudRate : DEFAULT_BAUD_RATE;

        serialPort = SerialPort.getCommPort(portDescriptor);
        serialPort.setBaudRate(effectiveBaud);
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
        logger.info("Порт открыт: " + portDescriptor + " @ " + effectiveBaud + " 8N1");
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
            long lastSendAt = 0;
            while (sendRunning) {
                try {
                    String cmd = sendQueue.poll(50, TimeUnit.MILLISECONDS);
                    if (cmd != null && serialPort != null && serialPort.isOpen()) {
                        int interval = minCommandIntervalMs;
                        if (interval > 0 && lastSendAt != 0) {
                            long elapsed = System.currentTimeMillis() - lastSendAt;
                            long wait = interval - elapsed;
                            if (wait > 0) {
                                // Транспортный rate limit: следующий кадр пришёл
                                // раньше минимального межкадрового интервала —
                                // притормаживаем шину и сигнализируем наверх.
                                throttled = true;
                                lastThrottleTimeMs = System.currentTimeMillis();
                                Runnable l = throttleListener;
                                if (l != null) {
                                    try { l.run(); } catch (Exception ignored) {}
                                }
                                Thread.sleep(wait);
                            }
                        }
                        byte[] bytes = (cmd + "\r\n").getBytes(StandardCharsets.US_ASCII);
                        serialPort.writeBytes(bytes, bytes.length);
                        lastSendAt = System.currentTimeMillis();
                        logger.debug("TX: " + cmd);
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
     *
     * Для команд чтения (начинаются с @R, кроме записи @WR) выполняется
     * дедупликация: если такая же команда чтения уже стоит в очереди, повторная
     * не добавляется. Это не нарушает очерёдность (существующая команда остаётся
     * на своём месте) и защищает очередь от разрастания периодическими опросами.
     * Команды записи (@WR) никогда не дедуплицируются.
     */
    public void sendCommand(String command) {
        if (!connected || command == null || command.isBlank()) return;
        if (isReadCommand(command) && sendQueue.contains(command)) {
            return;
        }
        if (!sendQueue.offer(command)) {
            logger.warn("Очередь отправки переполнена, команда отброшена: " + command);
        }
    }

    private static boolean isReadCommand(String command) {
        return command.startsWith("@R");
    }

    /**
     * Текущее количество команд, ожидающих отправки.
     */
    public int getSendQueueSize() {
        return sendQueue.size();
    }

    /**
     * Максимальная вместимость очереди отправки.
     */
    public int getSendQueueCapacity() {
        return SEND_QUEUE_CAPACITY;
    }

    /**
     * Отправка команды с ожиданием подтверждения (OK).
     * Блокирует вызывающий поток на время ожидания.
     * @return true если получен OK в пределах timeoutMs, иначе false
     */
    public boolean sendCommandSync(String command, long timeoutMs) {
        if (!connected || command == null || command.isBlank()) return false;
        CountDownLatch latch = new CountDownLatch(1);
        Consumer<String> listener = response -> {
            if (response.contains("OK")) latch.countDown();
        };
        addResponseListener(listener);
        if (!sendQueue.offer(command)) {
            logger.warn("Очередь отправки переполнена, команда отброшена: " + command);
            removeResponseListener(listener);
            return false;
        }
        try {
            return latch.await(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            removeResponseListener(listener);
        }
    }

    /**
     * Удобные методы формирования команд по протоколу
     */
    public void writeOutput(int channel, boolean on, int durationMs) {
        // @WR<CH> <state>[,<duration>]
        // duration > 0  -> @WR<CH> 1,<duration> (импульс на duration мс)
        // duration == 0 -> @WR<CH> 1            (постоянное включение)
        String ch = String.format("%02d", channel);
        String cmd;
        if (on) {
            cmd = durationMs > 0
                ? String.format("@WR%s 1,%d", ch, durationMs)
                : String.format("@WR%s 1,0", ch);
        } else {
            cmd = String.format("@WR%s 0", ch);
        }
        sendCommand(cmd);
    }

    /**
     * Синхронная запись выхода: отправляет команду и ждёт подтверждения (OK)
     * именно на эту команду, логируя полученный ответ прибора.
     * Используется в режиме последовательности импульсов, чтобы не начинать
     * следующую команду, пока не пришёл ответ на предыдущую.
     *
     * @return true если ответ получен в пределах timeoutMs, иначе false
     */
    public boolean writeOutputSync(int channel, boolean on, int durationMs, long timeoutMs) {
        String ch = String.format("%02d", channel);
        String cmd;
        if (on) {
            cmd = durationMs > 0
                ? String.format("@WR%s 1,%d", ch, durationMs)
                : String.format("@WR%s 1,0", ch);
        } else {
            cmd = String.format("@WR%s 0", ch);
        }
        return sendCommandSyncLogged(cmd, timeoutMs);
    }

    /**
     * Отправка команды с ожиданием ответа именно на неё и логированием ответа.
     * Ответом считается строка, начинающаяся с того же адреса/команды и
     * содержащая OK (например, запрос "@WR01 0" -> ответ "@WR01 OK").
     */
    public boolean sendCommandSyncLogged(String command, long timeoutMs) {
        if (!connected || command == null || command.isBlank()) return false;

        String prefix = command.contains(" ") ? command.substring(0, command.indexOf(' ')) : command;
        CountDownLatch latch = new CountDownLatch(1);
        final String[] received = new String[1];
        Consumer<String> listener = response -> {
            if (response.startsWith(prefix) && response.contains("OK")) {
                received[0] = response;
                latch.countDown();
            }
        };
        addResponseListener(listener);
        logger.info("TX (ожидание ответа): " + command);
        if (!sendQueue.offer(command)) {
            logger.warn("Очередь отправки переполнена, команда отброшена: " + command);
            removeResponseListener(listener);
            return false;
        }
        try {
            boolean ok = latch.await(timeoutMs, TimeUnit.MILLISECONDS);
            if (ok) {
                logger.info("RX ответ на " + command + ": " + received[0]);
            } else {
                logger.warn("Таймаут ожидания ответа на команду: " + command);
            }
            return ok;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            removeResponseListener(listener);
        }
    }

    /**
     * Удаляет из очереди отправки все ожидающие команды чтения (@R...),
     * чтобы во время последовательности импульсов шина не засорялась
     * фоновым опросом состояния.
     */
    public void flushPendingReads() {
        sendQueue.removeIf(McpsCommunicationService::isReadCommand);
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
