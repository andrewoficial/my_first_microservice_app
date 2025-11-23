package org.example.device.protFnirsiDps150;

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

public class FNIRSI_DPS150 implements SomeDevice, NonAscii, ProtocolComPort {
    private static final Logger log = Logger.getLogger(FNIRSI_DPS150.class);
    @Getter
    private final ComConnectParameters comParameters = new ComConnectParameters(); // Типовые параметры связи для прибора
    private final SerialPort comPort;
    @Getter
    private final DataBitsList defaultDataBit = DataBitsList.B8;
    @Getter
    private final ParityList defaultParity = ParityList.P_NO;
    @Getter
    private final BaudRatesList defaultBaudRate = BaudRatesList.B115200; // Assumed; may need adjustment or use searchBaudrate
    @Getter
    private final StopBitsList defaultStopBit = StopBitsList.S1;

    private final DeviceCommandListClass commands;
    private final FnirsiDps150CommandRegistry commandRegistry;

    private volatile byte[] lastAnswerBytes = new byte[1];
    private StringBuilder lastAnswer = new StringBuilder();
    private AnswerValues answerValues = null;
    private int received = 0;

    private String cmdToSend;
    private int expectedBytes = 0;

    private String devIdent = "FNIRSI_DPS150";
    private byte[] rawCmd = null;

    private static final int[] BAUDRATES = {9600, 19200, 38400, 57600, 115200}; // Limited to common rates

    public FNIRSI_DPS150() {
        log.info("Создан объект протокола FNIRSI_DPS150 эмуляция");
        this.comPort = null;
        this.commandRegistry = new FnirsiDps150CommandRegistry();
        this.commands = commandRegistry.getCommandList();
    }

    public FNIRSI_DPS150(SerialPort port) {
        log.info("Создан объект протокола FNIRSI_DPS150");
        this.comPort = port;
        this.commandRegistry = new FnirsiDps150CommandRegistry();
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

        if ("getVout".equals(cmdName)) {
            bytesList.add(commandRegistry.buildReadCommand(FnirsiDps150CommandRegistry.TYPE_VOUT_MEAS));
        } else if ("getIout".equals(cmdName)) {
            bytesList.add(commandRegistry.buildReadCommand(FnirsiDps150CommandRegistry.TYPE_IOUT_MEAS));
        } else if ("getPower".equals(cmdName)) {
            bytesList.add(commandRegistry.buildReadCommand(FnirsiDps150CommandRegistry.TYPE_POWER));
        } else if ("getTemp".equals(cmdName)) {
            bytesList.add(commandRegistry.buildReadCommand(FnirsiDps150CommandRegistry.TYPE_TEMP));
        } else if ("getOutput".equals(cmdName)) {
            bytesList.add(commandRegistry.buildReadCommand(FnirsiDps150CommandRegistry.TYPE_OUTPUT));
        } else if ("setVout".equals(cmdName)) {
            if (parts.length < 2) {
                throw new IllegalArgumentException("Value required for setVout");
            }
            float value;
            try {
                value = Float.parseFloat(parts[1]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid value for setVout");
            }
            ByteBuffer bb = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(value);
            byte[] valueBytes = bb.array();
            bytesList.add(commandRegistry.buildWriteCommand(FnirsiDps150CommandRegistry.TYPE_VOUT_SET, valueBytes));
        } else if ("setIout".equals(cmdName)) {
            if (parts.length < 2) {
                throw new IllegalArgumentException("Value required for setIout");
            }
            float value;
            try {
                value = Float.parseFloat(parts[1]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid value for setIout");
            }
            ByteBuffer bb = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(value);
            byte[] valueBytes = bb.array();
            bytesList.add(commandRegistry.buildWriteCommand(FnirsiDps150CommandRegistry.TYPE_IOUT_SET, valueBytes));
        } else if ("setOutput".equals(cmdName)) {
            if (parts.length < 2) {
                throw new IllegalArgumentException("Value required for setOutput (0 or 1)");
            }
            byte value = Byte.parseByte(parts[1]);
            byte[] valueBytes = {value};
            bytesList.add(commandRegistry.buildWriteCommand(FnirsiDps150CommandRegistry.TYPE_OUTPUT, valueBytes));
        } else if ("searchBaudrate".equals(cmdName)) {
            searchBaudrate();
            return new ArrayList<>();
        }
        return bytesList;
    }

    public int searchBaudrate() {
        byte[] request = commandRegistry.buildReadCommand(FnirsiDps150CommandRegistry.TYPE_VOUT_MEAS); // Use a simple read command for testing

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
                if (commandRegistry.validateChecksum(response, false)) {
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

            byte[] sentPart = new byte[3];
            System.arraycopy(rawCmd, 0, sentPart, 0, 3);

            byte[] commandPart = new byte[3];
            log.info("Определяю команду... ");
            for (SingleCommand value : commandsList.values()) {
                System.arraycopy(value.getBaseBody(), 0, commandPart, 0, 3);
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
                    log.info("FNIRSI_DPS150 Cant create answers obj (error in answer)");
                }
            } else {
                lastAnswer.setLength(0);
                lastAnswer.append(MyUtilities.bytesToHex(lastAnswerBytes));
                log.info("FNIRSI_DPS150 Cant create answers obj (unknown command)");
            }
        } else {
            log.info("FNIRSI_DPS150: empty received");
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