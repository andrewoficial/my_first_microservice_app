package org.example.device.protEspKantserBleEmu;

import com.fazecast.jSerialComm.SerialPort;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.example.device.DeviceCommandListClass;
import org.example.device.ProtocolComPort;
import org.example.device.SomeDevice;
import org.example.device.TemplatedAscii;
import org.example.device.command.SingleCommand;
import org.example.device.connectParameters.ComConnectParameters;
import org.example.services.AnswerValues;
import org.example.services.transport.serial.*;

/**
 * ESP32 Kantser BLE simulator.
 * Link params: 115200 8N1, CR.
 */
@Slf4j
public class ESP_KANTSER_BLE_EMU implements SomeDevice, ProtocolComPort, TemplatedAscii {
    @Getter
    private final ComConnectParameters comParameters = new ComConnectParameters();
    private final SerialPort comPort;

    @Getter
    private final DataBitsList defaultDataBit = DataBitsList.B8;
    @Getter
    private final ParityList defaultParity = ParityList.P_NO;
    @Getter
    private final BaudRatesList defaultBaudRate = BaudRatesList.B115200;
    @Getter
    private final StopBitsList defaultStopBit = StopBitsList.S1;

    private final DeviceCommandListClass commands;
    private final EspKantserBleEmuCommandRegistry commandRegistry;

    private volatile byte[] lastAnswerBytes = new byte[1];
    private StringBuilder lastAnswer = new StringBuilder();
    private AnswerValues answerValues = null;
    private int received = 0;

    private String cmdToSend;
    private int expectedBytes = 0;

    private String devIdent = "ESP_KANTSER_BLE_EMU";

    public ESP_KANTSER_BLE_EMU() {
        log.info("Создан объект протокола ESP_KANTSER_BLE_EMU эмуляция");
        this.comPort = null;
        this.commandRegistry = new EspKantserBleEmuCommandRegistry();
        this.commands = commandRegistry.getCommandList();
    }

    public ESP_KANTSER_BLE_EMU(SerialPort port) {
        log.info("Создан объект протокола ESP_KANTSER_BLE_EMU");
        this.comPort = port;
        this.commandRegistry = new EspKantserBleEmuCommandRegistry();
        this.commands = commandRegistry.getCommandList();
        comParameters.setDataBits(DataBitsList.B8);
        comParameters.setParity(ParityList.P_NO);
        comParameters.setBaudRate(BaudRatesList.B115200);
        comParameters.setStopBits(StopBitsList.S1);
        comParameters.setStringEndian(StringEndianList.CR);
        comParameters.setMillisLimit(450);
        comParameters.setRepeatWaitTime(100);
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
            cmdToSend = str;
            SingleCommand resolved = resolveCommand(str);
            expectedBytes = resolved != null ? resolved.getExpectedBytes() : 500;
        }
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
            SingleCommand command = resolveCommand(cmdToSend);
            if (command != null) {
                answerValues = command.getResult(lastAnswerBytes);
                if (answerValues != null) {
                    for (int i = 0; i < answerValues.getValues().length; i++) {
                        lastAnswer.append(String.valueOf(answerValues.getValues()[i]).replace(".", ","));
                        lastAnswer.append(" ");
                        lastAnswer.append(answerValues.getUnits()[i]);
                        lastAnswer.append("  ");
                    }
                } else {
                    for (byte lastAnswerByte : lastAnswerBytes) {
                        lastAnswer.append((char) lastAnswerByte);
                    }
                    log.warn("Failed to parse answer for command '{}'", cmdToSend);
                }
            } else {
                for (byte lastAnswerByte : lastAnswerBytes) {
                    lastAnswer.append((char) lastAnswerByte);
                }
                log.warn("Unknown command '{}'", cmdToSend);
            }
        } else {
            log.debug("Empty received");
        }
    }

    /**
     * Resolves command by exact name, then by first token (e.g. {@code SCH1 0100} → {@code SCH1}).
     * Firmware also accepts a query name without trailing '?' (e.g. {@code VE} → {@code VE?}).
     */
    private SingleCommand resolveCommand(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        SingleCommand cmd = commands.getCommand(name);
        if (cmd != null) {
            return cmd;
        }
        int sp = name.indexOf(' ');
        if (sp > 0) {
            String token = name.substring(0, sp);
            cmd = commands.getCommand(token);
            if (cmd != null) {
                return cmd;
            }
            if (!token.endsWith("?")) {
                cmd = commands.getCommand(token + "?");
                if (cmd != null) {
                    return cmd;
                }
            }
        }
        if (!name.endsWith("?")) {
            cmd = commands.getCommand(name + "?");
            if (cmd != null) {
                return cmd;
            }
        }
        return null;
    }

    @Override
    public boolean isKnownCommand() {
        return resolveCommand(cmdToSend) != null;
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
}
