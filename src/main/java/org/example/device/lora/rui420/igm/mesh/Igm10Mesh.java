package org.example.device.lora.rui420.igm.mesh;

import com.fazecast.jSerialComm.SerialPort;
import lombok.Getter;
import org.apache.log4j.Logger;
import org.example.device.Answerable;
import org.example.device.DeviceCommandListClass;
import org.example.device.ProtocolComPort;
import org.example.device.SomeDevice;
import org.example.device.connectParameters.ComConnectParameters;
import org.example.device.lora.LoraMeshMessage;
import org.example.device.lora.rui420.Rui420Message;
import org.example.device.lora.rui420.igm.IgmTenMessage;
import org.example.services.AnswerValues;
import org.example.services.comPort.*;
import org.example.utilites.MyUtilities;

public class Igm10Mesh implements SomeDevice, ProtocolComPort, Answerable {
    private static final Logger log = Logger.getLogger(Igm10Mesh.class);
    @Getter
    private final ComConnectParameters comParameters = new ComConnectParameters(); // Типовые параметры связи для прибора
    private final SerialPort comPort;
    @Getter
    private final DataBitsList defaultDataBit = DataBitsList.B8;
    @Getter
    private final ParityList defaultParity = ParityList.P_EV;
    @Getter
    private final BaudRatesList defaultBaudRate = BaudRatesList.B9600;
    @Getter
    private final StopBitsList defaultStopBit = StopBitsList.S1;

    private final DeviceCommandListClass commands;
    private final Igm10MeshCommandRegistry commandRegistry;

    private volatile byte[] lastAnswerBytes = new byte[1];
    private StringBuilder lastAnswer = new StringBuilder();
    private AnswerValues answerValues = null;
    private int received = 0;

    private String cmdToSend;
    private int expectedBytes = 0;

    private String devIdent = "IGM10_MESH";

    public Igm10Mesh() {
        log.info("Создан объект протокола Igm10Mesh эмуляция");
        this.comPort = null;
        this.commandRegistry = new Igm10MeshCommandRegistry();
        this.commands = commandRegistry.getCommandList();
    }

    public Igm10Mesh(SerialPort port) {
        log.info("Создан объект протокола Igm10Mesh");
        this.comPort = port;
        this.commandRegistry = new Igm10MeshCommandRegistry();
        this.commands = commandRegistry.getCommandList();
        comParameters.setDataBits(defaultDataBit);
        comParameters.setParity(defaultParity);
        comParameters.setBaudRate(defaultBaudRate);
        comParameters.setStopBits(defaultStopBit);
        comParameters.setStringEndian(StringEndianList.CR);
        comParameters.setMillisLimit(300);
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
            //Получает количество ожидаемых байт
            expectedBytes = commands.getExpectedBytes(str); //ToDo распространить на остальные девайсы
            cmdToSend = str;
        }
    }

    @Override
    public int getExpectedBytes() {
        return expectedBytes;
    }

    @Override
    public long getMillisPrev() {
        return SomeDevice.super.getMillisPrev();
    }

    @Override
    public boolean isASCII() {
        return SomeDevice.super.isASCII();
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
        //System.out.println("Igm10Mesh run parse");
        if (lastAnswerBytes != null && lastAnswerBytes.length > 0) {
            Rui420Message.parse(lastAnswerBytes)
                    .flatMap(rui -> {
                        log.info(rui.getEventType());
                        log.info(rui.getDbi());
                        log.info(rui.getSnr());
                        log.info("Payload: " + MyUtilities.byteArrayToString(rui.getPayload()));
                        return LoraMeshMessage.parse(rui.getPayload());
                    })
                    .flatMap(lora -> {
                        log.info("LoRa parsed: Type=" + lora.getType() + ", From=" + lora.getFrom());
                        return IgmTenMessage.parse(lora.getData());
                    })
                    .ifPresentOrElse(igm -> {
                        log.info("IGM parsed: Temp=" + igm.getTemperature() + "°C, GasType=" + igm.getGasType() + ", Conc=" + igm.getConcentration());
                        // Здесь можно установить answerValues на основе полей igm
                        // Например:
                        // answerValues = new AnswerValues(3);
                        // answerValues.addValue(igm.getTemperature(), "°C");
                        // answerValues.addValue(igm.getGasType(), "");
                        // answerValues.addValue(igm.getConcentration(), getUnitSymbol(igm.getUnit()));
                        // Затем продолжить с lastAnswer.append как в оригинале
                    }, () -> {
                        log.warn("Parsing chain failed at some stage");
                        // Альтернативная обработка: установить lastAnswer как raw string
                        lastAnswer.setLength(0);
                        for (byte b : lastAnswerBytes) {
                            lastAnswer.append((char) b);
                        }
                    });

            // Продолжение оригинальной логики, если цепочка удалась или нет
            lastAnswer.setLength(0); //Очистка строкового представления ответа
            if (commands.isKnownCommand(cmdToSend)) { //Проверка наличия команды в реестре команд
                answerValues = commands.getCommand(cmdToSend).getResult(lastAnswerBytes); //Получение значений в ответе
                if (answerValues != null) {
                    for (int i = 0; i < answerValues.getValues().length; i++) {
                        lastAnswer.append(answerValues.getValues()[i]);
                        lastAnswer.append(" ");
                        lastAnswer.append(answerValues.getUnits()[i]);
                        lastAnswer.append("  ");
                    }
                } else {
                    for (byte lastAnswerByte : lastAnswerBytes) {
                        lastAnswer.append((char) lastAnswerByte);
                    }
                    log.info("Igm10Mesh Cant create answers obj (error in answer)");
                }
            } else {
                for (byte lastAnswerByte : lastAnswerBytes) {
                    lastAnswer.append((char) lastAnswerByte);
                }
                log.info("Igm10Mesh Cant create answers obj (unknown command)");
            }
        } else {
            log.info("Igm10Mesh empty received");
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
    public byte[] generateAnswer(byte[] message) {
        StringBuilder sb = new StringBuilder();
        for (byte b : message) {
            sb.append((char) b);
        }

        if (sb.toString().equals("Hello world")) {
            return new byte[] { 0x48, 0x65, 0x6c, 0x6c, 0x6f, 0x20, 0x77, 0x6f, 0x72, 0x6c, 0x64 };
        }else if (sb.toString().trim().equalsIgnoreCase("+EVT:RXP2P RECEIVE TIMEOUT")) {
            //AT+PRECV=9999\cr
            return new byte[] { 0x41, 0x54, 0x2b, 0x50, 0x52, 0x45, 0x43, 0x56, 0x3d, 0x39, 0x39, 0x39, 0x39, 0x0d };
        }
        //empty answer
        return new byte[0];
    }
}