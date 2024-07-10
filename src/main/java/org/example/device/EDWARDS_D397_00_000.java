package org.example.device;

import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import com.fazecast.jSerialComm.SerialPort;
import lombok.Setter;
import org.apache.log4j.Logger;
import org.example.services.AnswerValues;



public class EDWARDS_D397_00_000  implements SomeDevice  {
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
        return buffClearTimeLimit;
    }

    @Override
    public void setHasAnswer(boolean hasAnswer) {
        this.hasAnswer = hasAnswer;
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
    public int getRepetCounterLimit() {
        return 0;
    }


    @Override
    public void parseData() {
        if(received > 0) {
            log.info("Начинаю разбор посылки длиною " + received);
            byte[] buffer = new byte[comPort.bytesAvailable()];
            comPort.readBytes(buffer, comPort.bytesAvailable());

            lastAnswer = new StringBuilder(new String(buffer));
            hasAnswer = true;
            if(knownCommand && lastAnswer.length() > 11) {
                if (hasAnswer) {
                    EDWARDS_D397_00_000.CommandList cmd = EDWARDS_D397_00_000.CommandList.getCommandByName(cmdToSend);
                    if (cmd != null) {
                        Double[] answer = cmd.parseAnswer(lastAnswer.toString());
                        if (answer.length > 0) {
                            answerValues = new AnswerValues(answer.length);
                            for (int i = 0; i < answer.length; i++) {
                                answerValues.addValue(answer[i], " unit");
                                System.out.println(answerValues.getValues()[i]);
                            }
                        }
                    }
                }
            }
        }
    }


    public String getAnswer(){

        int index = lastAnswer.indexOf("\n");
        if(index > 0){
            lastAnswer.deleteCharAt(index);
        }
        index = lastAnswer.indexOf("\r");
        if(index > 0){
            lastAnswer.deleteCharAt(index);
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
        return knownCommand && hasAnswer;
    }

    public AnswerValues getValues(){
        return this.answerValues;
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
                for (EDWARDS_D397_00_000.CommandList someEnum : EDWARDS_D397_00_000.CommandList.values()) {
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
                List<String> values = EDWARDS_D397_00_000.CommandList.getValues();
                return values.get(number);
            }

            public Double[] parseAnswer(String response) {
                return parseFunction.apply(response);
            }

            public static EDWARDS_D397_00_000.CommandList getCommandByName(String name) {
                for (EDWARDS_D397_00_000.CommandList command : EDWARDS_D397_00_000.CommandList.values()) {
                    if (command.name.equals(name)) {
                        return command;
                    }
                }
                return null;
            }
        }
}
