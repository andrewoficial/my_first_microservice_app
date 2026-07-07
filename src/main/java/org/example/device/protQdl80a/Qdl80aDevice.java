package org.example.device.protQdl80a;

import com.fazecast.jSerialComm.SerialPort;
import lombok.Getter;
import org.apache.log4j.Logger;
import org.example.device.DeviceCommandListClass;
import org.example.device.NonAscii;
import org.example.device.ProtocolComPort;
import org.example.device.SomeDevice;
import org.example.device.command.SingleCommand;
import org.example.device.connectParameters.ComConnectParameters;
import org.example.services.AnswerValues;
import org.example.services.comPort.*;
import org.example.utilites.MyUtilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Реализация протокола для датчика QDL80A (Anhui Qudian).
 * Протокол: Modbus RTU, RS-485, half-duplex.
 */
public class Qdl80aDevice implements SomeDevice, NonAscii, ProtocolComPort {
    private static final Logger log = Logger.getLogger(Qdl80aDevice.class);

    @Getter
    private final ComConnectParameters comParameters = new ComConnectParameters();
    private final SerialPort comPort;

    @Getter
    private final DataBitsList defaultDataBit = DataBitsList.B8;
    @Getter
    private final ParityList defaultParity = ParityList.P_NO;
    @Getter
    private final BaudRatesList defaultBaudRate = BaudRatesList.B9600;
    @Getter
    private final StopBitsList defaultStopBit = StopBitsList.S1;

    private final DeviceCommandListClass commands;
    private final Qdl80aCommandRegistry commandRegistry;

    private volatile byte[] lastAnswerBytes = new byte[1];
    private StringBuilder lastAnswer = new StringBuilder();
    private AnswerValues answerValues = null;
    private int received = 0;

    private String cmdToSend;
    private int expectedBytes = 0;

    private byte[] rawCmd = null;

    // Поддерживаемые скорости (из документации)
    private static final int[] BAUDRATES = {1200, 2400, 4800, 9600, 19200, 38400};

    // Адрес устройства по умолчанию
    private byte deviceAddress = 0x01;

    public Qdl80aDevice() {
        log.info("Создан объект протокола QDL80A (эмуляция)");
        this.comPort = null;
        this.commandRegistry = new Qdl80aCommandRegistry();
        this.commands = commandRegistry.getCommandList();
    }

    public Qdl80aDevice(SerialPort port) {
        log.info("Создан объект протокола QDL80A");
        this.comPort = port;
        this.commandRegistry = new Qdl80aCommandRegistry();
        this.commands = commandRegistry.getCommandList();
        comParameters.setDataBits(defaultDataBit);
        comParameters.setParity(defaultParity);
        comParameters.setBaudRate(defaultBaudRate);
        comParameters.setStopBits(defaultStopBit);
        comParameters.setStringEndian(StringEndianList.CR);
        comParameters.setMillisLimit(3000);
        comParameters.setRepeatWaitTime(250);
        this.enable();
    }

    @Override
    public DeviceCommandListClass getCommandListClass() {
        return this.commands;
    }

    @Override
    public void setCmdToSend(String str) {
        if (str == null || str.isEmpty()) {
            expectedBytes = 500;
            cmdToSend = null;
        } else {
            expectedBytes = commands.getExpectedBytes(str.split(" ")[0]);
            cmdToSend = str;
        }
    }

