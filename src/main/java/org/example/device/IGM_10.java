package org.example.device;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import com.fazecast.jSerialComm.SerialPort;
import lombok.Setter;
import org.apache.log4j.Logger;
import org.example.services.AnswerValues;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;

public class IGM_10 implements SomeDevice {
    private volatile boolean busy = false;
    private static final Logger log = Logger.getLogger(IGM_10.class);
    private final SerialPort comPort;
    private byte [ ] lastAnswerBytes;
    private StringBuilder lastAnswer = new StringBuilder();
    private final StringBuilder emulatedAnswer = new StringBuilder();
    private final boolean knownCommand = false;
    private volatile boolean hasAnswer = false;
    private  volatile boolean hasValue = false;
    @Setter
    private byte [] strEndian = {13};//CR
    private int received = 0;
    private final long millisLimit = 750;
    private final long repeatWaitTime = 200;
    private final long millisPrev = System.currentTimeMillis();
    private final static Charset charset = Charset.forName("Cp1251");
    private static final CharsetDecoder decoder = charset.newDecoder();
    private AnswerValues answerValues = new AnswerValues(0);
    String cmdToSend;

    public IGM_10(SerialPort port){
        System.out.println("Создан объект протокола ИГМ-10");
        this.comPort = port;
        this.enable();
    }

    public IGM_10(){
        System.out.println("Создан объект протокола ИГМ-10 эмуляция");
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
    public void setLastAnswer(byte [] ans) {
        lastAnswerBytes = ans;
    }

    @Override
    public StringBuilder getEmulatedAnswer() {
        return this.emulatedAnswer;
    }

    @Override
    public void setEmulatedAnswer(StringBuilder sb) {
        emulatedAnswer.setLength(0);
        emulatedAnswer.append(sb);
    }


    @Override
    public void setHasAnswer(boolean hasAnswer) {
        this.hasAnswer = hasAnswer;
    }

    private CommandListClass commands = new CommandListClass();
    public boolean enable() throws RuntimeException{
        comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 65, 65);
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


    public void setReceived(String answer){
        this.received = answer.length();
        this.parseData();
    }

    @Override
    public void parseData() {
        System.out.println("Start parse...");
            if(lastAnswerBytes.length > 0) {
                answerValues = null;
                CommandList cmd = CommandList.getCommandByName(cmdToSend);
                //System.out.println("cmdToSend" + cmdToSend);
                //lastAnswer = new StringBuilder(lastAnswer.toString().replaceAll("\\p{C}", "?"));
                if (cmd != null) {
                    answerValues = cmd.parseAnswer(lastAnswerBytes);
                    //System.out.println("lastAnswer.toString()" + lastAnswer.toString());

                }

                lastAnswer.setLength(0);
                if(answerValues != null){
                    hasAnswer = true;
                    for (int i = 0; i < answerValues.getValues().length; i++) {
                        lastAnswer.append(answerValues.getValues()[i]);
                        lastAnswer.append(" ");
                        lastAnswer.append(answerValues.getUnits()[i]);
                        lastAnswer.append("  ");
                    }
                    System.out.println("done correct...");

                }else{
                    hasAnswer = true;
                    for (byte lastAnswerByte : lastAnswerBytes) {
                        System.out.print(lastAnswerByte);
                        System.out.print(" ");
                        lastAnswer.append((char) lastAnswerByte);
                    }
                    System.out.println("done unknown...");
                }
            }else {
                for (byte lastAnswerByte : lastAnswerBytes) {
                    System.out.print(lastAnswerByte);
                    System.out.print(" ");
                    lastAnswer.append((char) lastAnswerByte);
                }
                System.out.println("done empty...");
                hasAnswer = false;
            }


    }


    public boolean isBusy(){
        return busy;
    }

    @Override
    public void setBusy(boolean busy){
        this.busy = busy;
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
        return false;
    }

    public AnswerValues getValues(){
        return answerValues;
    }

    private enum CommandList{


        TERM("TERM?", (response) -> {
            // Ваш алгоритм проверки для TERM?

            System.out.print("Start TERM length ");
            System.out.println(response.length);
            AnswerValues answerValues = null;
            if(response.length > 5 && response.length < 10){
                ArrayList <Byte> cleanAnswer= new ArrayList<>();
                for (byte responseByte : response) {
                    if(responseByte > 32 && responseByte < 127)
                        cleanAnswer.add(responseByte);
                    //System.out.print(HexFormat.of().toHexDigits(lastAnswerByte) + " ");
                }

                //System.out.println();
                response = new byte[cleanAnswer.size()];
                StringBuilder sb = new StringBuilder();
                for (byte aByte : cleanAnswer) {
                    sb.append((char) aByte);
                }

                try{
                    Double answer = Double.parseDouble(sb.toString());
                    answer /= 100;
                    if(answer > 80 || answer < -80){
                        throw new NumberFormatException("Wrong number");
                    }else {
                        answerValues = new AnswerValues(1);
                        answerValues.addValue(answer, "°С");
                        System.out.println("degree " + answer);
                    }

                }catch (NumberFormatException ignored){

                }
            }
            //System.out.println("Result " + anAr[0]);
            return answerValues;
        }),
        FF("F", (response) -> {
            // Ваш алгоритм проверки для F
            return null;
        }),
        SREV("SREV?", (response) -> {
            // Ваш алгоритм проверки для SREV?
            return null;
        }),
        ALMH("ALMH?", (response) -> {
            // Ваш алгоритм проверки для SRAL?
            CharBuffer charBuffer = CharBuffer.allocate(512);  // Предполагаемый максимальный размер
            ByteBuffer byteBuffer = ByteBuffer.wrap(response);
            CoderResult result = decoder.decode(byteBuffer, charBuffer, true);
            if (result.isError()) {
                System.out.println("Error during decoding: " + result.toString());
            }else{
                return null;
            }
            charBuffer.flip();

            StringBuilder sb = new StringBuilder();
            sb.append(charBuffer);
            AnswerValues an = new AnswerValues(1);
            an.addValue(0.0, sb.toString());
            charBuffer.clear();
            sb.setLength(0);
            return an;
        });

        private final String name;
        private final Function<byte [], AnswerValues> parseFunction;
        private static final List<String> VALUES;

        static {
            VALUES = new ArrayList<>();
            for (CommandList someEnum : CommandList.values()) {
                VALUES.add(someEnum.name);
            }
        }

        CommandList(String name, Function<byte [], AnswerValues> parseFunction) {
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
            List<String> values = CommandList.getValues();
            return values.get(number);
        }

        public AnswerValues parseAnswer(byte [] response) {
            return parseFunction.apply(response);
        }

        public static CommandList getCommandByName(String name) {
            for (CommandList command : CommandList.values()) {
                if (command.name.equals(name)) {
                    return command;
                }
            }
            return null;
        }
    }
}
