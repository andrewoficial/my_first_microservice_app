package org.example.device.protArdBadVlt;

import com.fazecast.jSerialComm.SerialPort;
import lombok.Getter;
import lombok.Setter;
import org.apache.log4j.Logger;
import org.example.device.DeviceCommandListClass;
import org.example.device.SingleCommand;
import org.example.device.SomeDevice;
import org.example.device.connectParameters.ComConnectParameters;
import org.example.device.protDvk4rd.Dvk4rdCommandRegistry;
import org.example.services.AnswerValues;
import org.example.services.comPort.*;

import static org.example.utilites.MyUtilities.*;

public class ARD_BAD_VLT implements SomeDevice {
    private static final Logger log = Logger.getLogger(ARD_BAD_VLT.class);
    @Getter
    private final ComConnectParameters comParameters = new ComConnectParameters(); // Типовые параметры связи для прибора
    private final SerialPort comPort;

    private final DeviceCommandListClass commands;
    private final ArdBadVltCommandRegistry commandRegistry;

    private volatile byte [] lastAnswerBytes = new byte[1];
    private StringBuilder lastAnswer = new StringBuilder();
    private AnswerValues answerValues = null;
    private int received = 0;

    private String cmdToSend;
    private int expectedBytes = 0;

    private String devIdent = "ARD_BAD_VLT";

    public ARD_BAD_VLT(){
        log.info("Создан объект протокола ARD_BAD_VLT эмуляция");
        this.comPort = null;
        this.commandRegistry = new ArdBadVltCommandRegistry();
        this.commands = commandRegistry.getCommandList();
    }

    public ARD_BAD_VLT(SerialPort port){
        log.info("Создан объект протокола ARD_BAD_VLT");
        this.comPort = port;
        this.commandRegistry = new ArdBadVltCommandRegistry();
        this.commands = commandRegistry.getCommandList();
        comParameters.setDataBits(DataBitsList.B8);
        comParameters.setParity(ParityList.P_EV);
        comParameters.setBaudRate(BaudRatesList.B9600);
        comParameters.setStopBits(StopBitsList.S1);
        comParameters.setStringEndian(StringEndianList.CR);
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
        //ToDo сделать в остальных
        if(lastAnswerBytes != null && lastAnswerBytes.length > 0) {
            lastAnswer.setLength(0); //Очистка строкового представления ответа
            if (commands.isKnownCommand(cmdToSend)) { //Проверка наличия команды в реестре команд
                answerValues = commands.getCommand(cmdToSend).getResult(lastAnswerBytes); //Получение значений в ответе

                if(answerValues != null){
                    for (int i = 0; i < answerValues.getValues().length; i++) {
                        lastAnswer.append(answerValues.getValues()[i]);
                        lastAnswer.append(" ");
                    }
                }else {
                    for (int i = 0; i < lastAnswerBytes.length; i++) {
                        lastAnswer.append( (char) lastAnswerBytes[i]);
                    }
                }
            }else {

                for (int i = 0; i < lastAnswerBytes.length; i++) {
                    lastAnswer.append( (char) lastAnswerBytes[i]);
                }
                log.info("ARD_BAD_VLT Cant create answers obj (error in answer)");
            }
        }else{
            log.info("ARD_BAD_VLT empty received");
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
