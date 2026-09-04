package org.example.device.protBoto;

import com.fazecast.jSerialComm.SerialPort;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.example.device.DeviceCommandListClass;
import org.example.device.ProtocolComPort;
import org.example.device.SomeDevice;
import org.example.device.command.SingleCommand;
import org.example.device.connectParameters.ComConnectParameters;
import org.example.services.AnswerValues;
import org.example.services.transport.serial.*;

/**
 * Термокамера BOTO 1000 / China Modbus (Modbus RTU, RS-485/RS-232, 9600 8N1).
 * Протокол: {@code boto.md}. Регистры: 12 (T), 100 (уставка), 105 (вкл/выкл).
 */
@Slf4j
public class BOTO1000_DEVICE implements SomeDevice, ProtocolComPort {
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
    private final Boto1000CommandRegistry commandRegistry;

    private volatile byte[] lastAnswerBytes = new byte[1];
    private final StringBuilder lastAnswer = new StringBuilder();
    private AnswerValues answerValues = null;
    private int received = 0;
    private String cmdToSend;
    private int expectedBytes = 0;

    public BOTO1000_DEVICE() {
        log.info("Создан объект протокола BOTO1000_DEVICE эмуляция");
        this.comPort = null;
        this.commandRegistry = new Boto1000CommandRegistry();
        this.commands = commandRegistry.getCommandList();
    }

    public BOTO1000_DEVICE(SerialPort port) {
        log.info("Создан объект протокола BOTO1000_DEVICE");
        this.comPort = port;
        this.commandRegistry = new Boto1000CommandRegistry();
        this.commands = commandRegistry.getCommandList();
        comParameters.setDataBits(DataBitsList.B8);
        comParameters.setParity(ParityList.P_NO);
        comParameters.setBaudRate(BaudRatesList.B9600);
        comParameters.setStopBits(StopBitsList.S1);
        comParameters.setStringEndian(StringEndianList.CR);
        comParameters.setMillisLimit(500);
        comParameters.setRepeatWaitTime(200);
        this.enable();
    }

    @Override
    public DeviceCommandListClass getCommandListClass() { return this.commands; }

    @Override
    public void setCmdToSend(String str) {
        if (str == null || str.isEmpty()) { expectedBytes = 500; cmdToSend = null; }
        else { expectedBytes = commands.getExpectedBytes(str); cmdToSend = str; }
    }

    @Override public int getExpectedBytes() { return expectedBytes; }
    @Override public byte[] getStrEndian() { return this.comParameters.getStringEndian().getBytes(); }
    @Override public SerialPort getComPort() { return this.comPort; }
    @Override public int getReceivedCounter() { return received; }
    @Override public void setReceivedCounter(int cnt) { this.received = cnt; }
    @Override public long getMillisLimit() { return comParameters.getMillisLimit(); }
    @Override public int getMillisReadLimit() { return comParameters.getMillisReadLimit(); }
    @Override public int getMillisWriteLimit() { return comParameters.getMillisWriteLimit(); }
    @Override public long getRepeatWaitTime() { return comParameters.getRepeatWaitTime(); }
    @Override public void setLastAnswer(byte[] sb) { lastAnswerBytes = sb; }

    public boolean enable() { return true; }

    @Override
    public void parseData() {
        if (lastAnswerBytes != null && lastAnswerBytes.length > 0) {
            lastAnswer.setLength(0);
            SingleCommand cmd = commands.getCommand(cmdToSend);
            if (cmd != null) {
                answerValues = cmd.getResult(lastAnswerBytes);
                if (answerValues != null) {
                    for (int i = 0; i < answerValues.getValues().length; i++) {
                        lastAnswer.append(answerValues.getValues()[i]).append(" ");
                    }
                } else {
                    for (byte b : lastAnswerBytes) lastAnswer.append((char) b);
                    log.warn("BOTO1000: parse failed for '{}'", cmdToSend);
                }
            } else {
                for (byte b : lastAnswerBytes) lastAnswer.append((char) b);
                log.warn("BOTO1000: unknown command '{}'", cmdToSend);
            }
        }
    }

    @Override public boolean isKnownCommand() { return cmdToSend != null && commands.getCommand(cmdToSend) != null; }

    public String getAnswer() {
        if (hasAnswer()) { received = 0; lastAnswerBytes = null; return lastAnswer.toString(); }
        return null;
    }
    public boolean hasAnswer() { return lastAnswerBytes != null && lastAnswerBytes.length > 0; }
    @Override public boolean hasValue() { return answerValues != null; }
    public AnswerValues getValues() { return this.answerValues; }
}
