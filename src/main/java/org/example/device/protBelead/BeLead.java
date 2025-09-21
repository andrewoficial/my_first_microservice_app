package org.example.device.protBelead;

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

public class BeLead implements SomeDevice, NonAscii, ProtocolComPort {
    private static final Logger log = Logger.getLogger(BeLead.class);
    @Getter
    private final ComConnectParameters comParameters = new ComConnectParameters(); // Типовые параметры связи для прибора
    private final SerialPort comPort;
    @Getter
    private final DataBitsList defaultDataBit = DataBitsList.B8;
    @Getter
    private final ParityList defaultParity= ParityList.P_NO;
    @Getter
    private final BaudRatesList defaultBaudRate = BaudRatesList.B57600;
    @Getter
    private final StopBitsList defaultStopBit = StopBitsList.S1;

    private final DeviceCommandListClass commands;
    private final BeLeadCommandRegistry commandRegistry;

    private volatile byte[] lastAnswerBytes = new byte[1];
    private StringBuilder lastAnswer = new StringBuilder();
    private AnswerValues answerValues = null;
    private int received = 0;

    private String cmdToSend;
    private int expectedBytes = 0;

    private String devIdent = "BLDM";
    private byte [] rawCmd = null;

    private static final int[] BAUDRATES = {38400, 50, 75, 110, 150, 300, 600, 1200, 2400, 4800, 9600, 19200, 57600, 115200};

    public BeLead() {
        log.info("Создан объект протокола BELEAD эмуляция");
        this.comPort = null;
        this.commandRegistry = new BeLeadCommandRegistry();
        this.commands = commandRegistry.getCommandList();
    }

    public BeLead(SerialPort port) {
        log.info("Создан объект протокола BELEAD");
        this.comPort = port;
        this.commandRegistry = new BeLeadCommandRegistry();
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

        if ("getAllData".equals(cmdName)) {
            bytesList.add(commandRegistry.buildReadCommand((byte) 0x13, null));
        } else if ("getSimpleData".equals(cmdName)) {
            bytesList.add(commandRegistry.buildReadCommand((byte) 0x06, null));
        } else if ("setZero".equals(cmdName)) {
            bytesList.add(commandRegistry.buildWriteCommand((byte) 0x02, null));
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
            bytesList.add(commandRegistry.buildWriteCommand((byte) 0x03, valueBytes));
        } else if ("searchBaudrate".equals(cmdName)) {
            searchBaudrate();
            return new ArrayList<>();
        }
        return bytesList;
    }

    public int searchBaudrate() {
        // Adapted from Dynament; test with a read command
        byte[] request = commandRegistry.buildReadCommand((byte) 0x13, null);

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
                // Validate if response is valid (e.g., starts with START, etc.)
                boolean success = true; // Placeholder; add actual validation if needed
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
        //rawCmd
        if (lastAnswerBytes != null && lastAnswerBytes.length > 0) {
            lastAnswer.setLength(0);
            String cmdName = cmdToSend != null ? cmdToSend.split(" ")[0] : "";
            boolean isKnown = false;
            log.info("Отправленная команда: " + MyUtilities.bytesToHex(rawCmd));
            log.info("Полученный ответ: " + MyUtilities.bytesToHex(lastAnswerBytes));

            HashMap <String, SingleCommand> commandsList = commands.getCommandPool();
            SingleCommand foundetCommand = null;

            byte[]  sentPart = new byte[3];
            //Заменено на rawCmd вместо сломанного от стрин билдера
            System.arraycopy(rawCmd, 0, sentPart, 0, 3);

            byte[]  commandPart = new byte[3];
            System.arraycopy(rawCmd, 0, sentPart, 0, 3);
            log.info("Определяю команду... ");
            for (SingleCommand value : commandsList.values()) {
                log.info("Готовлюсь к просмотру тела команды (имя): " + value.getGuiName());
                //log.info("Готовлюсь к просмотру тела команды (значение): " +  MyUtilities.bytesToHex(value.getBaseBody()));
                System.arraycopy(value.getBaseBody(), 0, commandPart, 0, 3);
                log.info("Сравниваю commandPart: " +  MyUtilities.bytesToHex(commandPart)  + " sentPart: " + MyUtilities.bytesToHex(sentPart));
                if(Arrays.equals(commandPart, sentPart)){
                    log.info("Found command pattern for command [" + value.getMapKey() + "]");
                    isKnown = true;
                    foundetCommand = value;
                    break;
                }
            }
            //log.info("Завершил поиск команды");
            if (isKnown) {

                answerValues = foundetCommand.getResult(lastAnswerBytes);
                log.info("Записываю ответ [" +answerValues + "]");
                if (answerValues != null) {
                    for (int i = 0; i < answerValues.getValues().length; i++) {
                        lastAnswer.append(answerValues.getValues()[i]);
                        lastAnswer.append("\t");
                    }
                } else {
                    lastAnswer.append(new String(lastAnswerBytes));
                    log.info("BELEAD Cant create answers obj (error in answer)");
                }
            } else {
                lastAnswer.setLength(0);
                lastAnswer.append(MyUtilities.bytesToHex(lastAnswerBytes));
                log.info("BELEAD Cant create answers obj (unknown command)");
            }
        } else {
            log.info("BELEAD: empty received");
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