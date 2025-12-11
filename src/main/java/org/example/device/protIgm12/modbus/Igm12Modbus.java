package org.example.device.protIgm12.modbus;

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
import java.util.HashMap;
import java.util.List;

public class Igm12Modbus implements SomeDevice, ProtocolComPort, NonAscii {
    private static final Logger log = Logger.getLogger(Igm12Modbus.class);
    @Getter
    private final ComConnectParameters comParameters = new ComConnectParameters();
    private final SerialPort comPort;
    @Getter
    private final DataBitsList defaultDataBit = DataBitsList.B8;
    @Getter
    private final ParityList defaultParity = ParityList.P_EV;
    @Getter
    private final BaudRatesList defaultBaudRate = BaudRatesList.B19200;
    @Getter
    private final StopBitsList defaultStopBit = StopBitsList.S1;

    private final DeviceCommandListClass commands;
    private final Igm12ModbusCommandRegistry commandRegistry;

    private volatile byte[] lastAnswerBytes = new byte[1];
    private StringBuilder lastAnswer = new StringBuilder();
    private AnswerValues answerValues = null;
    private int received = 0;

    private String cmdToSend;
    private int expectedBytes = 0;

    private String devIdent = "IGM12";
    private byte[] rawCmd = null;
    private int slaveAddress = 1; // Default Modbus slave address

    public Igm12Modbus() {
        log.info("Created Igm10 protocol emulation object");
        this.comPort = null;
        this.commandRegistry = new Igm12ModbusCommandRegistry();
        this.commands = commandRegistry.getCommandList();
    }

    public Igm12Modbus(SerialPort port) {
        log.info("Created Igm10 protocol object");
        this.comPort = port;
        this.commandRegistry = new Igm12ModbusCommandRegistry();
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

    public void setSlaveAddress(int address) {
        this.slaveAddress = address;
    }

    public int getSlaveAddress() {
        return this.slaveAddress;
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

        commandRegistry.setSlaveAddress(slaveAddress);

        if ("getConc".equals(cmdName)) {
            bytesList.add(commandRegistry.buildReadHolding(6, 1));
        } else if ("getVersion".equals(cmdName)) {
            bytesList.add(commandRegistry.buildReadHolding(16, 1));
        } else if ("getSerial".equals(cmdName)) {
            bytesList.add(commandRegistry.buildReadHolding(1, 2));
        } else if ("getGasProperty".equals(cmdName)) {
            bytesList.add(commandRegistry.buildReadHolding(3, 1)); // Gas type
            bytesList.add(commandRegistry.buildReadHolding(8, 1)); // Range
        } else if ("setZero".equals(cmdName)) {
            bytesList.add(commandRegistry.buildWriteSingle(6, 0xAAAA));
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
            int intValue = Math.round(value * 10); // Assume %НКПР *10
            bytesList.add(commandRegistry.buildWriteSingle(6, intValue));
        } else if ("resetFactory".equals(cmdName)) {
            bytesList.add(commandRegistry.buildWriteSingle(6, 0xBBBB));
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
            boolean isKnown = false;
            log.info("Отправленная команда STW: " + MyUtilities.bytesToHex(cmdToSend.getBytes()));
            if(rawCmd == null){
                log.warn("RAW command is null");
            }else{
                log.info("Отправленная команда RAW: " + MyUtilities.bytesToHex(rawCmd));
            }
            log.info("Полученный ответ     RAW: " + MyUtilities.bytesToHex(lastAnswerBytes));

            HashMap<String, SingleCommand> commandsList = commands.getCommandPool();
            SingleCommand foundCommand = null;
            //01 03 00 06 00 01 64 0B
            //3 элемент массива это регистр
            byte[] subArray = Arrays.copyOfRange(cmdToSend.getBytes(), 0, 6);
            String cmdPattern = MyUtilities.bytesToHex(subArray);
            log.warn("Bytes:" + cmdPattern);
            for (SingleCommand value : commandsList.values()) {
                log.info("Просматривваю " + MyUtilities.bytesToHex(value.getBaseBody()) + " " + value.getGuiName());
                if (MyUtilities.compare(value.getBaseBody(), rawCmd, 1, false)){
                    log.info("Found command for [" + value.getGuiName() + "]");
                    isKnown = true;
                    foundCommand = value;
                    break;
                }
            }

            if (isKnown) {
                answerValues = foundCommand.getResult(lastAnswerBytes);
                log.info("Записываю ответ [" + answerValues + "]");
                if (answerValues != null) {
                    for (int i = 0; i < answerValues.getValues().length; i++) {
                        lastAnswer.append(answerValues.getValues()[i]);
                        lastAnswer.append("\t");
                        lastAnswer.append(answerValues.getUnits()[i]);
                    }
                } else {
                    lastAnswer.append(new String(lastAnswerBytes));
                    log.info("IGM10 Cant create answers obj (error in answer)");
                }
            } else {
                lastAnswer.setLength(0);
                lastAnswer.append(MyUtilities.bytesToHex(lastAnswerBytes));
                log.warn("IGM10 Cant create answers obj (unknown command)");
            }
        } else {
            log.info("IGM10: empty received");
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

    public void setRawCommand(byte[] cmd) {
        this.rawCmd = cmd;
    }
}