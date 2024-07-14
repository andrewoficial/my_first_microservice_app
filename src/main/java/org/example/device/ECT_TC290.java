package org.example.device;

import java.util.*;
import java.util.function.Function;
import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import lombok.Setter;
import org.apache.log4j.Logger;
import org.example.gui.ChartWindow;
import org.example.services.AnswerValues;
import org.example.utilites.CommandListClass;
import org.example.utilites.ParseException;
import org.example.utilites.SingleCommand;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

public class ECT_TC290 implements SomeDevice  {
    public CommandListClass commandListClass = new CommandListClass();
    private volatile boolean bisy = false;
    private static final Logger log = Logger.getLogger(IGM_10.class);
    private final SerialPort comPort;
    private byte [ ] lastAnswerBytes;
    private final StringBuilder lastAnswer = new StringBuilder();
    private StringBuilder emulatedAnswer = new StringBuilder();
    private final boolean knownCommand = false;
    private volatile boolean hasAnswer = false;
    private  volatile boolean hasValue = false;
    @Setter
    private byte [] strEndian = {13};//CR
    private int received = 0;
    private final long millisLimit = 500000;
    private final long repeatGetAnswerTimeDelay = 1;
    private final int buffClearTimeLimit = 2;
    private final int repetCounterLimit = 150;
    private final long millisPrev = System.currentTimeMillis();
    private final Charset charset = Charset.forName("Cp1251");
    private final CharsetDecoder decoder = charset.newDecoder();
    private final CharBuffer charBuffer = CharBuffer.allocate(512);  // Предполагаемый максимальный размер
    private static AnswerValues answerValues = new AnswerValues(0);
    String cmdToSend;



    public ECT_TC290(SerialPort port){
        log.info("Создан объект протокола ECT_TC290");
        this.comPort = port;

        this.enable();
    }

    public ECT_TC290(){
        System.out.println("Создан объект протокола ECT_TC290 эмуляция");
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
        received = cnt;
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
    public void setLastAnswer(byte[] ans) {
//        for (byte an : ans) {
//            System.out.print(an);
//        }
        lastAnswerBytes = ans;
    }

    @Override
    public StringBuilder getEmulatedAnswer() {
        return emulatedAnswer;
    }

    @Override
    public void setEmulatedAnswer(StringBuilder sb) {
        emulatedAnswer = sb;
    }

    @Override
    public int getBuffClearTimeLimit() {
        return buffClearTimeLimit;
    }

    @Override
    public void setHasAnswer(boolean hasAnswer) {

    }

    private CommandListClass commands = new CommandListClass();

    @Override
    public CommandListClass getCommandListClass(){
        return this.commands;
    }
    public void enable() {
        comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 15, 10);
        if(comPort.isOpen()){
            log.info("Порт открыт, задержки выставлены");
        }
    }

    @Override
    public int getRepetCounterLimit() {
        return repetCounterLimit;
    }


    public String getForSend(){
        return cmdToSend;
    }

    public void setReceived(String answer){
        this.received = answer.length();
        this.parseData();
    }

    @Override
    public void parseData() {
        //System.out.println("ECT_TC290 run parse");
        if(lastAnswerBytes.length > 0) {
            lastAnswer.setLength(0);
            if (commands.isKnownCommand(cmdToSend)) {
                answerValues = commands.getCommand(cmdToSend).getResult(lastAnswerBytes);
                hasAnswer = true;
                if(answerValues == null){
                    System.out.println("ECT_TC290 done known command. Result NULL.");
                    return;
                }
                for (int i = 0; i < answerValues.getValues().length; i++) {
                    lastAnswer.append(answerValues.getValues()[i]);
                    lastAnswer.append(" ");
                    lastAnswer.append(answerValues.getUnits()[i]);
                    lastAnswer.append("  ");
                }
                //System.out.println("ECT_TC290 done correct...[" + lastAnswer.toString() + "]...");
            }else {
                for (int i = 0; i < lastAnswerBytes.length; i++) {
                    lastAnswer.append( (char) lastAnswerBytes[i]);
                }
                System.out.println("ECT_TC290 Cant create answers obj");
            }


        }else{
            System.out.println("ECT_TC290 empty received");
        }
    }


