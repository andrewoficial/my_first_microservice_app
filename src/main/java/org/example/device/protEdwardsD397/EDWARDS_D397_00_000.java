package org.example.device.protEdwardsD397;


import com.fazecast.jSerialComm.SerialPort;
import lombok.Getter;
import org.apache.log4j.Logger;
import org.example.device.DeviceCommandListClass;
import org.example.device.SomeDevice;
import org.example.device.connectParameters.ComConnectParameters;
import org.example.device.protArdTerm.ArdTermCommandRegistry;
import org.example.device.protEctTc290.EctTc290CommandRegistry;
import org.example.services.AnswerValues;
import org.example.services.comPort.*;


public class EDWARDS_D397_00_000 implements SomeDevice {
    private static final Logger log = Logger.getLogger(EDWARDS_D397_00_000.class);
    @Getter
    private final ComConnectParameters comParameters = new ComConnectParameters(); // Типовые параметры связи для прибора
    private final SerialPort comPort;

    private final DeviceCommandListClass commands;
    private final EdwardsD397CommandRegistry commandRegistry;

    private volatile byte [] lastAnswerBytes = new byte[1];
    private StringBuilder lastAnswer = new StringBuilder();
    private AnswerValues answerValues = null;
    private int received = 0;

    private String cmdToSend;
    private int expectedBytes = 0;

    private String devIdent = "EDWARDS_D397_00_000";

    public EDWARDS_D397_00_000(){
        log.info("Создан объект протокола EDWARDS_D397_00_000 эмуляция");
        this.comPort = null;
        this.commandRegistry = new EdwardsD397CommandRegistry();
        this.commands = commandRegistry.getCommandList();
    }

    public EDWARDS_D397_00_000(SerialPort port){
        log.info("Создан объект протокола EDWARDS_D397_00_000");
        this.comPort = port;
        this.commandRegistry = new EdwardsD397CommandRegistry();
        this.commands = commandRegistry.getCommandList();
        comParameters.setDataBits(DataBitsList.B8);
        comParameters.setParity(ParityList.P_EV);
        comParameters.setBaudRate(BaudRatesList.B9600);
        comParameters.setStopBits(StopBitsList.S1);
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
        lastAnswerBytes = sb;
    }

    public boolean enable() {
        return true;
    }

    @Override
    public void parseData() {
        //System.out.println("EDWARDS_D397_00_000 run parse");
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
                }else{
                    for (byte lastAnswerByte : lastAnswerBytes) {
                        lastAnswer.append((char) lastAnswerByte);
                    }
                    log.info("EDWARDS_D397_00_000 Cant create answers obj (error in answer)");
                }
            }else {
                for (byte lastAnswerByte : lastAnswerBytes) {
                    lastAnswer.append((char) lastAnswerByte);
                }
                log.info("EDWARDS_D397_00_000 Cant create answers obj (unknown command)");
            }
        }else{
            log.info("EDWARDS_D397_00_000 empty received");
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
