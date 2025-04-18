package org.example.device;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import com.fazecast.jSerialComm.SerialPort;
import lombok.Setter;
import org.apache.log4j.Logger;
import org.example.services.AnswerValues;

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
    private byte [] strEndian = {13};//CR + LF
    private boolean knownCommand = false;

    private StringBuilder emulatedAnswer;
    private  volatile boolean hasValue;
    private int received = 0;
    private final long millisLimit = 450;
    private final long repeatWaitTime = 100;

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


    public boolean isBusy(){
        return bisy;
    }

    @Override
    public void setBusy(boolean busy){
        this.bisy = busy;
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
    public int getMillisReadLimit() {
        return 500;
    }

    @Override
    public int getMillisWriteLimit() {
        return 500;
    }

    @Override
    public long getRepeatWaitTime() {
        return this.repeatWaitTime;
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
            return true;
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


            if(hasAnswer){
                    DEMO_PROTOCOL.CommandList cmd = DEMO_PROTOCOL.CommandList.getCommandByName(cmdToSend);
            }

    }



    public String getAnswer(){
    if(! hasValue){
        //Добавление вывода в HEX находится тут
        lastAnswer.setLength(0);
        for (int i = 0; i < lastAnswerBytes.length; i++) {
            lastAnswer.append( (char) lastAnswerBytes[i]);
        }

        //lastAnswer.append("\n");
        //lastAnswer.append(Arrays.toString(lastAnswerBytes));
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


        TERM("AM_I_DEMO", (response) -> {
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
