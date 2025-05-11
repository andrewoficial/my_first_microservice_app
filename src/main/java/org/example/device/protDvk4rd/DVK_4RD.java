package org.example.device.protDvk4rd;

import com.fazecast.jSerialComm.SerialPort;
import lombok.Getter;
import org.apache.log4j.Logger;
import org.example.device.DeviceCommandListClass;
import org.example.device.SomeDevice;
import org.example.device.connectParameters.ComConnectParameters;
import org.example.device.protArdBadVlt.ARD_BAD_VLT;
import org.example.services.AnswerValues;
import org.example.services.comPort.*;


public class DVK_4RD  implements SomeDevice {
    private static final Logger log = Logger.getLogger(DVK_4RD.class);
    @Getter
    private final ComConnectParameters comParameters = new ComConnectParameters(); // Типовые параметры связи для прибора
    private final SerialPort comPort;

    private final DeviceCommandListClass commands;
    private final Dvk4rdCommandRegistry commandRegistry;

    private volatile byte [] lastAnswerBytes = new byte[1];
    private StringBuilder lastAnswer = new StringBuilder();
    private AnswerValues answerValues = null;
    private int received = 0;

    private String cmdToSend;
    private int expectedBytes = 0;

    private String devIdent = "DVK_4RD";

    public DVK_4RD(){
        log.info("Создан объект протокола DVK_4RD эмуляция");
        this.comPort = null;
        this.commandRegistry = new Dvk4rdCommandRegistry();
        this.commands = commandRegistry.getCommandList();
    }

    public DVK_4RD(SerialPort port){
        log.info("Создан объект протокола DVK_4RD");
        this.comPort = port;
        this.commandRegistry = new Dvk4rdCommandRegistry();
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
        //System.out.println("DVK_4RD run parse");
        if(lastAnswerBytes != null && lastAnswerBytes.length > 0) {
            lastAnswer.setLength(0); //Очистка строкового представления ответа
            if (commands.isKnownCommand(cmdToSend)) { //Проверка наличия команды в реестре команд
                answerValues = commands.getCommand(cmdToSend).getResult(lastAnswerBytes); //Получение значений в ответе
                if(answerValues != null){
                    for (int i = 0; i < answerValues.getValues().length; i++) { //Преобразование в строковое представление
                        if(i == 0){
                            double ans = answerValues.getValues()[i] * 100;
                            ans = Math.round(ans) / 100.0;
                            lastAnswer.append(ans);
                            lastAnswer.append("\t");
                        }else {
                            lastAnswer.append(Math.round(answerValues.getValues()[i]));
                            lastAnswer.append("\t");
                        }
                    }
                }else{
                    for (byte lastAnswerByte : lastAnswerBytes) {
                        lastAnswer.append((char) lastAnswerByte);
                    }
                    log.info("DVK_4RD Cant create answers obj (error in answer)");
                }
            }else {
                for (byte lastAnswerByte : lastAnswerBytes) {
                    lastAnswer.append((char) lastAnswerByte);
                }
                log.info("DVK_4RD Cant create answers obj (unknown command)");
            }
        }else{
            log.info("DVK_4RD empty received");
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
