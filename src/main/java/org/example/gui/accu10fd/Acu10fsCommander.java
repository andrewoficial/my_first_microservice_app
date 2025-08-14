package org.example.gui.accu10fd;

import com.fazecast.jSerialComm.SerialPort;
import org.apache.log4j.Logger;
import org.example.utilites.MyUtilities;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class Acu10fsCommander {
    private static final byte DEVICE_ADDRESS = 0x01;
    private static final int RESPONSE_TIMEOUT_MS = 200;
    private static final int READ_RETRIES = 3;
    private static final int[] TEST_BAUD_RATES = {
            55, 75, 110, 150, 300, 600, 1200, 2400, 4800,
            9600, 19200, 38400, 57600, 115200, 230400, 460800, 921600
    };
    private static final int TEST_REGISTER = 0x0010; // Instantaneous Flow

    private int detectedBaudRate = -1;
    private final SerialPort comPort;
    private final Logger log = Logger.getLogger(Acu10fsCommander.class);

    public Acu10fsCommander(SerialPort port) {
        this.comPort = port;
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

                try {
                    float value = readRegisterValue(TEST_REGISTER);
                    log.info("Device found at " + baud + " baud, test value=" + value);
                    detectedBaudRate = baud;
                    return baud;
                } catch (Exception ignored) {
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
        if (!isPortConsistent()) throw new Exception("COM port not initialized");

        log.info("Com connection speed: " + comPort.getBaudRate());
        comPort.writeBytes(request, request.length);
        log.info("Sent: " + bytesToHex(request));
        if(waitForAnswer) {
            byte[] response = null;
            for (int i = 0; i < READ_RETRIES; i++) {
                response = readResponse(request[0], request[1]);
                if (response != null) break;
                Thread.sleep(50);
            }

            if (response == null) throw new Exception("No response from device");
            if (!checkCrc(response)) throw new Exception("CRC check failed");

            return response;
        }
        return null;
    }

    private byte[] readResponse(byte expectedAddress, byte expectedFunction) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        while (comPort.bytesAvailable() == 0) {
            if (System.currentTimeMillis() - startTime > RESPONSE_TIMEOUT_MS) {
                return null;
            }
            Thread.sleep(10);
        }
        int available = comPort.bytesAvailable();
        byte[] buffer = new byte[available];
        int read = comPort.readBytes(buffer, available);
        log.debug("Received: " + bytesToHex(Arrays.copyOf(buffer, read)));

        if (read < 5 || buffer[0] != expectedAddress || buffer[1] != expectedFunction) {
            return null;
        }
        return Arrays.copyOf(buffer, read);
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
