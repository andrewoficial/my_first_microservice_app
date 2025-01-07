package org.example.device;

import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;

import com.fazecast.jSerialComm.SerialPort;
import lombok.Setter;
import org.apache.log4j.Logger;
import org.example.services.AnswerValues;

import java.nio.charset.Charset;

public class ERSTEVAK_MTP4D implements SomeDevice {
    private volatile boolean busy = false;
    private static final Logger log = Logger.getLogger(IGM_10.class);
    private final SerialPort comPort;
    private byte [ ] lastAnswerBytes = new byte[1];
    private final StringBuilder lastAnswer = new StringBuilder();
    private StringBuilder emulatedAnswer = new StringBuilder();
    private final boolean knownCommand = false;
    private volatile boolean hasAnswer = false;
    private  volatile boolean hasValue = false;
    @Setter
    private byte [] strEndian = {13};//CR
    private int received = 0;
    private final long millisLimit = 155;
    private final long repeatWaitTime = 100;
    private final long millisPrev = System.currentTimeMillis();
    private final Charset charset = Charset.forName("Cp1251");
    private final CharsetDecoder decoder = charset.newDecoder();
    private final CharBuffer charBuffer = CharBuffer.allocate(512);  // Предполагаемый максимальный размер
    private static AnswerValues answerValues = new AnswerValues(0);
    String cmdToSend;


    public ERSTEVAK_MTP4D(SerialPort port){
        log.info("Создан объект протокола ERSTEVAK_MTP4D");
        this.comPort = port;
        this.enable();
    }

    public ERSTEVAK_MTP4D(){
        System.out.println("Создан объект протокола ERSTEVAK_MTP4D эмуляция");
        this.comPort = null;
    }
    @Override
    public void setCmdToSend(String str) {
        cmdToSend = str;
        //str = cmdToSend;
    }

    @Override
    public Integer getTabForAnswer() {
        return null;
    }


    @Override
    public boolean isBusy(){
        return busy;
    }

    @Override
    public void setBusy(boolean busy){
        this.busy = busy;
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
    public long getMillisPrev() {
        return millisPrev;
    }

    @Override
    public long getMillisLimit() {
        return millisLimit;
    }

    @Override
    public long getRepeatWaitTime() {
        return repeatWaitTime;
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
    public void setHasAnswer(boolean hasAnswer) {
        this.hasAnswer = hasAnswer;
    }

    public CommandListClass commands = new CommandListClass();
    @Override
    public CommandListClass getCommandListClass(){
        return this.commands;
    }
    public boolean enable() {
        if(! comPort.isOpen()){
            comPort.openPort();
            comPort.flushDataListener();
            comPort.removeDataListener();
            comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 15, 10);
            if(comPort.isOpen()){
                log.info("Порт открыт, задержки выставлены");
                return true;
            }else {
                throw new RuntimeException("Cant open COM-Port");
            }

        }else{
            log.info("Порт был открыт ранее");
            return true;
        }
    }


    public String getForSend(){
        return cmdToSend;
    }

    public void setReceived(String answer){
        lastAnswerBytes = answer.getBytes();
        this.received = lastAnswerBytes.length;
         //this.parseData();
    }

    @Override
    public void parseData() {
        //System.out.println("ERSTEVAK_MTP4D run parse");
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
                    //System.out.println("ERSTEVAK_MTP4D done correct...[" + lastAnswer.toString() + "]...");
                }else {
                    hasAnswer = true;
                    for (int i = 0; i < lastAnswerBytes.length; i++) {
                        lastAnswer.append( (char) lastAnswerBytes[i]);
                    }
                    //System.out.println("ERSTEVAK_MTP4D Cant create answers obj");
                }


        }else{
            //System.out.println("ERSTEVAK_MTP4D empty received");
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
        return hasValue;
    }

    public AnswerValues getValues(){
        return answerValues;
    }

    {
        commands.addCommand(
                new SingleCommand(
                        "M^", "M^ - запрос давления у датчика. 001M^ - запрос температуры у первого прибора в линии",
                        (response) -> {
                            answerValues = null;
                            //System.out.println("Proceed M^");
                            String example = "001M960022Q\n";
                            if (response.length == example.length()) {
                                if (response[3] == 'M') {
                                    Double value, degree;
                                    StringBuilder sb = new StringBuilder();
                                    for (byte b : response) {
                                        sb.append((char) b);
                                    }
                                    String rsp = sb.toString();
                                    //System.out.println("Asw value " + rsp);
                                    //log.debug("Parse " + rsp);
                                    try {
                                        int firstPart = rsp.indexOf("M") + 1;
                                        //System.out.println(firstPart);
                                        value = (double) Integer.parseInt(rsp.substring(firstPart, firstPart + 5));
                                        degree = (double) Integer.parseInt(rsp.substring(firstPart + 5, firstPart + 6));
                                    } catch (NumberFormatException e) {
                                        value = 0.0;
                                        degree = 0.0;
                                    }

                                    value = (value * (double) Math.pow(10, degree));
                                    value /= 10000.0;
                                    //System.out.println("Parser result " + value);
                                    answerValues = new AnswerValues(1);
                                    answerValues.addValue(value, " unit");
                                    return answerValues;
                                } else {
                                    //System.out.println("Wrong M position  " + Arrays.toString(response));
                                    return null;
                                }
                            } else {
                                //System.out.println("Wrong answer length " + response.length);
                            }
                            return answerValues;
                        })
        );
    }
}
