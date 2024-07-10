package org.example.device;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import org.example.services.AnswerValues;
import lombok.Setter;
import org.apache.log4j.Logger;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.example.Main.comPorts;

public class ARD_FEE_BRD_METER implements SomeDevice {
    private volatile boolean bisy = false;
    private static final Logger log = Logger.getLogger(DEMO_PROTOCOL.class);
    private final SerialPort comPort;
    private volatile boolean hasAnswer = false;
    private volatile StringBuilder lastAnswer;
    private AnswerValues answerValues = new AnswerValues(0);

    private volatile String lastValue;

    @Setter
    private byte [] strEndian = {13, 10};//CR + LF
    private boolean knownCommand = false;

    private StringBuilder emulatedAnswer;
    private  volatile boolean hasValue;
    private int received = 0;
    private long millisLimit = 5000L;

    private long repeatGetAnswerTimeDelay = 200;
    private long millisDela = 0L;
    private long millisPrev = System.currentTimeMillis();
    private double value = 0;
    private long degree = 0;
    private double val;
    private int buffClearTimeLimit = 250;
    //For JUnits
    private StringBuilder strToSend;
    private String deviceAnswer;
    String cmdToSend;



    public ARD_FEE_BRD_METER(SerialPort port){
        //System.out.println("Create obj");
        this.comPort = port;
        this.enable();
    }

    @Override
    public void setCmdToSend(String str) {
        str = cmdToSend;
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
        //this.lastAnswer = sb;
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
        return this.buffClearTimeLimit;
    }

    @Override
    public void setHasAnswer(boolean hasAnswer) {
        this.hasAnswer = hasAnswer;
    }
    public void enable() {

        comPort.flushDataListener();
        comPort.removeDataListener();
        //comPort.addDataListener(this);
        //System.out.println("open port and add listener");
        int timeout = 1000 - comPort.getBaudRate();
        comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, timeout, timeout);
        if(comPort.isOpen()){
            log.info("Порт открыт, задержки выставлены");
        }else{
            log.info("Ошибка открытия порта");
        }
    }

    @Override
    public int getRepetCounterLimit() {
        return 0;
    }


    public void setReceived(String answer){
        this.received = answer.length();
        deviceAnswer = answer;
        this.parseData();
    }

    @Override
    public void parseData() {
        if(received > 0) {
            if(comPort != null) {
                log.trace("Начинаю разбор посылки длиною " + received);
                byte[] buffer = new byte[comPort.bytesAvailable()];
                //System.out.println("Создал буфер ");
                comPort.readBytes(buffer, comPort.bytesAvailable());
                //System.out.println("Считал данные " );
                lastAnswer = new StringBuilder(new String(buffer));
                //System.out.println("Сохранил ответ " );
            }else{
                lastAnswer = new StringBuilder(deviceAnswer);
            }
            hasAnswer = true;
            if(knownCommand && isCorrectAnswer() && hasAnswer){
                log.debug("Ответ правильный " + lastAnswer.toString());
                value = 0;
                degree = 0;
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
                hasValue = false;
            }
        }

    }

    private boolean isCorrectAnswer(){
        if((lastAnswer.length() == 11 || lastAnswer.length() == 12 || lastAnswer.length() == 13)){
            //ToDo add CRC
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
