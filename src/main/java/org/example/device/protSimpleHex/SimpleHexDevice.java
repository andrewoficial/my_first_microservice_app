package org.example.device.protSimpleHex;

import com.fazecast.jSerialComm.SerialPort;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
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
import java.util.List;

@Slf4j
public class SimpleHexDevice implements SomeDevice, NonAscii, ProtocolComPort {
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
    private final SimpleHexCommandRegistry commandRegistry;

    private volatile byte[] lastAnswerBytes = new byte[1];
    private StringBuilder lastAnswer = new StringBuilder();
    private AnswerValues answerValues = null;
    private int received = 0;

    private String cmdToSend;
    private int expectedBytes = 0;

    private byte[] rawCmd = null;

    public SimpleHexDevice() {
        log.info("Создан объект протокола SimpleHex (эмуляция)");
        this.comPort = null;
        this.commandRegistry = new SimpleHexCommandRegistry();
        this.commands = commandRegistry.getCommandList();
    }

    public SimpleHexDevice(SerialPort port) {
        log.info("Создан объект протокола SimpleHex");
        this.comPort = port;
        this.commandRegistry = new SimpleHexCommandRegistry();
        this.commands = commandRegistry.getCommandList();
        comParameters.setDataBits(defaultDataBit);
        comParameters.setParity(defaultParity);
        comParameters.setBaudRate(defaultBaudRate);
        comParameters.setStopBits(defaultStopBit);
        comParameters.setStringEndian(StringEndianList.NO);
        comParameters.setMillisLimit(10000);
        comParameters.setRepeatWaitTime(500);
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
        String[] parts = cmdToSend.split("\\s+", 2);
        String cmdName = parts[0];
        List<byte[]> bytesList = new ArrayList<>();

        switch (cmdName) {
            case "sendHex":
                if (parts.length < 2) throw new IllegalArgumentException("Укажите hex-строку");
                SingleCommand cmd = commands.getCommand("sendHex");
                if (cmd != null) {
                    java.util.Map<String, Object> args = new java.util.HashMap<>();
                    args.put("hex", parts[1]);
                    bytesList.add(cmd.build(args));
                }
                break;
            default:
                log.warn("SimpleHex: Неизвестная команда: " + cmdName);
                return new ArrayList<>();
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
        if (lastAnswerBytes == null || lastAnswerBytes.length == 0) {
            log.info("SimpleHex: пустой ответ");
            return;
        }

        lastAnswer.setLength(0);
        log.info("SimpleHex отправлено: " + (rawCmd != null ? MyUtilities.bytesToHexString(rawCmd) : "null"));
        log.info("SimpleHex получено: " + MyUtilities.bytesToHexString(lastAnswerBytes));

        lastAnswer.append(MyUtilities.bytesToHexString(lastAnswerBytes));
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