    public List<byte[]> getBytesToSend() {
        if (cmdToSend == null) {
            return new ArrayList<>();
        }
        String[] parts = cmdToSend.split("\\s+");
        String cmdName = parts[0];
        List<byte[]> bytesList = new ArrayList<>();

        Qdl80aCommandRegistry registry = commandRegistry;

        switch (cmdName) {
            // === Чтение данных ===
            case "readMeasurement":
                // Чтение первичного измерения (регистр H:4)
                bytesList.add(registry.buildReadRequest(deviceAddress, (byte)0x03, (short)0x0004, (short)1));
                break;
            case "readFloatMeasurement":
                // Чтение значения в формате float (если поддерживается)
                bytesList.add(registry.buildReadRequest(deviceAddress, (byte)0x03, (short)0x0016, (short)2));
                break;
            case "readAddress":
                // Чтение адреса (регистр H:0)
                bytesList.add(registry.buildReadRequest(deviceAddress, (byte)0x03, (short)0x0000, (short)1));
                break;
            case "readBaudRate":
                // Чтение кода скорости (регистр H:1)
                bytesList.add(registry.buildReadRequest(deviceAddress, (byte)0x03, (short)0x0001, (short)1));
                break;
            case "readUnit":
                // Чтение кода единиц измерения (регистр H:2)
                bytesList.add(registry.buildReadRequest(deviceAddress, (byte)0x03, (short)0x0002, (short)1));
                break;
            case "readDecimalPoints":
                // Чтение количества десятичных знаков (регистр H:3)
                bytesList.add(registry.buildReadRequest(deviceAddress, (byte)0x03, (short)0x0003, (short)1));
                break;

            // === Запись данных ===
            case "writeAddress":
                if (parts.length < 2) throw new IllegalArgumentException("Укажите новый адрес (1-247)");
                int newAddr = Integer.parseInt(parts[1]);
                if (newAddr < 1 || newAddr > 247) throw new IllegalArgumentException("Адрес должен быть 1..247");
                bytesList.add(registry.buildWriteSingleRegisterRequest(deviceAddress, (short)0x0000, (short)newAddr));
                break;
            case "writeBaudRate":
                if (parts.length < 2) throw new IllegalArgumentException("Укажите код скорости (0-5)");
                int baudCode = Integer.parseInt(parts[1]);
                if (baudCode < 0 || baudCode > 5) throw new IllegalArgumentException("Код скорости 0..5");
                bytesList.add(registry.buildWriteSingleRegisterRequest(deviceAddress, (short)0x0001, (short)baudCode));
                break;
            case "writeUnit":
                if (parts.length < 2) throw new IllegalArgumentException("Укажите код единиц (см. документацию)");
                int unitCode = Integer.parseInt(parts[1]);
                bytesList.add(registry.buildWriteSingleRegisterRequest(deviceAddress, (short)0x0002, (short)unitCode));
                break;
            case "writeZeroOffset":
                if (parts.length < 2) throw new IllegalArgumentException("Укажите смещение (-32768..32767)");
                int offset = Integer.parseInt(parts[1]);
                if (offset < -32768 || offset > 32767) throw new IllegalArgumentException("Смещение вне диапазона");
                bytesList.add(registry.buildWriteSingleRegisterRequest(deviceAddress, (short)0x000C, (short)offset));
                break;
            case "saveParameters":
                // Сохранение в пользовательскую область (регистр H:F, значение 0)
                bytesList.add(registry.buildWriteSingleRegisterRequest(deviceAddress, (short)0x000F, (short)0));
                break;
            case "restoreFactory":
                // Сброс к заводским настройкам (регистр H:10, значение 1)
                bytesList.add(registry.buildWriteSingleRegisterRequest(deviceAddress, (short)0x0010, (short)1));
                break;
            case "searchBaudrate":
                searchBaudrate();
                return new ArrayList<>();
            default:
                log.warn("Неизвестная команда: " + cmdName);
                return new ArrayList<>();
        }
        return bytesList;
    }

    /**
     * Поиск рабочей скорости путём перебора.
     * Пытается прочитать регистр адреса (H:0).
     */
    public int searchBaudrate() {
        byte[] request = commandRegistry.buildReadRequest(deviceAddress, (byte)0x03, (short)0x0000, (short)1);
        int originalBaud = comPort.getBaudRate();

        for (int baud : BAUDRATES) {
            comPort.setBaudRate(baud);
            try { Thread.sleep(100); } catch (InterruptedException ignored) {}

            comPort.writeBytes(request, request.length);
            byte[] buffer = new byte[100];
            int read = comPort.readBytes(buffer, buffer.length);

            if (read > 0) {
                byte[] response = new byte[read];
                System.arraycopy(buffer, 0, response, 0, read);

                if (response.length >= 5 && Qdl80aCommandRegistry.validateCRC(response)) {
                    if (response[0] == deviceAddress && (response[1] == 0x03 || response[1] == 0x04)) {
                        log.info("Найдена скорость: " + baud);
                        return baud;
                    }
                }
            }
        }

        comPort.setBaudRate(originalBaud);
        return -1;
    }

