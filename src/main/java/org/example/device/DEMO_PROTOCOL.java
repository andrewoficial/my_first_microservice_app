package org.example.device;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import com.fazecast.jSerialComm.SerialPort;
import lombok.Setter;
import org.apache.log4j.Logger;
import org.example.services.AnswerValues;
import org.example.utilites.CommandListClass;

import java.util.Arrays;

public class DEMO_PROTOCOL implements SomeDevice {
    private volatile boolean bisy = false;
    private static final Logger log = Logger.getLogger(DEMO_PROTOCOL.class);
    private final SerialPort comPort;
    private volatile boolean hasAnswer = false;
    private volatile StringBuilder lastAnswer;
    private byte [ ] lastAnswerBytes;
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


    public DEMO_PROTOCOL(SerialPort port){
        log.info("Создан объект протокола DEMO_PROTOCOL");
        this.comPort = port;
        this.enable();
    }

    public DEMO_PROTOCOL(){
        System.out.println("Создан объект протокола DEMO_PROTOCOL эмуляция");
        this.comPort = null;
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
    public StringBuilder getEmulatedAnswer(){
        return this.emulatedAnswer;
    }

    @Override
    public void setEmulatedAnswer(StringBuilder sb){
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

    @Override
    public byte[] getStrEndian() {
        return this.strEndian;
    }

    @Override
    public boolean isKnownCommand(){
        String cmd = String.valueOf(this.strToSend);
        return cmd != null && cmd.contains("CRDG?") && (cmd.length() == "CRDG? 10".length() || cmd.length() == ("CRDG? 5".length()));
    }

    @Override
    public int getReceivedCounter() {
        return this.received;
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
        return this.millisLimit;
    }

    @Override
    public long getRepeatGetAnswerTimeDelay() {
        return this.repeatGetAnswerTimeDelay;
    }

    @Override
    public void setLastAnswer(byte [] ans) {

        lastAnswerBytes = ans;
        this.lastAnswer = new StringBuilder(Arrays.toString(ans));
    }

    @Override
    public SerialPort getComPort(){
        return this.comPort;
    }

    private CommandListClass commands = new CommandListClass();

    public boolean enable() {
        comPort.openPort();
        comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 85, 95);
        if(comPort.isOpen()){
            log.info("Порт открыт, задержки выставлены");
        }else {
            throw new RuntimeException("Cant open COM-Port");
        }
        millisDela = 0L;
        return false;
    }

    @Override
    public int getRepetCounterLimit() {
        return 0;
    }

    public StringBuilder getForSend(){
        return strToSend;
    }

    public void setReceived(String answer){
        this.received = answer.length();
        deviceAnswer = answer;
        this.parseData();
    }

    public void parseData() {

            if(knownCommand && isCorrectAnswer() && hasAnswer){
                log.trace("Ответ правильный " + lastAnswer.toString());
                System.out.println("Ответ правильный " + lastAnswer.toString());
                value = 0.0;
                degree = 0;
                try{
                    hasValue = true;
                    //int firstPart = lastAnswer.indexOf("M") + 1;
                    //System.out.println(firstPart);
                    value = Double.parseDouble(lastAnswer.toString());
                    //degree = Integer.parseInt(lastAnswer.substring(firstPart+5, firstPart+6));
                } catch (NumberFormatException e) {
                    //System.out.println("Parse error");
                    //throw new RuntimeException(e);
                    hasValue = false;
                    return;
                }


                val = value;
                //val /= 10000.0;
                System.out.println(val);
                if(hasAnswer){
                    DEMO_PROTOCOL.CommandList cmd = DEMO_PROTOCOL.CommandList.getCommandByName(cmdToSend);
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
                //System.out.println(lastAnswer.toString());
                //lastValue = String.valueOf(val);
            }else{
                log.trace("Ответ с ошибкой " + lastAnswer.toString());
                System.out.println("Ответ с ошибкой " + lastAnswer.toString());
                hasValue = false;
            }

    }


    private boolean isCorrectAnswer(){
        if((lastAnswer.length() == 7 || lastAnswer.length() == 6 || lastAnswer.length() == 8)){
            //ToDo add CRC
            return true;
        }
        return false;
    }
    public String getAnswer(){
    if(! hasValue){
        lastAnswer.setLength(0);
        for (int i = 0; i < lastAnswerBytes.length; i++) {
            lastAnswer.append( (char) lastAnswerBytes[i]);
        }

        lastAnswer.append("\n");
        lastAnswer.append(Arrays.toString(lastAnswerBytes));
    }

        String forReturn = new String(lastAnswer);
        lastAnswer = null;
        hasAnswer = false;
        return forReturn;
    }

    public boolean hasAnswer(){
        //System.out.println("return flag " + hasAnswer);
        return hasAnswer;
    }

    public boolean hasValue(){
        return hasValue;
    }

    public AnswerValues getValues(){
        AnswerValues valToSend =  new AnswerValues(1);
        valToSend.addValue(val, "°C");
        return valToSend;
    }

    private enum CommandList{


        TERM("TERM?", (response) -> {
            // Ваш алгоритм проверки для TERM?
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
        }),
        FF("F", (response) -> {
            // Ваш алгоритм проверки для F
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
        }),
        SREV("SREV?", (response) -> {
            // Ваш алгоритм проверки для SREV?
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
        }),
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
            for (DEMO_PROTOCOL.CommandList someEnum : DEMO_PROTOCOL.CommandList.values()) {
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
            List<String> values = DEMO_PROTOCOL.CommandList.getValues();
            return values.get(number);
        }

        public Double[] parseAnswer(String response) {
            return parseFunction.apply(response);
        }

        public static DEMO_PROTOCOL.CommandList getCommandByName(String name) {
            for (DEMO_PROTOCOL.CommandList command : DEMO_PROTOCOL.CommandList.values()) {
                if (command.name.equals(name)) {
                    return command;
                }
            }
            return null;
        }
    }
}
