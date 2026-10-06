package org.example.gui.accu10fd;

import com.fazecast.jSerialComm.SerialPort;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.example.device.protAcu10fd.Acu10fdFrames;
import org.example.utilites.MyUtilities;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class Acu10fsCommander {
    private static byte DEVICE_ADDRESS = 0x01;
    private static final int RESPONSE_TIMEOUT_MS = 500;
    private static final int READ_RETRIES = 10;
    private static final int[] TEST_BAUD_RATES = { 4800, 9600, 19200, 38400, 57600, 115200, 9600 };
    private static final int TEST_REGISTER = 0x0078; // DevAddress
    private int detectedBaudRate = -1;
    @Getter @Setter
    private SerialPort comPort;
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
        byte[] request = Acu10fdFrames.readHoldingRegisters(DEVICE_ADDRESS, registerAddress, 2);
        byte[] response = sendModbusRequest(request, true);
        return Acu10fdFrames.parseFloatCdab(response, 3);
    }

    private void writeRegisterValue(int registerAddress, float value) throws Exception {
        byte[] request = Acu10fdFrames.writeFloatCdab(DEVICE_ADDRESS, registerAddress, value);
        log.info("Преобразованное значение " + value + " :" + bytesToHex(request));
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
            if (!Acu10fdFrames.checkCrc(response)){
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

    // ===== Утилиты =====

    private static String bytesToHex(byte[] bytes) {
        return MyUtilities.bytesToHexString(bytes);
    }


}
