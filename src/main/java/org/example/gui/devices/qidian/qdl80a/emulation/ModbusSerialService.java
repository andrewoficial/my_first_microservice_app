package org.example.gui.devices.qidian.qdl80a.emulation;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import org.example.gui.devices.stu.mcps.AsyncLogger;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

public class ModbusSerialService {

    private final AsyncLogger logger;
    private SerialPort port;
    private volatile boolean running = false;

    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private final ModbusRequestHandler handler;
    private final SerialPortDataListener listener;

    // Для отлова дублирующихся запросов (jSerialComm иногда шлёт одно и то же дважды)
    private byte[] lastFrameBytes = null;
    private long lastFrameTime = 0;

    // Таймаут ожидания полного кадра после появления данных
    private static final long FRAME_WAIT_TIMEOUT_MS = 500;

    public ModbusSerialService(ModbusRequestHandler handler, AsyncLogger logger) {
        this.handler = handler;
        this.logger = logger;
        this.listener = new SerialPortDataListener() {
            @Override
            public int getListeningEvents() {
                return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
            }

            @Override
            public void serialEvent(SerialPortEvent event) {
                if (event.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE) {
                    return;
                }
                logger.debug("Событие: данные доступны на порту");
                readAndWaitForCompleteFrame();
            }
        };
        logger.info("ModbusSerialService создан");
    }

    /**
     * Читает доступные данные и ждёт, пока не наберётся полный кадр Modbus
     * либо не истечёт FRAME_WAIT_TIMEOUT_MS.
     */
    private void readAndWaitForCompleteFrame() {
        try {
            if (port == null || !port.isOpen()) {
                logger.warn("readAndWaitForCompleteFrame: порт не открыт");
                return;
            }

            // Сначала читаем всё, что уже есть
            readAvailableImmediately();

            long startTime = System.currentTimeMillis();
            boolean frameReady;

            do {
                byte[] currentData;
                synchronized (buffer) {
                    currentData = buffer.toByteArray();
                }

                int frameLen = getCompleteFrameLength(currentData);
                frameReady = (frameLen > 0 && currentData.length >= frameLen);

                if (!frameReady) {
                    // Если кадра нет — ждём ещё немного, периодически подчитывая новые байты
                    long elapsed = System.currentTimeMillis() - startTime;
                    long remaining = FRAME_WAIT_TIMEOUT_MS - elapsed;
                    if (remaining <= 0) {
                        logger.debug("readAndWaitForCompleteFrame: таймаут ожидания кадра истёк");
                        break;
                    }

                    // Небольшая пауза, чтобы не гонять цикл на 100% CPU, и снова читаем доступные данные
                    Thread.sleep(Math.min(remaining, 20));
                    readAvailableImmediately();
                }
            } while (!frameReady);

            processBuffer();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("readAndWaitForCompleteFrame: ожидание кадра прервано");
        } catch (Exception e) {
            logger.error("readAndWaitForCompleteFrame: ошибка при чтении данных");
        }
    }

    /**
     * Мгновенно читает все доступные на данный момент байты из порта.
     * Не ждёт: просто забирает то, что уже пришло.
     */
    private void readAvailableImmediately() {
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
        // Можно оставить один лаконичный лог, если нужно
        // logger.debug("Прочитано {} байт, текущий размер буфера: {}", read, buffer.size());
    }

    private void processBuffer() {
        byte[] data;
        synchronized (buffer) {
            data = buffer.toByteArray();
        }

        if (data.length < 3) {
            return;
        }

        int frameLen = getCompleteFrameLength(data);
        if (frameLen > 0 && data.length >= frameLen) {
            byte[] frame = Arrays.copyOf(data, frameLen);

            // Пропускаем дубликат: те же байты, пришедшие менее 200 мс назад
            long now = System.currentTimeMillis();
            if (lastFrameBytes != null && Arrays.equals(lastFrameBytes, frame) && (now - lastFrameTime) < 200) {
                synchronized (buffer) {
                    buffer.reset();
                    if (data.length > frameLen) {
                        buffer.write(data, frameLen, data.length - frameLen);
                    }
                }
                return;
            }
            lastFrameBytes = frame;
            lastFrameTime = now;

            synchronized (buffer) {
                buffer.reset();
                if (data.length > frameLen) {
                    buffer.write(data, frameLen, data.length - frameLen);
                }
            }

            logPacketDetails(frame);

            byte[] response = handler.handleRequest(frame);
            if (response != null && response.length > 0) {
                int written = port.writeBytes(response, response.length);
                if (written != response.length) {
                    logger.warn("processBuffer: отправлено {} из {} байт");
                }
            }
        }
    }

