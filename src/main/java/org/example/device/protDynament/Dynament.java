package org.example.device.protDynament;

import com.fazecast.jSerialComm.SerialPort;
import lombok.Getter;
import org.apache.log4j.Logger;
import org.example.device.DeviceCommandListClass;
import org.example.device.SomeDevice;
import org.example.device.command.SingleCommand;
import org.example.device.connectParameters.ComConnectParameters;
import org.example.services.AnswerValues;
import org.example.services.comPort.*;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Dynament implements SomeDevice {
    private static final Logger log = Logger.getLogger(Dynament.class);
    @Getter
    private final ComConnectParameters comParameters = new ComConnectParameters(); // Типовые параметры связи для прибора
    private final SerialPort comPort;

    private final DeviceCommandListClass commands;
    private final DynamentCommandRegistry commandRegistry;

    private volatile byte[] lastAnswerBytes = new byte[1];
    private StringBuilder lastAnswer = new StringBuilder();
    private AnswerValues answerValues = null;
    private int received = 0;

    private String cmdToSend;
    private int expectedBytes = 0;

    private String devIdent = "DYNAMENT";

    private static final int[] BAUDRATES = {38400, 50, 75, 110, 150, 300, 600, 1200, 2400, 4800, 9600, 19200, 57600, 115200};

    public Dynament() {
        log.info("Создан объект протокола DYNAMENT эмуляция");
        this.comPort = null;
        this.commandRegistry = new DynamentCommandRegistry();
        this.commands = commandRegistry.getCommandList();
    }

    public Dynament(SerialPort port) {
        log.info("Создан объект протокола DYNAMENT");
        this.comPort = port;
        this.commandRegistry = new DynamentCommandRegistry();
        this.commands = commandRegistry.getCommandList();
        comParameters.setDataBits(DataBitsList.B8);
        comParameters.setParity(ParityList.P_NO);
        comParameters.setBaudRate(BaudRatesList.B38400); // Default baudrate from Python
        comParameters.setStopBits(StopBitsList.S1);
        comParameters.setStringEndian(StringEndianList.CR);
        comParameters.setMillisLimit(3000); // Timeout from Python
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
            expectedBytes = commands.getExpectedBytes(str.split(" ")[0]); // Ignore params for expected
            cmdToSend = str;
        }
    }

    /**
     * Returns the list of byte arrays to send for the command.
     * For simple reads, one array; for sets, multiple (WR then DAT).
     */
    public List<byte[]> getBytesToSend() {
        if (cmdToSend == null) {
            return new ArrayList<>();
        }
        String[] parts = cmdToSend.split("\\s+");
        String cmdName = parts[0];
        List<byte[]> bytesList = new ArrayList<>();

        if ("getConc".equals(cmdName)) {
            bytesList.add(commandRegistry.buildReadCommand(0x01));
        } else if ("getVersion".equals(cmdName)) {
            bytesList.add(commandRegistry.buildReadCommand(0x00));
        } else if ("setZero".equals(cmdName)) {
            bytesList.add(commandRegistry.buildWriteCommand(0x02, true));
            bytesList.add(commandRegistry.buildDataFrame(new byte[0]));
        } else if ("setConc".equals(cmdName)) {
            if (parts.length < 2) {
                throw new IllegalArgumentException("Value required for setConc");
            }
            float value;
            try {
                value = Float.parseFloat(parts[1]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid value for setConc");
            }
            ByteBuffer bb = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(value);
            byte[] valueBytes = bb.array();
            bytesList.add(commandRegistry.buildWriteCommand(0x03, true));
            bytesList.add(commandRegistry.buildDataFrame(valueBytes));
        } else if ("searchBaudrate".equals(cmdName)) {
            // Search is handled separately
            searchBaudrate();
            return new ArrayList<>();
        }
        return bytesList;
    }

    /**
     * Searches for the working baudrate by trying each one.
     * Sets the comPort baudrate if found.
     * @return found baudrate or -1 if not found
     */
    public int searchBaudrate() {
        byte[] requestWithoutCs = new byte[]{0x10, 0x13, 0x01, 0x10, 0x1F};
        byte[] checksum = commandRegistry.calculateChecksum(requestWithoutCs);
        byte[] request = new byte[requestWithoutCs.length + 2];
        System.arraycopy(requestWithoutCs, 0, request, 0, requestWithoutCs.length);
        System.arraycopy(checksum, 0, request, requestWithoutCs.length, 2);

        int originalBaud = comPort.getBaudRate();
        for (int baud : BAUDRATES) {
            comPort.setBaudRate(baud);
            // Flush or wait
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
                //boolean success = commandRegistry.getCommandList().getCommand()response);
                boolean success = true;
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
            HashMap <String, SingleCommand> commandsList = commands.getCommandPool();
            SingleCommand foundetCommand = null;
            for (SingleCommand value : commandsList.values()) {
                if(Arrays.equals(value.getBaseBody(), cmdToSend.getBytes())){
                    log.info("Compare " + Arrays.toString(value.getBaseBody()) + " and " + Arrays.toString(cmdToSend.getBytes()));
                    isKnown = true;
                    foundetCommand = value;
                    break;
                }
            }
            if (isKnown) {
                answerValues = foundetCommand.getResult(lastAnswerBytes);
                if (answerValues != null) {
                    for (int i = 0; i < answerValues.getValues().length; i++) {
                        lastAnswer.append(answerValues.getValues()[i]);
                        lastAnswer.append("\t");
                    }
                } else {
                    lastAnswer.append(new String(lastAnswerBytes));
                    log.info("DYNAMENT Cant create answers obj (error in answer)");
                }
            } else {
                lastAnswer.append(new String(lastAnswerBytes));
                log.info("DYNAMENT Cant create answers obj (unknown command)");
            }
        } else {
            log.info("DYNAMENT empty received");
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
    public boolean isASCII(){
        return false;
    }
    public AnswerValues getValues() {
        return this.answerValues;
    }
}