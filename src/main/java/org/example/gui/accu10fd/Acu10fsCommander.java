package org.example.gui.accu10fd;

import com.fazecast.jSerialComm.SerialPort;
import lombok.Getter;
import lombok.Setter;
import org.apache.log4j.Logger;
import org.example.utilites.MyUtilities;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public class Acu10fsCommander {
    private static byte DEVICE_ADDRESS = 0x01;
    private static final int RESPONSE_TIMEOUT_MS = 500;
    private static final int READ_RETRIES = 10;
    private static final int[] TEST_BAUD_RATES = {
            4800, 9600, 19200, 38400, 57600, 115200, 9600
    };
    private static final int TEST_REGISTER = 0x0078; // DevAddr

    private int detectedBaudRate = -1;
    @Getter @Setter
    private SerialPort comPort;
    private final Logger log = Logger.getLogger(Acu10fsCommander.class);
    @Getter
    private AtomicBoolean busyStatus = new AtomicBoolean(false);
    public Acu10fsCommander(SerialPort port) {
        this.comPort = port;
    }

    public boolean isBusy(){
        return this.busyStatus.get();
    }

    public void forceRelease(){
        this.busyStatus.set(false);
    }

    public boolean isPortConsistent() {
        return comPort != null && comPort.isOpen();
    }

    /**
     * Автоматическое определение скорости порта
     */
    public int autoDetectBaudRate() {
        if (!comPort.isOpen()) {
            log.warn("Port is closed! Cannot detect baud rate");
            return -1;
        }
        int originalBaud = comPort.getBaudRate();
        int originalParity = comPort.getParity();

        try {
            for (int baud : TEST_BAUD_RATES) {
                log.debug("Testing baud rate: " + baud);
                comPort.setBaudRate(baud);
                comPort.flushIOBuffers();
                for(byte adr = 0; adr < 3; adr++) {
                    DEVICE_ADDRESS = adr;
                    for (int i = 0; i < READ_RETRIES; i++) {
                        try {
                            float value = readRegisterValue(TEST_REGISTER);
                            log.info("Device found at " + baud + " baud, test value=" + value + " address " + DEVICE_ADDRESS + " retries " + i);
                            detectedBaudRate = baud;
                            return baud;
                        } catch (Exception ignored) {
                        }
                    }

                }
            }
        } finally {
            comPort.setBaudRate(originalBaud);
            comPort.setParity(originalParity);
        }
        return -1;
    }

    public int getDetectedBaudRate() {
        return detectedBaudRate;
    }

    public boolean applyOptimalBaudRate() {
        if (detectedBaudRate > 0) {
            comPort.setBaudRate(detectedBaudRate);
            return true;
        }
        return false;
    }

    // ===== Универсальные методы чтения/записи =====

    public float readRegisterValue(int registerAddress) throws Exception {
        int regHigh = (registerAddress >> 8) & 0xFF;
        int regLow = registerAddress & 0xFF;
        byte[] request = createReadCommand(regHigh, regLow, 0, 2);
        byte[] response = sendModbusRequest(request, true);
        return parseFloatCDAB(response, 3);
    }

    private void writeRegisterValue(int registerAddress, float value) throws Exception {
        byte[] request = createWriteRequest(registerAddress, value);
        sendModbusRequest(request, false);
    }

    // ===== Специфичные команды =====

    public float readInstantaneousFlow() throws Exception {
        return readRegisterValue(0x0010);
    }

    public float readCumulativeFlow() throws Exception {
        return readRegisterValue(0x001C);
    }

    public void resetCumulativeFlow() throws Exception {
        writeRegisterValue(0x001C, 0.0f);
    }

    public void setAnalogControlMode() throws Exception {
        writeRegisterValue(0x0074, 25.0f);
    }

    public void setZeroPoint() throws Exception {
        writeRegisterValue(0x0076, 0.0F);
    }

    public void cancelZeroPoint() throws Exception {
        writeRegisterValue(0x0076, 1.0F);
    }

    public void setDigitalControlMode() throws Exception {
        writeRegisterValue(0x0074, 26.0f);
    }

    public void setFlowRate(float value) throws Exception {
        writeRegisterValue(0x006A, value);
    }

    public void setGasCoefficient(float coefficient) throws Exception {
        writeRegisterValue(0x0072, coefficient);
    }

    // ===== Формирование команд =====

    private byte[] createReadCommand(int registerH, int registerL, int forReadL, int forReadH) {
        ByteBuffer buf = ByteBuffer.allocate(6)
                .put(DEVICE_ADDRESS)
                .put((byte) 0x03)
                .put((byte) registerH)
                .put((byte) registerL)
                .put((byte) forReadL)
                .put((byte) forReadH);
        return addCrc(buf.array());
    }

    private byte[] createWriteRequest(int register, float value) {
        byte[] floatBytes = ByteBuffer.allocate(4)
                .order(ByteOrder.BIG_ENDIAN)
                .putFloat(value)
                .array();
        byte[] swappedFloatBytes = new byte[4];
        swappedFloatBytes[0] = floatBytes[2];
        swappedFloatBytes[1] = floatBytes[3];
        swappedFloatBytes[2] = floatBytes [0];
        swappedFloatBytes [3] = floatBytes [1];
        log.info("Преобразованное значение " + value + " :" + bytesToHex(swappedFloatBytes));
        ByteBuffer buf = ByteBuffer.allocate(11)
                .order(ByteOrder.BIG_ENDIAN)
                .put(DEVICE_ADDRESS)
                .put((byte) 0x10)
                .putShort((short) register)
                .putShort((short) 2)
                .put((byte) 4)
                .put(swappedFloatBytes);
        return addCrc(buf.array());
    }

    // ===== Отправка и чтение ответа =====

    private byte[] sendModbusRequest(byte[] request, boolean waitForAnswer) throws Exception {
        if (!isPortConsistent()){
            log.info("COM port not open");
            throw new Exception("COM port not open");
        }
        if(busyStatus.get()){
            log.warn("COM connection busy");
            return null;
        }
        this.busyStatus.set(true);
        comPort.flushDataListener();
        comPort.flushIOBuffers();
        Thread.sleep(50L);
        comPort.writeBytes(request, request.length);
        log.info("Sent: " + bytesToHex(request) + " wait for answer? " + waitForAnswer);
        if(waitForAnswer) {
            byte[] response = null;
            response = readResponse(request[0], request[1]);
            if (response == null){
                this.busyStatus.set(false);
                log.warn("Нет ответа от прибора");
                throw new Exception("Нет ответа от прибора");
            }
            if (!checkCrc(response)){
                this.busyStatus.set(false);
                log.warn("Ошибка проверки CRC");
                throw new Exception("Ошибка проверки CRC");
            }
            this.busyStatus.set(false);
            return response;
        }
        this.busyStatus.set(false);
        return null;
    }

    private byte[] readResponse(byte expectedAddress, byte expectedFunction) throws InterruptedException, IOException {
        long startTime = System.currentTimeMillis();
        ByteArrayOutputStream accumulatedBuffer = new ByteArrayOutputStream();
        int minPacketSize = 5; // Минимальный размер пакета для проверки

        while (System.currentTimeMillis() - startTime <= RESPONSE_TIMEOUT_MS) {
            int available = comPort.bytesAvailable();
            if (available == 0) {
                Thread.sleep(20); // Ждем, если данных нет
                continue;
            }

            // Читаем доступные байты
            byte[] tempBuffer = new byte[available];
            int read = comPort.readBytes(tempBuffer, available);
            if (read > 0) {
                // Добавляем считанные байты в накопительный буфер
                accumulatedBuffer.write(Arrays.copyOf(tempBuffer, read));
                log.debug("Received chunk: " + bytesToHex(Arrays.copyOf(tempBuffer, read)) +
                        ", Accumulated: " + bytesToHex(accumulatedBuffer.toByteArray()));
            } else {
                log.warn("Read returned 0 bytes despite available=" + available);
                Thread.sleep(20);
                continue;
            }

            // Проверяем накопленный буфер
            byte[] currentData = accumulatedBuffer.toByteArray();
            if (currentData.length >= minPacketSize &&
                    currentData[0] == expectedAddress &&
                    currentData[1] == expectedFunction) {
                log.debug("Valid packet received: " + bytesToHex(currentData));
                return currentData;
            } else {
                log.info("Accumulated data invalid: length=" + currentData.length +
                        ", address=" + (currentData.length > 0 ? currentData[0] : "N/A") +
                        ", function=" + (currentData.length > 1 ? currentData[1] : "N/A"));
            }

            Thread.sleep(70);
        }

        log.error("Timeout reached or no valid data received. Accumulated: " +
                bytesToHex(accumulatedBuffer.toByteArray()));
        return null;
    }

    // ===== Парсинг =====

    private float parseFloatCDAB(byte[] response, int offset) {
        if (response.length < offset + 4) throw new IllegalArgumentException("Invalid response length");
        byte[] cdab = Arrays.copyOfRange(response, offset, offset + 4);
        byte[] abcd = {cdab[2], cdab[3], cdab[0], cdab[1]};
        return ByteBuffer.wrap(abcd).order(ByteOrder.BIG_ENDIAN).getFloat();
    }

    // ===== CRC =====

    private static byte[] addCrc(byte[] data) {
        int crc = calculateCrc(data);
        byte[] result = Arrays.copyOf(data, data.length + 2);
        result[result.length - 2] = (byte) (crc & 0xFF);
        result[result.length - 1] = (byte) ((crc >> 8) & 0xFF);
        return result;
    }

    private static boolean checkCrc(byte[] data) {
        if (data.length < 3) return false;
        byte[] withoutCrc = Arrays.copyOf(data, data.length - 2);
        int calculatedCrc = calculateCrc(withoutCrc);
        int receivedCrc = (data[data.length - 1] & 0xFF) << 8 | (data[data.length - 2] & 0xFF);
        return calculatedCrc == receivedCrc;
    }

    private static int calculateCrc(byte[] data) {
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
        return crc;
    }

    // ===== Утилиты =====

    private static String bytesToHex(byte[] bytes) {
        return MyUtilities.bytesToHex(bytes);
    }


}