    private void logPacketDetails(byte[] packet) {


        boolean minLengthOk = packet.length >= 4;


        if (!minLengthOk) {
            logger.warn("Пакет слишком короткий для проверки CRC");
            return;
        }

        int len = packet.length;
        byte[] data = Arrays.copyOfRange(packet, 0, len - 2);
        short receivedCRC = (short) ((packet[len - 1] & 0xFF) << 8 | (packet[len - 2] & 0xFF));
        short calcCRC = calculateCRC(data);
        boolean crcOk = (receivedCRC == calcCRC);


    }

    private int getCompleteFrameLength(byte[] data) {
        if (data.length < 3) {
            return 0;
        }
        int function = data[1] & 0xFF;

        // Modbus-запросы (адрес + функция + данные + CRC(2))
        if (function == 0x03 || function == 0x04) {
            // Запрос чтения: 1 + 1 + 2 (startAddr) + 2 (quantity) + 2 (CRC) = 8
            if (data.length >= 8) return 8;
            return 0;
        } else if (function == 0x06) {
            // Запрос записи одного регистра: 1 + 1 + 2 (regAddr) + 2 (value) + 2 (CRC) = 8
            if (data.length >= 8) return 8;
            return 0;
        } else if (function == 0x10) {
            // Запрос записи нескольких регистров: 1 + 1 + 2 (startAddr) + 2 (quantity) + 1 (byteCount) + data + 2 (CRC)
            if (data.length < 7) return 0;
            int byteCount = data[6] & 0xFF;
            int totalLen = 7 + byteCount + 2;
            if (data.length >= totalLen) return totalLen;
            return 0;
        } else if ((function & 0x80) != 0) {
            // Error response
            if (data.length >= 5) return 5;
            return 0;
        }
        return 0;
    }

    private short calculateCRC(byte[] data) {
        int crc = 0xFFFF;
        for (byte b : data) {
            crc ^= (b & 0xFF);
            for (int i = 0; i < 8; i++) {
                if ((crc & 0x0001) != 0) {
                    crc = (crc >> 1) ^ 0xA001;
                } else {
                    crc >>= 1;
                }
            }
        }
        return (short) crc;
    }

    public synchronized boolean openPort(String portName) {
        logger.info("Попытка открыть порт: {}");

        if (port != null && port.isOpen()) {
            logger.warn("Порт уже открыт, закрываем перед повторным открытием");
            closePort();
        }

        port = SerialPort.getCommPort(portName);
        port.setBaudRate(9600);
        port.setNumDataBits(8);
        port.setNumStopBits(1);
        port.setParity(SerialPort.NO_PARITY);
        port.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
        // Полублокирующее чтение с таймаутом на уровне драйвера
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 100, 0);

        logger.info("Настройки порта: 9600,8,N,1, таймаут чтения {} мс"+ 100);

        if (port.openPort()) {
            logger.info("Порт успешно открыт");
            port.addDataListener(listener);
            running = true;
            synchronized (buffer) {
                buffer.reset();
            }
            logger.info("Слушатель добавлен, буфер очищен");
            return true;
        } else {
            logger.error("Не удалось открыть порт: ");
            return false;
        }
    }

    public synchronized void closePort() {
        logger.info("Закрытие порта");
        if (port != null && port.isOpen()) {
            port.removeDataListener();
            port.closePort();
            logger.info("Порт закрыт");
        } else {
            logger.warn("Попытка закрыть порт, который уже закрыт или не инициализирован");
        }
        running = false;
        port = null;
        synchronized (buffer) {
            buffer.reset();
            logger.info("Буфер очищен");
        }
    }

    public boolean isRunning() {
        return running;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }
}
