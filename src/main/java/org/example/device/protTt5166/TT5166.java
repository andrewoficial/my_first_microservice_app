package org.example.device.protTt5166;

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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class TT5166 implements SomeDevice, NonAscii, ProtocolComPort {
    private static final Logger log = Logger.getLogger(TT5166.class);
    @Getter
    private final ComConnectParameters comParameters = new ComConnectParameters(); // Типовые параметры связи для прибора
    private final SerialPort comPort;
    @Getter
    private final DataBitsList defaultDataBit = DataBitsList.B8;
    @Getter
    private final ParityList defaultParity = ParityList.P_EV;
    @Getter
    private final BaudRatesList defaultBaudRate = BaudRatesList.B38400;
    @Getter
    private final StopBitsList defaultStopBit = StopBitsList.S1;

    private final DeviceCommandListClass commands;
    private final TT5166CommandRegistry commandRegistry;

    private volatile byte[] lastAnswerBytes = new byte[1];
    private StringBuilder lastAnswer = new StringBuilder();
    private AnswerValues answerValues = null;
    private int received = 0;

    private String cmdToSend;
    private int expectedBytes = 0;

    private String devIdent = "TT5166";
    private byte[] rawCmd = null;

    private static final int[] BAUDRATES = {38400, 50, 75, 110, 150, 300, 600, 1200, 2400, 4800, 9600, 19200, 57600, 115200};

    public TT5166() {
        log.info("Создан объект протокола TT5166 эмуляция");
        this.comPort = null;
        this.commandRegistry = new TT5166CommandRegistry();
        this.commands = commandRegistry.getCommandList();
    }

    public TT5166(SerialPort port) {
        log.info("Создан объект протокола TT5166");
        this.comPort = port;
        this.commandRegistry = new TT5166CommandRegistry();
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

        if ("getData".equals(cmdName)) {
            bytesList.add(commandRegistry.buildReadRegisters((byte) 0x01, 0x0000, 6));
        } else if ("getFault".equals(cmdName)) {
            bytesList.add(commandRegistry.buildReadRegisters((byte) 0x01, 0x001B, 1));
        } else if ("start".equals(cmdName)) {
            bytesList.add(commandRegistry.buildForceCoil((byte) 0x01, 0x0000, true));
        } else if ("stop".equals(cmdName)) {
            bytesList.add(commandRegistry.buildForceCoil((byte) 0x01, 0x0001, true));
        } else if ("setConstTemp".equals(cmdName)) {
            if (parts.length < 2) {
                throw new IllegalArgumentException("Value required for setConstTemp");
            }
            float value;
            try {
                value = Float.parseFloat(parts[1]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid value for setConstTemp");
            }
            short sval = (short) (value * 10);
            bytesList.add(commandRegistry.buildWriteRegister((byte) 0x01, 0x0026, sval));
        } else if ("setConstHum".equals(cmdName)) {
            if (parts.length < 2) {
                throw new IllegalArgumentException("Value required for setConstHum");
            }
            float value;
            try {
                value = Float.parseFloat(parts[1]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid value for setConstHum");
            }
            short sval = (short) (value * 10);
            bytesList.add(commandRegistry.buildWriteRegister((byte) 0x01, 0x0027, sval));
        } else if ("searchBaudrate".equals(cmdName)) {
            searchBaudrate();
            return new ArrayList<>();
        }
        return bytesList;
    }

    public int searchBaudrate() {
        byte[] request = commandRegistry.buildReadRegisters((byte) 0x01, 0x0000, 1);

        int originalBaud = comPort.getBaudRate();
        for (int baud : BAUDRATES) {
            comPort.setBaudRate(baud);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // ignore
            }
            comPort.writeBytes(request, request.length);
            byte[] buffer = new byte[100];
            int read = comPort.readBytes(buffer, buffer.length);
            if (read > 0) {
                byte[] response = new byte[read];
                System.arraycopy(buffer, 0, response, 0, read);
                boolean success = commandRegistry.validateChecksum(response);
                if (success) {
                    log.info("Found baudrate: " + baud);
                    return baud;
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
        if (lastAnswerBytes != null && lastAnswerBytes.length > 0) {
            lastAnswer.setLength(0);
            String cmdName = cmdToSend != null ? cmdToSend.split(" ")[0] : "";
            boolean isKnown = false;
            log.info("Отправленная команда: " + MyUtilities.bytesToHex(rawCmd));
            log.info("Полученный ответ: " + MyUtilities.bytesToHex(lastAnswerBytes));

            HashMap<String, SingleCommand> commandsList = commands.getCommandPool();
            SingleCommand foundetCommand = null;

            byte[] sentPart = new byte[4];
            System.arraycopy(rawCmd, 0, sentPart, 0, 4);

            byte[] commandPart = new byte[4];//Увеличил включив счетчик
            log.info("Определяю команду... ");
            for (SingleCommand value : commandsList.values()) {
                System.arraycopy(value.getBaseBody(), 0, commandPart, 0, 4);//Увеличил включив счетчик
                if (Arrays.equals(commandPart, sentPart)) {
                    log.info("Found command pattern for command [" + value.getMapKey() + "]");
                    isKnown = true;
                    foundetCommand = value;
                    break;
                }
            }
            if (isKnown) {
                answerValues = foundetCommand.getResult(lastAnswerBytes);
                log.info("Записываю ответ [" + answerValues + "]");
                if (answerValues != null) {
                    for (int i = 0; i < answerValues.getValues().length; i++) {
                        lastAnswer.append(answerValues.getValues()[i]);
                        lastAnswer.append("\t");
                    }
                } else {
                    lastAnswer.append(new String(lastAnswerBytes));
                    log.info("TT5166 Cant create answers obj (error in answer)");
                }
            } else {
                lastAnswer.setLength(0);
                lastAnswer.append(MyUtilities.bytesToHex(lastAnswerBytes));
                log.info("TT5166 Cant create answers obj (unknown command)");
            }
        } else {
            log.info("TT5166: empty received");
        }
    }

    public String getAnswer() {
        if (hasAnswer()) {
            received = 0;
            lastAnswerBytes = null;
            return lastAnswer.toString();
        } else {
            return null;
        }
    }

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