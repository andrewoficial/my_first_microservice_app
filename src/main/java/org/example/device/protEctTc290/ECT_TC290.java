package org.example.device.protEctTc290;


import com.fazecast.jSerialComm.SerialPort;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.example.device.DeviceCommandListClass;
import org.example.device.ProtocolComPort;
import org.example.device.SlaveAddressable;
import org.example.device.SomeDevice;
import org.example.device.TemplatedAscii;
import org.example.device.connectParameters.ComConnectParameters;
import org.example.services.AnswerValues;
import org.example.services.transport.serial.*;

@Slf4j
public class ECT_TC290 implements SomeDevice, ProtocolComPort, TemplatedAscii, SlaveAddressable {
    @Getter
    private final ComConnectParameters comParameters = new ComConnectParameters(); // Типовые параметры связи для прибора
    private final SerialPort comPort;
    @Getter
    private final DataBitsList defaultDataBit = DataBitsList.B8;
    @Getter
    private final ParityList defaultParity= ParityList.P_EV;
    @Getter
    private final BaudRatesList defaultBaudRate = BaudRatesList.B115200;
    @Getter
    private final StopBitsList defaultStopBit = StopBitsList.S1;

    private final DeviceCommandListClass commands;
    private final EctTc290CommandRegistry commandRegistry;

    private volatile byte [] lastAnswerBytes = new byte[1];
    private StringBuilder lastAnswer = new StringBuilder();
    private AnswerValues answerValues = null;
    private int received = 0;

    private String cmdToSend;
    private int expectedBytes = 0;
    private int slaveAddress = 1; // Сетевой адрес (адресный префикс вида "@01")

    private String devIdent = "ECT_TC290";

    public ECT_TC290(){
        log.info("Создан объект протокола ECT_TC290 эмуляция");
        this.comPort = null;
        this.commandRegistry = new EctTc290CommandRegistry();
        this.commands = commandRegistry.getCommandList();
    }

    public ECT_TC290(SerialPort port){
        log.info("Создан объект протокола ECT_TC290");
        this.comPort = port;
        this.commandRegistry = new EctTc290CommandRegistry();
        this.commands = commandRegistry.getCommandList();
        comParameters.setDataBits(defaultDataBit);
        comParameters.setParity(defaultParity);
        comParameters.setBaudRate(defaultBaudRate);
        comParameters.setStopBits(defaultStopBit);
        comParameters.setStringEndian(StringEndianList.CR);
        comParameters.setMillisLimit(150);
        comParameters.setRepeatWaitTime(100);
        this.enable();
    }

    @Override
    public DeviceCommandListClass getCommandListClass() {
        return this.commands;
    }

    @Override
    public void setCmdToSend(String str) {
        if(str == null || str.isEmpty()){
            expectedBytes = 500;
            cmdToSend = null;
        }else{
            //Адресный префикс — "@" + 1..2 цифры. Нормализуем к виду "@NN",
            //чтобы "1CRDG? 1" или "@1CRDG? 1" превратились в "@01CRDG? 1".
            String normalized = normalizeAddressPrefix(str);
            //Получает количесвто одидаемых байт
            expectedBytes = commands.getExpectedBytes(normalized); //ToDo распространить на сотальные девайсы
            cmdToSend = normalized;
        }
    }

    @Override
    public void setSlaveAddress(int address) {
        this.slaveAddress = address;
    }

    public int getSlaveAddress() {
        return this.slaveAddress;
    }

    /**
     * Приводит ведущий адрес к виду {@code @NN}: {@code 1CRDG? 1 -> @01CRDG? 1},
     * {@code @5CRDG? 1 -> @05CRDG? 1}. Команда без адреса возвращается как есть.
     */
    private static String normalizeAddressPrefix(String command) {
        boolean hasAt = command.charAt(0) == '@';
        int start = hasAt ? 1 : 0;
        int digits = 0;
        while (start + digits < command.length() && Character.isDigit(command.charAt(start + digits))) {
            digits++;
        }
        if (digits == 0) {
            return command;
        }
        int address = Integer.parseInt(command.substring(start, start + digits));
        String rest = command.substring(start + digits);
        return String.format("@%02d%s", address, rest);
    }


    @Override
    public int getExpectedBytes(){
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
        return  commands.isKnownCommand(cmdToSend);
    }

    @Override
    public int getReceivedCounter() {
        return received;
    }

    @Override
    public void setReceivedCounter(int cnt) {
        this.received = cnt;
    }

    public void setReceived(String answer){
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
        //System.out.println("ECT_TC290 run parse");
        if(lastAnswerBytes != null && lastAnswerBytes.length > 0) {
            lastAnswer.setLength(0); //Очистка строкового представления ответа
            if (commands.isKnownCommand(cmdToSend)) { //Проверка наличия команды в реестре команд
                answerValues = commands.getCommand(cmdToSend).getResult(lastAnswerBytes); //Получение значений в ответе
                if(answerValues != null){
                    for (int i = 0; i < answerValues.getValues().length; i++) {
                        lastAnswer.append(answerValues.getValues()[i]);
                        lastAnswer.append(" ");
                        lastAnswer.append(answerValues.getUnits()[i]);
                        lastAnswer.append("  ");
                    }
                    //Добавление вывода в HEX находится тут
                    //lastAnswer.append("\n");
                    //lastAnswer.append(Arrays.toString(lastAnswerBytes));
                }else{
                    for (byte lastAnswerByte : lastAnswerBytes) {
                        lastAnswer.append((char) lastAnswerByte);
                    }
                    //Добавление вывода в HEX находится тут
                    //lastAnswer.append("\n");
                    //lastAnswer.append(Arrays.toString(lastAnswerBytes));
                    log.info("ECT_TC290 Cant create answers obj (error in answer)");
                }
            }else {
                for (byte lastAnswerByte : lastAnswerBytes) {
                    lastAnswer.append((char) lastAnswerByte);
                }
                //Добавление вывода в HEX находится тут
                //lastAnswer.append("\n");
                //lastAnswer.append(Arrays.toString(lastAnswerBytes));
                log.info("ECT_TC290 Cant create answers obj (unknown command)");
            }
        }else{
            log.info("ECT_TC290 empty received");
        }
    }


    public String getAnswer(){
        if(hasAnswer()) {
            received = 0;
            lastAnswerBytes = null;
            return lastAnswer.toString();
        }else {
            return null;
        }
    }

    public boolean hasAnswer(){
        return lastAnswerBytes != null && lastAnswerBytes.length > 0;
    }

    @Override
    public boolean hasValue(){
        return answerValues != null;
    }

    public AnswerValues getValues(){
        return this.answerValues;
    }
}