    @Override
    public int getExpectedBytes() {
        return expectedBytes;
    }

    @Override
    public byte[] getStrEndian() {
        return this.comParameters.getStringEndian().getBytes();
    }

    @Override
    public SerialPort getComPort() {
        return this.comPort;
    }

    @Override
    public boolean isKnownCommand() {
        return commands.isKnownCommand(cmdToSend.split(" ")[0]);
    }

    @Override
    public int getReceivedCounter() {
        return received;
    }

    @Override
    public void setReceivedCounter(int cnt) {
        this.received = cnt;
    }

    public void setReceived(String answer) {
        lastAnswerBytes = answer.getBytes();
        this.received = lastAnswerBytes.length;
    }

    @Override
    public long getMillisLimit() {
        return comParameters.getMillisLimit();
    }

    @Override
    public int getMillisReadLimit() {
        return comParameters.getMillisReadLimit();
    }

    @Override
    public int getMillisWriteLimit() {
        return comParameters.getMillisWriteLimit();
    }

    @Override
    public long getRepeatWaitTime() {
        return comParameters.getRepeatWaitTime();
    }

    @Override
    public void setLastAnswer(byte[] sb) {
        lastAnswerBytes = sb;
    }

    public boolean enable() {
        return true;
    }

    @Override
    public void parseData() {
        if (lastAnswerBytes == null || lastAnswerBytes.length == 0) {
            log.info("QDL80A: пустой ответ");
            return;
        }

        lastAnswer.setLength(0);
        String cmdName = cmdToSend != null ? cmdToSend.split(" ")[0] : "";
        log.info("Отправленная команда: " + MyUtilities.bytesToHexString(rawCmd));
        log.info("Полученный ответ: " + MyUtilities.bytesToHexString(lastAnswerBytes));

        // Поиск команды по префиксу (адрес + функция)
        byte[] sentPrefix = new byte[2];
        System.arraycopy(rawCmd, 0, sentPrefix, 0, 2);
        SingleCommand foundCommand = null;

        for (SingleCommand cmd : commands.getCommandPool().values()) {
            byte[] cmdPrefix = new byte[2];
            System.arraycopy(cmd.getBaseBody(), 0, cmdPrefix, 0, 2);
            if (Arrays.equals(sentPrefix, cmdPrefix)) {
                foundCommand = cmd;
                break;
            }
        }

        if (foundCommand != null) {
            answerValues = foundCommand.getResult(lastAnswerBytes);
            if (answerValues != null) {
                for (Object val : answerValues.getValues()) {
                    lastAnswer.append(val).append("\t");
                }
            } else {
                lastAnswer.append(MyUtilities.bytesToHexString(lastAnswerBytes));
                log.warn("QDL80A: не удалось распарсить ответ");
            }
        } else {
            lastAnswer.append(MyUtilities.bytesToHexString(lastAnswerBytes));
            log.warn("QDL80A: неизвестная команда");
        }
    }

    @Override
    public String getAnswer() {
        if (hasAnswer()) {
            received = 0;
            lastAnswerBytes = null;
            return lastAnswer.toString();
        }
        return null;
    }

    @Override
    public boolean hasAnswer() {
        return lastAnswerBytes != null && lastAnswerBytes.length > 0;
    }

    @Override
    public boolean hasValue() {
        return answerValues != null;
    }

    @Override
    public boolean isASCII() {
        return false;
    }

    public AnswerValues getValues() {
        return this.answerValues;
    }

    @Override
    public void setRawCommand(byte[] cmd) {
        this.rawCmd = cmd;
    }
}