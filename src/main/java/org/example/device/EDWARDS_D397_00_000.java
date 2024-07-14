package org.example.device;

import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import com.fazecast.jSerialComm.SerialPort;
import lombok.Setter;
import org.apache.log4j.Logger;
import org.example.services.AnswerValues;
import org.example.utilites.CommandListClass;
import org.example.utilites.SingleCommand;


public class EDWARDS_D397_00_000  implements SomeDevice  {
    private volatile boolean bisy = false;
    private static final Logger log = Logger.getLogger(IGM_10.class);
    private final SerialPort comPort;
    private byte [ ] lastAnswerBytes = new byte[1];
    private StringBuilder lastAnswer = new StringBuilder();
    private StringBuilder emulatedAnswer = new StringBuilder();
    private final boolean knownCommand = false;
    private volatile boolean hasAnswer = false;
    private  volatile boolean hasValue = false;
    @Setter
    private byte [] strEndian = {13};//CR
    private int received = 0;
    private final long millisLimit = 500000;
    private final long repeatGetAnswerTimeDelay = 2;
    private final int buffClearTimeLimit = 5;
    private final int repetCounterLimit = 300;
    private final long millisPrev = System.currentTimeMillis();
    private final Charset charset = Charset.forName("Cp1251");
    private final CharsetDecoder decoder = charset.newDecoder();
    private final CharBuffer charBuffer = CharBuffer.allocate(512);  // Предполагаемый максимальный размер
    private AnswerValues answerValues = new AnswerValues(0);
    String cmdToSend;


    public EDWARDS_D397_00_000(SerialPort port){
        log.info("Создан объект протокола ERSTEVAK_MTP4D");
        this.comPort = port;
        this.enable();
    }

    public EDWARDS_D397_00_000(){
        System.out.println("Создан объект протокола EDWARDS_D397_00_000 эмуляция");
        this.comPort = null;
    }

    @Override
    public void setCmdToSend(String str) {
        cmdToSend = str;
    }

    @Override
    public Integer getTabForAnswer() {
        return null;
    }

    @Override
    public boolean isBisy(){
        return bisy;
    }

    @Override
    public void setBisy(boolean bisy){
        this.bisy = bisy;
    }
    @Override
    public byte[] getStrEndian() {
        return this.strEndian;
    }

    @Override
    public SerialPort getComPort() {
        return this.comPort;
    }

    @Override
    public boolean isKnownCommand() {
        return knownCommand;
    }

    @Override
    public int getReceivedCounter() {
        return received;
    }

    @Override
    public void setReceivedCounter(int cnt) {
        this.received = cnt;
    }

    @Override
    public int getRepetCounterLimit() {
        return repetCounterLimit;
    }

    public void setReceived(String answer){
        lastAnswerBytes = answer.getBytes();
        this.received = lastAnswerBytes.length;
        //this.parseData();
    }
    @Override
    public long getMillisPrev() {
        return millisPrev;
    }

    @Override
    public long getMillisLimit() {
        return millisLimit;
    }

    @Override
    public long getRepeatGetAnswerTimeDelay() {
        return repeatGetAnswerTimeDelay;
    }

    @Override
    public void setLastAnswer(byte[] sb) {
        lastAnswerBytes = sb;
    }

    @Override
    public StringBuilder getEmulatedAnswer() {
        return this.emulatedAnswer;
    }

    @Override
    public void setEmulatedAnswer(StringBuilder sb) {
        this.emulatedAnswer = sb;
    }

    @Override
    public int getBuffClearTimeLimit() {
        return buffClearTimeLimit;
    }

    @Override
    public void setHasAnswer(boolean hasAnswer) {
        this.hasAnswer = hasAnswer;
    }

    private CommandListClass commands = new CommandListClass();

    @Override
    public CommandListClass getCommandListClass(){
        return this.commands;
    }

    public void enable() {
        comPort.openPort();
        comPort.flushDataListener();
        comPort.removeDataListener();
        comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 15, 10);
        if(comPort.isOpen()){
            log.info("Порт открыт, задержки выставлены");
        }
    }

    @Override
    public void parseData() {
        System.out.println("EDWARDS_D397_00_000 run parse");
        if(lastAnswerBytes.length > 0) {

            lastAnswer.setLength(0);
            if (commands.isKnownCommand(cmdToSend)) {
                answerValues = commands.getCommand(cmdToSend).getResult(lastAnswerBytes);
                hasAnswer = true;
                for (int i = 0; i < answerValues.getValues().length; i++) {
                    lastAnswer.append(answerValues.getValues()[i]);
                    lastAnswer.append(" ");
                    lastAnswer.append(answerValues.getUnits()[i]);
                    lastAnswer.append("  ");
                }
                System.out.println("EDWARDS_D397_00_000 done correct...[" + lastAnswer.toString() + "]...");
            }else {
                for (int i = 0; i < lastAnswerBytes.length; i++) {
                    lastAnswer.append( (char) lastAnswerBytes[i]);
                }
                System.out.println("EDWARDS_D397_00_000 Cant create answers obj");
            }


        }else{
            System.out.println("EDWARDS_D397_00_000 empty received");
        }
    }


    public String getAnswer(){
        if(hasAnswer) {
            hasAnswer = false;
            return lastAnswer.toString();
        }else {
            return null;
        }
    }

    public boolean hasAnswer(){
        //System.out.println("return flag " + hasAnswer);
        return hasAnswer;
    }

    public boolean hasValue(){
        return knownCommand && hasAnswer;
    }

    public AnswerValues getValues(){
        return this.answerValues;
    }

    {
        commands.addCommand(
                new SingleCommand(
                        "?V00913", "?V00913 - Запрос давления",
                        (response) -> {
                            answerValues = null;
                            if (response.length > 11 && response[1] == 'V') {  // Проверка длины и наличия буквы 'V' на позиции 1
                                String responseStr = new String(response);

                                int firstPart = responseStr.indexOf("V913 ") + 5;
                                if (firstPart > 4) {  // Проверка, что "V913 " найдено в строке
                                    int endPart = responseStr.indexOf(";", firstPart);
                                    if (endPart > firstPart) {  // Проверка, что найдена первая точка с запятой после числа
                                        try {
                                            double value = Double.parseDouble(responseStr.substring(firstPart, endPart));
                                            System.out.println("Parser result " + value);
                                            answerValues = new AnswerValues(1);
                                            answerValues.addValue(value, " unit");
                                            return answerValues;
                                        } catch (NumberFormatException e) {
                                            System.out.println("Can't parse");
                                            return null;
                                        }
                                    } else {
                                        System.out.println("End part not found");
                                        return null;
                                    }
                                } else {
                                    System.out.println("Pattern 'V913 ' not found");
                                    return null;
                                }
                            } else {
                                System.out.println("Wrong Length or Missing 'V' at position 1, Length: " + response.length + ", Content: " + Arrays.toString(response));
                                return null;
                            }
                        })
        );
    }
}
