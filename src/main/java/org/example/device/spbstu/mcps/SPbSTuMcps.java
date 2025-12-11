package org.example.device.spbstu.mcps;

import com.fazecast.jSerialComm.SerialPort;
import lombok.Getter;
import org.apache.log4j.Logger;
import org.example.device.DeviceCommandListClass;
import org.example.device.SomeDevice;
import org.example.device.ProtocolComPort;
import org.example.device.TemplatedAscii;
import org.example.device.command.ArgumentDescriptor;
import org.example.device.command.SingleCommand;
import org.example.device.connectParameters.ComConnectParameters;
import org.example.services.AnswerValues;
import org.example.services.comPort.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SPbSTuMcps implements SomeDevice, ProtocolComPort, TemplatedAscii {
    private static final Logger log = Logger.getLogger(SPbSTuMcps.class);
    @Getter
    private final ComConnectParameters comParameters = new ComConnectParameters();
    private final SerialPort comPort;
    private final DeviceCommandListClass commands;
    private final SPbSTuMcpsCommandRegistry commandRegistry;

    private volatile byte[] lastAnswerBytes = new byte[1];
    private StringBuilder lastAnswer = new StringBuilder();
    private AnswerValues answerValues = null;
    private int received = 0;

    private String cmdToSend;
    private int expectedBytes = 0;

    private String devIdent = "SPBSTUMCPS";

    public SPbSTuMcps() {
        log.info("Создан объект протокола SPbSTuMcps эмуляция");
        this.comPort = null;
        this.commandRegistry = new SPbSTuMcpsCommandRegistry();
        this.commands = commandRegistry.getCommandList();
    }

    public SPbSTuMcps(SerialPort port) {
        log.info("Создан объект протокола SPbSTuMcps");
        this.comPort = port;
        this.commandRegistry = new SPbSTuMcpsCommandRegistry();
        this.commands = commandRegistry.getCommandList();
        comParameters.setDataBits(DataBitsList.B8);
        comParameters.setParity(ParityList.P_NO);
        comParameters.setBaudRate(BaudRatesList.B9600);
        comParameters.setStopBits(StopBitsList.S1);
        comParameters.setStringEndian(StringEndianList.CR_LF); // Adjusted to CR, but overridden below
        comParameters.setMillisLimit(400);
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
            expectedBytes = commands.getExpectedBytes(str);
            cmdToSend = str;
        }
    }

    @Override
    public int getExpectedBytes() {
        return expectedBytes;
    }

    @Override
    public byte[] getStrEndian() {
        return new byte[]{(byte) 13, (byte) 10}; // CR LF
    }

    @Override
    public SerialPort getComPort() {
        return this.comPort;
    }

    @Override
    public boolean isKnownCommand() {
        return commands.isKnownCommand(cmdToSend);
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
            if (commands.isKnownCommand(cmdToSend)) {
                answerValues = commands.getCommand(cmdToSend).getResult(lastAnswerBytes);
                if (answerValues != null) {
                    for (int i = 0; i < answerValues.getValues().length; i++) {
                        if (i == 0) {
                            double ans = answerValues.getValues()[i] * 100;
                            ans = Math.round(ans) / 100.0;
                            lastAnswer.append(ans);
                            lastAnswer.append("\t");
                        } else {
                            lastAnswer.append(Math.round(answerValues.getValues()[i]));
                            lastAnswer.append("\t");
                        }
                    }
                } else {
                    lastAnswer.append(new String(lastAnswerBytes));
                    log.info("SPbSTuMcps Cant create answers obj (error in answer)");
                }
            } else {
                lastAnswer.append(new String(lastAnswerBytes));
                log.info("SPbSTuMcps Cant create answers obj (unknown command)");
            }
        } else {
            log.info("SPbSTuMcps empty received");
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

    public AnswerValues getValues() {
        return this.answerValues;
    }

    @Override
    public boolean isASCII() {
        return true;
    }

    @Override
    public DataBitsList getDefaultDataBit() {
        return comParameters.getDataBits();
    }

    @Override
    public ParityList getDefaultParity() {
        return comParameters.getParity();
    }

    @Override
    public BaudRatesList getDefaultBaudRate() {
        return comParameters.getBaudRate();
    }

    @Override
    public StopBitsList getDefaultStopBit() {
        return comParameters.getStopBits();
    }
}