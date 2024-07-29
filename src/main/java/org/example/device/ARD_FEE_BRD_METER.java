package org.example.device;

import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import com.fazecast.jSerialComm.SerialPort;
import org.example.services.AnswerValues;
import lombok.Setter;
import org.apache.log4j.Logger;
import org.example.utilites.CommandListClass;

import java.nio.charset.Charset;

public class ARD_FEE_BRD_METER implements SomeDevice {
    private volatile boolean bisy = false;
    private static final Logger log = Logger.getLogger(IGM_10.class);
    private final SerialPort comPort;
    private byte [ ] lastAnswerBytes;
    private StringBuilder lastAnswer = new StringBuilder();
    private StringBuilder emulatedAnswer = new StringBuilder();
    private final boolean knownCommand = false;
    private volatile boolean hasAnswer = false;
    private  volatile boolean hasValue = false;
    @Setter
    private byte [] strEndian = {13, 10};//CR
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



    public ARD_FEE_BRD_METER(SerialPort port){
        log.info("Создан объект протокола ECT_TC290");
        this.comPort = port;
        this.enable();
    }

    public ARD_FEE_BRD_METER(){
        System.out.println("Создан объект протокола ARD_FEE_BRD_METER эмуляция");
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

    public boolean enable() {
        comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 15, 10);
        if(comPort.isOpen()){
            log.info("Порт открыт, задержки выставлены");
        }else {
            throw new RuntimeException("Cant open COM-Port");
        }
        return false;
    }

    @Override
    public int getRepetCounterLimit() {
        return repetCounterLimit;
    }

    //ToDo изменить логику обработки, как в остальных протоколах
    int value = 0;
    int degree = 0;
    double val  = 0.0;

    public String getForSend(){
        return cmdToSend;
    }

    public void setReceived(String answer){
        this.received = answer.length();
        this.parseData();
    }

    @Override
    public void parseData() {
        received = lastAnswerBytes.length;
        if(received > 0) {

                lastAnswer.setLength(0);
                for (int i = 0; i < lastAnswerBytes.length; i++) {
                    lastAnswer.append( (char) lastAnswerBytes[i]);
                }

            hasAnswer = true;

            if(knownCommand && isCorrectAnswer() && hasAnswer){
                log.debug("Ответ правильный " + lastAnswer.toString());

                try{
                    int firstPart = lastAnswer.indexOf("M") + 1;
                    //System.out.println(firstPart);
                    value = Integer.parseInt(lastAnswer.substring(firstPart, firstPart+5));
                    degree = Integer.parseInt(lastAnswer.substring(firstPart+5, firstPart+6));
                } catch (NumberFormatException e) {
                    //System.out.println("Parse error");
                    //throw new RuntimeException(e);
                    hasValue = false;
                    return;
                }
                hasValue = true;

                val = value * (long) Math.pow(10, degree);
                val /= 10000.0;
                //System.out.println(val);
                //lastValue = String.valueOf(val);

                if(hasAnswer){
                    ARD_FEE_BRD_METER.CommandList cmd = ARD_FEE_BRD_METER.CommandList.getCommandByName(cmdToSend);
                    if (cmd != null) {
                        Double [] answer = cmd.parseAnswer(lastAnswer.toString());
                        if(answer.length > 0){
                            answerValues = new AnswerValues(answer.length);
                            for (int i = 0; i < answer.length; i++) {
                                answerValues.addValue(answer[i], " unit");
                                System.out.println(answerValues.getValues()[i]);
                            }
                        }
                    }
                }
            }else{
                log.debug("Ответ с ошибкой " + lastAnswer.toString());
                System.out.println("Ответ с ошибкой " + lastAnswer.toString());
                lastAnswer.setLength(0);
                for (int i = 0; i < lastAnswerBytes.length; i++) {
                    lastAnswer.append( (char) lastAnswerBytes[i]);
                }
                hasValue = false;
            }
        }else{
            log.debug("Ничего не принято в ответ");
            System.out.println("Ничего не принято в ответ");
        }

    }

    private boolean isCorrectAnswer(){
        if((lastAnswer.length() == 11 || lastAnswer.length() == 12 || lastAnswer.length() == 13)){
            return true;
        }
        return false;
    }
    public String getAnswer(){

        System.out.println("Return answer " + lastAnswer.toString());
        String forReturn = new String(lastAnswer);

        lastAnswer = null;
        hasAnswer = false;
        return forReturn;
    }

    public boolean hasAnswer(){
        return hasAnswer;
    }

    public boolean hasValue(){
        return hasValue;
    }

    public AnswerValues getValues(){
        AnswerValues valToSend =  new AnswerValues(1);
        valToSend.addValue(val, " dunno");
        return valToSend;
    }

    private enum CommandList{

        SRAL("SRAL?", (response) -> {
            // Ваш алгоритм проверки для SRAL?
            Double [] anAr = new Double[0];
            if(response.length() == 7 && response.contains("\n")){
                try{
                    Double answer = Double.parseDouble(response);
                    anAr = new Double[1];
                    anAr [0] = answer;
                }catch (NumberFormatException e){
                    anAr = new Double[0];
                }
                return anAr;
            }
            return anAr;
        });

        private final String name;
        private final Function<String, Double[]> parseFunction;
        private static final List<String> VALUES;

        static {
            VALUES = new ArrayList<>();
            for (ARD_FEE_BRD_METER.CommandList someEnum : ARD_FEE_BRD_METER.CommandList.values()) {
                VALUES.add(someEnum.name);
            }
        }

        CommandList(String name, Function<String, Double[]> parseFunction) {
            this.name = name;
            this.parseFunction = parseFunction;
        }

        public String getValue() {
            return name;
        }

        public static List<String> getValues() {
            return Collections.unmodifiableList(VALUES);
        }

        public static String getLikeArray(int number) {
            List<String> values = ARD_FEE_BRD_METER.CommandList.getValues();
            return values.get(number);
        }

        public Double[] parseAnswer(String response) {
            return parseFunction.apply(response);
        }

        public static ARD_FEE_BRD_METER.CommandList getCommandByName(String name) {
            for (ARD_FEE_BRD_METER.CommandList command : ARD_FEE_BRD_METER.CommandList.values()) {
                if (command.name.equals(name)) {
                    return command;
                }
            }
            return null;
        }
    }
}