    private boolean isCorrectAnswer(){
        if((lastAnswer.length() == 7 || lastAnswer.length() == 6 || lastAnswer.length() == 8)){
            return true;
        }
        return false;
    }
    public String getAnswer(){

        hasAnswer = false;
        return lastAnswer.toString();
    }

    public boolean hasAnswer(){
        //System.out.println("return flag " + hasAnswer);
        return hasAnswer;
    }

    public boolean hasValue(){
        return hasValue;
    }

    private void saveAnswerValue(){
        answerValues = new AnswerValues(1);

        double ans = Double.parseDouble(lastAnswer.toString());
        answerValues.addValue(ans, "deg");

    }
    public AnswerValues getValues(){
        return this.answerValues;
    }




    {
        commands.addCommand(
                new SingleCommand(
                        "CRDG? ", "CRDG? 1 - запрос температуры у первого датчика. CRDG? 10 - запрос температуры у десятого датчика",
                        (response) -> { //"CRDG? 1" - direct
                    answerValues = null;
                    //System.out.println("Proceed CRDG direct");
                    String example = "29.1899";
                    if(response.length >= 7 ){
                        //response[2] == '.' || (response[0] == '-' && response[3] == '.') || (response[0] == '-' && response[4] == '.')
                        //toDo set filters
                        if(true) {
                            Double value;
                            StringBuilder sb = new StringBuilder();
                            for (byte b : response) {
                                sb.append((char)b);
                            }
                            String rsp = sb.toString();
                            //System.out.println("Parse answer [" + rsp + "] ");

                            rsp = rsp.trim();
                            //log.debug("Parse " + rsp);

                            rsp = rsp.replaceAll("[^0-9.,-]", ""); // удалится все кроме цифр и указанных знаков
                            boolean success = false;
                            try{

                                value = (double) Double.parseDouble(rsp);
                                success = true;
                            }catch (NumberFormatException e){
                                System.out.println("Exception " + e.getMessage());
                                value = 0.0;
                                success = false;
                            }
                            if(success){
                                answerValues = new AnswerValues(1);
                                answerValues.addValue(value, " °C");
                            }else{
                                answerValues = null;
                            }
                            //System.out.println("Parser result " + value);

                            return answerValues;
                        }else {
                            System.out.println("Wrong POINT position  " + Arrays.toString(response));
                        }
                    }else {
                        System.out.println("Wrong answer length " + response.length);
                        for (byte b : response) {
                            System.out.print(b + " ");
                        }
                        System.out.println();
                    }
                    return answerValues;
                })
        );

        commands.addCommand(
                new SingleCommand("CRDG?", "CRDG? - без аргументов. Опрос у всех сенсоров ", (response) -> {
                    answerValues = null;
                    //System.out.println("Proceed CRDG direct");
                    String example = "29.1899";
                    if(response.length >= 99 ){
                        StringBuilder sb = new StringBuilder();
                        for (byte b : response) {
                            sb.append((char)b);
                        }
                        String rsp = sb.toString();
                        //System.out.println("Parse answer [" + rsp + "] ");

                        rsp = rsp.trim();
                        String [] strValues = rsp.split(",");
                        //System.out.println("Found" + strValues.length);
                        answerValues = new AnswerValues(10);
                        for (int i = 0; i < strValues.length; i++) {
                            double value = 0.0;
                            strValues[i] = strValues[i].replace(",", "");
                            strValues[i] = strValues[i].trim();
                            strValues[i] = strValues[i].replaceAll("[^0-9.,-]", ""); // удалится все кроме цифр и указанных знаков
                            //System.out.println("Parse " + strValues[i]);
                            boolean success = false;
                            try{
                                success = true;
                                value = Double.parseDouble(strValues[i]);
                            }catch (NumberFormatException e){
                                success = false;
                                System.out.println("Exception " + e.getMessage());
                                //Past cleaner here
                                //Throw exception


                            }
                            if(success){
                                answerValues.addValue(value, " °C");
                            }else{
                                //throw new ParseException("Exception message", "Exception message");
                                answerValues = null;
                            }
                        }
                    }else {
                        System.out.println("Wrong answer length " + response.length);
                        for (byte b : response) {
                            System.out.print(b + " ");
                        }
                        System.out.println();
                    }
                    return answerValues;
                })
        );
    }

}
