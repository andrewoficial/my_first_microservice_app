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


public class GPS_Test implements SomeDevice {
    private static final Logger log = Logger.getLogger(GPS_Test.class);
    @Getter
    private final ComConnectParameters comParameters = new ComConnectParameters(); // Типовые параметры связи для прибора
    private final SerialPort comPort;


    private final DeviceCommandListClass commands;
    private final GpsTestCommandRegistry commandRegistry;

    private volatile byte [] lastAnswerBytes = new byte[1];
    private final StringBuilder lastAnswer = new StringBuilder();
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
        comParameters.setMillisLimit(250);
        comParameters.setRepeatWaitTime(70);
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
            //Получает количесвто одидаемых байт
            expectedBytes = commands.getExpectedBytes(str); //ToDo распространить на сотальные девайсы
            cmdToSend = str;
        }

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
        if(sb == null || sb.length < 1){
            log.error("Попытка задать в протокол пустой массив ответа");
        }
        lastAnswerBytes = sb;
    }

    public boolean enable() {
        return true;
    }

    @Override
    public void parseData() {
        //log.info("GPS_Test start parse");
        if(lastAnswerBytes != null && lastAnswerBytes.length > 0) {
                //log.info("GPS_Test run parse " + lastAnswerBytes.length);
                answerValues = null; // Сброс предыдущих значений ответов
                lastAnswer.setLength(0); //Очистка строкового представления ответа
                String inputString = new String(lastAnswerBytes);
                //log.info("Run search pattern in string " + inputString);
                //Костыль
                boolean skipped = false;
                if( ! inputString.isEmpty()){
                    if(inputString.indexOf("RXP2P") > -1){
                        //log.info("Через костыль нашли паттерн для RAK");
                        answerValues = commands.getCommand("CEIVE TIME").getResult(lastAnswerBytes);
                    }else if(inputString.indexOf("BYTE:") > -1){
                        //log.info("Через костыль нашли паттерн для EBYTE");
                        answerValues = commands.getCommand("BYTE:").getResult(lastAnswerBytes);
                    }else if(inputString.indexOf("AK:OK") > -1 && inputString.indexOf("AK:OK") < 5){//Сообщение началось с "RAK:OK♥"
                        //return;
                        lastAnswer.setLength(0);
                        lastAnswer.append(inputString.trim());
                        log.info("GPS_Test [OK] пропущено: " + lastAnswer);
                        answerValues = null;
                        lastAnswer.setLength(0);
                        return;
                        //skipped = true;
                    }
                }


                if(answerValues == null) {
                    lastAnswer.setLength(0);
                    lastAnswer.append(clearMessage(inputString));

                    if(skipped){

                    }else{
                        log.info("GPS_Test Cant create answers obj (error in answer) " + lastAnswer);
                    }

                }else{
                    if(answerValues.getDirection() == -55) { // Магическое число, обозначающее необходимость ответа
                        answerValues = new AnswerValues(1);
                        answerValues.setDirection(-55);
                        answerValues.addValue(-55, "NeedetString");
                    }
                    int countLimit =Math.min(answerValues.getValues().length, answerValues.getUnits().length);
                    for (int i = 0; i < countLimit; i++) {
                        lastAnswer.append(answerValues.getValues()[i]);
                        lastAnswer.append(" ");
                        lastAnswer.append(answerValues.getUnits()[i]);
                        lastAnswer.append("  ");
                    }
                    log.info("GPS_Test OK: " + lastAnswer);
                }
            }else {
                log.error("GPS_Test попытка обработки пустого ответа");
            }
    }

    public String clearMessage(String message){
        if(message == null){
            return "";
        }
        String forReturn = message.replace("\r", "");
        forReturn = forReturn.replace("\n", "");
        forReturn = forReturn.replace("+", "");
        forReturn = forReturn.trim();
        return forReturn;
    }
    public String getAnswer(){

        if(hasAnswer()) {
            String forReturn = lastAnswer.toString();
            received = 0;
            lastAnswerBytes = new byte[0];
            lastAnswer.setLength(0);
            return forReturn;
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
