package org.example.device.protGpsTest;


import com.fazecast.jSerialComm.SerialPort;
import lombok.Getter;
import org.apache.log4j.Logger;
import org.example.device.DeviceCommandListClass;
import org.example.device.SomeDevice;
import org.example.device.connectParameters.ComConnectParameters;
import org.example.services.AnswerValues;
import org.example.services.comPort.*;
import org.example.services.connectionPool.AnyPoolService;

//ToDo не работает. Необходима реализация ответов со стороны компьютера
public class GPS_Test implements SomeDevice {
    private static final Logger log = Logger.getLogger(GPS_Test.class);
    @Getter
    private final ComConnectParameters comParameters = new ComConnectParameters(); // Типовые параметры связи для прибора
    private final SerialPort comPort;


    private final DeviceCommandListClass commands;
    private final GpsTestCommandRegistry commandRegistry;

    private volatile byte [] lastAnswerBytes = new byte[1];
    private StringBuilder lastAnswer = new StringBuilder();
    private AnswerValues answerValues = null;
    private int received = 0;

    private String cmdToSend;
    private int expectedBytes = 0;

    private String devIdent = "GPS_Test";

    public GPS_Test(){
        log.info("Создан объект протокола GPS_Test эмуляция");
        this.comPort = null;
        this.commandRegistry = new GpsTestCommandRegistry();
        this.commands = commandRegistry.getCommandList();
    }

    public GPS_Test(SerialPort port){
        log.info("Создан объект протокола GPS_Test");
        this.comPort = port;
        this.commandRegistry = new GpsTestCommandRegistry();
        this.commands = commandRegistry.getCommandList();
        comParameters.setDataBits(DataBitsList.B8);
        comParameters.setParity(ParityList.P_EV);
        comParameters.setBaudRate(BaudRatesList.B9600);
        comParameters.setStopBits(StopBitsList.S1);
        comParameters.setStringEndian(StringEndianList.CR_LF);
        comParameters.setMillisLimit(110);
        comParameters.setRepeatWaitTime(50);
        this.enable();
    }

    @Override
    public DeviceCommandListClass getCommandListClass() {
        return this.commands;
    }

    @Override
    public void setCmdToSend(String str) {
        //Получает количесвто одидаемых байт
        expectedBytes = commands.getExpectedBytes(str); //ToDo распространить на сотальные девайсы
        cmdToSend = str;
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
        //System.out.println("GPS_Test run parse");
        log.info("GPS_Test start parse");
        if(lastAnswerBytes != null && lastAnswerBytes.length > 0) {
                log.info("GPS_Test run parse " + lastAnswerBytes.length);
                answerValues = null; // Сброс предыдущих значений ответов
                lastAnswer.setLength(0); //Очистка строкового представления ответа
                String inputString = new String(lastAnswerBytes);
                inputString = inputString.replace("\r", "");
                inputString = inputString.replace("\n", "");
                inputString = inputString.replace("+", "");
                inputString = inputString.trim();
                if(commands.isKnownCommand(inputString)){// Проверка что в принятой строке есть нужный паттерн
                    log.info("GPS_Test run parse known pattern " + inputString);
                    answerValues = commands.getCommand(inputString).getResult(lastAnswerBytes); //Получение значений в ответе
                } else if(cmdToSend != null && commands.isKnownCommand(cmdToSend)) { //Проверка что была какая-то команда
                    log.info("GPS_Test run parse known command " + cmdToSend);
                    answerValues = commands.getCommand(cmdToSend).getResult(lastAnswerBytes); //Получение значений в ответе по команде
                } else if(commands.isKnownCommand("UNKNOWN")){
                    log.info("GPS_Test run parse unknown command / unknown pattern");
                    answerValues = commands.getCommand("UNKNOWN").getResult(lastAnswerBytes);
                }

                if(answerValues == null) {
                    for (byte lastAnswerByte : lastAnswerBytes) {
                        lastAnswer.append((char) lastAnswerByte);
                    }
                    log.info("GPS_Test Cant create answers obj (error in answer) " + lastAnswer);
                }else{
                    if(answerValues.getDirection() == -55) { // Магическое число, обозначающее необходимость ответа
                        answerValues = new AnswerValues(1);
                        answerValues.setDirection(-55);
                        answerValues.addValue(-55, "NeedetString");
                    }
                    for (int i = 0; i < answerValues.getValues().length; i++) {
                        lastAnswer.append(answerValues.getValues()[i]);
                        lastAnswer.append(" ");
                        lastAnswer.append(answerValues.getUnits()[i]);
                        lastAnswer.append("  ");
                    }
                    log.info("GPS_Test OK: " + lastAnswer);
                }
            }else {
                log.info("GPS_Test empty received");
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
