package org.example.device.protCubic;

import com.fazecast.jSerialComm.SerialPort;
import lombok.Getter;
import org.apache.log4j.Logger;
import org.example.device.DeviceCommandListClass;
import org.example.device.NonAscii;
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

public class Cubic implements SomeDevice {
    private static final Logger log = Logger.getLogger(Cubic.class);
    @Getter
    private final ComConnectParameters comParameters = new ComConnectParameters();
    private final SerialPort comPort;

    private final DeviceCommandListClass commands;
    private final CubicCommandRegistry commandRegistry;

    private volatile byte[] lastAnswerBytes = new byte[1];
    private StringBuilder lastAnswer = new StringBuilder();
    private AnswerValues answerValues = null;
    private int received = 0;

    private String cmdToSend;
    private int expectedBytes = 0;

    private String devIdent = "CUBIC";
    private byte [] rawCmd = null;

    public Cubic() {
        log.info("Created Cubic protocol emulation object");
        this.comPort = null;
        this.commandRegistry = new CubicCommandRegistry();
        this.commands = commandRegistry.getCommandList();
    }

    public Cubic(SerialPort port) {
        log.info("Created Cubic protocol object");
        this.comPort = port;
        this.commandRegistry = new CubicCommandRegistry();
        this.commands = commandRegistry.getCommandList();
        comParameters.setDataBits(DataBitsList.B8);
        comParameters.setParity(ParityList.P_NO);
        comParameters.setBaudRate(BaudRatesList.B9600);
        comParameters.setStopBits(StopBitsList.S1);
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

        if ("getConc".equals(cmdName)) {
            bytesList.add(commandRegistry.buildCommand(0x01, new byte[0]));
        } else if ("getVersion".equals(cmdName)) {
            bytesList.add(commandRegistry.buildCommand(0x1E, new byte[0]));
        } else if ("getSerial".equals(cmdName)) {
            bytesList.add(commandRegistry.buildCommand(0x1F, new byte[0]));
        } else if ("getGasProperty".equals(cmdName)) {
            bytesList.add(commandRegistry.buildCommand(0x0D, new byte[0]));
        } else if ("setZero".equals(cmdName)) {
            bytesList.add(commandRegistry.buildCommand(0x4B, new byte[]{0x00, 0x00, 0x00}));
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
            int intValue = Math.round(value); // Assume n=0
            byte df1 = (byte) ((intValue >> 8) & 0xFF);
            byte df2 = (byte) (intValue & 0xFF);
            bytesList.add(commandRegistry.buildCommand(0x03, new byte[]{df1, df2}));
        } else if ("resetFactory".equals(cmdName)) {
            bytesList.add(commandRegistry.buildCommand(0x4D, new byte[]{0x00}));
        }
        return bytesList;
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
            log.info("Отправленная команда: " + MyUtilities.bytesToHex(cmdToSend.getBytes()));
            log.info("Полученный ответ: " + MyUtilities.bytesToHex(lastAnswerBytes));

            HashMap <String, SingleCommand> commandsList = commands.getCommandPool();
            SingleCommand foundetCommand = null;

            byte[]  sentPart = new byte[3];
            System.arraycopy(cmdToSend.getBytes(), 0, sentPart, 0, 3);

            byte[]  commandPart = new byte[3];
            System.arraycopy(cmdToSend.getBytes(), 0, sentPart, 0, 3);
            //log.info("Определяю команду... ");
            for (SingleCommand value : commandsList.values()) {
                //log.info("Готовлюсь к просмотру тела команды (имя): " + value.getGuiName());
                //log.info("Готовлюсь к просмотру тела команды (значение): " +  MyUtilities.bytesToHex(value.getBaseBody()));
                System.arraycopy(value.getBaseBody(), 0, commandPart, 0, 3);
                //log.info("Сравниваю commandPart: " +  MyUtilities.bytesToHex(commandPart)  + " sentPart: " + MyUtilities.bytesToHex(sentPart));
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
                    log.info("CUBIC Cant create answers obj (error in answer)");
                }
            } else {
                lastAnswer.setLength(0);
                lastAnswer.append(MyUtilities.bytesToHex(lastAnswerBytes));
                log.info("CUBIC Cant create answers obj (unknown command)");
            }
        } else {
            log.info("CUBIC: empty received");
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

    //@Override
    public void setRawCommand(byte[] cmd) {
        this.rawCmd = cmd;
    }
}