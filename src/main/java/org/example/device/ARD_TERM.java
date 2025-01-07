package org.example.device;

import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;

import com.fazecast.jSerialComm.SerialPort;
import lombok.Setter;
import org.apache.log4j.Logger;
import org.example.services.AnswerValues;
import org.example.utilites.MyUtilities;

import java.nio.charset.Charset;

public class ARD_TERM implements SomeDevice {
    private volatile boolean bisy = false;
    private static final Logger log = Logger.getLogger(ARD_TERM.class);
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
    private final long millisLimit = 170;
    private final long repeatWaitTime = 100;
    private final long millisPrev = System.currentTimeMillis();
    private final Charset charset = Charset.forName("Cp1251");
    private final CharsetDecoder decoder = charset.newDecoder();
    private final CharBuffer charBuffer = CharBuffer.allocate(512);  // Предполагаемый максимальный размер
    private static AnswerValues answerValues = new AnswerValues(0);
    String cmdToSend;


    public ARD_TERM(SerialPort port){
        log.info("Создан объект протокола OWON_SPE3051");
        this.comPort = port;
        this.enable();
    }

    public ARD_TERM(){
        System.out.println("Создан объект протокола OWON_SPE3051 эмуляция");
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
        return bisy;
    }

    @Override
    public void setBusy(boolean busy){
        this.bisy = busy;
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
        //System.out.println("OWON_SPE3051 run parse");
        //ToDo сделать в остальных
        lastAnswerBytes = MyUtilities.clearAsciiString(lastAnswerBytes);
        if(lastAnswerBytes.length > 0) {

            lastAnswer.setLength(0);
            if (commands.isKnownCommand(cmdToSend)) {
                answerValues = commands.getCommand(cmdToSend).getResult(lastAnswerBytes);
                hasAnswer = true;
                if (answerValues != null){
                    for (int i = 0; i < answerValues.getValues().length; i++) {
                        lastAnswer.append(answerValues.getValues()[i]);
                        lastAnswer.append(" ");
                    }
                }
                //System.out.println("OWON_SPE3051 done correct...[" + lastAnswer.toString() + "]...");
            }else {
                hasAnswer = true;
                for (int i = 0; i < lastAnswerBytes.length; i++) {
                    lastAnswer.append( (char) lastAnswerBytes[i]);
                }
                //System.out.println("OWON_SPE3051 Cant create answers obj");
            }


        }else{
            //System.out.println("OWON_SPE3051 empty received");
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
                        "F", "F - запрос измеренного значения температуры и давления. ",
                        (response) -> {
                            answerValues = null;
                            //System.out.println("Proceed MEAS:CURR?");
                            //String example = "5.229";
                            if (response.length > 1 && response.length < 95) {

                                double outputState = -1.0;
                                StringBuilder sb = new StringBuilder();
                                for (byte b : response) {
                                    sb.append((char) b);
                                }
                                sb.replace(0, 1, "");
                                System.out.println(sb);
                                StringBuilder tmpSb = new StringBuilder();
                                for (int i = 0; i < 5; i++) {
                                    if(sb.charAt(i) > 47 && sb.charAt(i) < 58) {
                                        tmpSb.append(sb.charAt(i));
                                    }
                                }

                                boolean isOk = true;
                                double termOne = 0;
                                double pressOne = 0;
                                double termTwo = 0;
                                int serialNum = 0;
                                if(tmpSb.length() == 5){
                                    System.out.print("termOne " + tmpSb);
                                    System.out.println(" is Correct");
                                    termOne = Double.parseDouble(tmpSb.toString()) / 100;
                                }else{
                                    System.out.print("termOne " + tmpSb);
                                    System.out.println(" is wrong");
                                    isOk = false;
                                }

                                if(isOk){
                                    tmpSb.setLength(0);
                                    for (int i = 6; i < 12; i++) {
                                        if(sb.charAt(i) > 47 && sb.charAt(i) < 58) {
                                            tmpSb.append(sb.charAt(i));
                                        }
                                    }
                                }
                                if(tmpSb.length() == 5){
                                    System.out.print("pressOne " + tmpSb);
                                    System.out.println(" is Correct");
                                    pressOne = Double.parseDouble(tmpSb.toString()) / 10;
                                }else{
                                    System.out.print("pressOne " + tmpSb);
                                    System.out.println(" is wrong");
                                    isOk = false;
                                }

                                if(isOk){
                                    tmpSb.setLength(0);
                                    for (int i = 12; i < 17; i++) {
                                        if(sb.charAt(i) > 47 && sb.charAt(i) < 58) {
                                            tmpSb.append(sb.charAt(i));
                                        }
                                    }
                                }
                                if(tmpSb.length() == 5){
                                    System.out.print("termTwo " + tmpSb);
                                    System.out.println(" is Correct");
                                    termTwo = Double.parseDouble(tmpSb.toString()) / 100;
                                }else{
                                    System.out.print("termTwo " + tmpSb);
                                    System.out.println(" is wrong");
                                    isOk = false;
                                }
                                if(isOk) {
                                    answerValues = new AnswerValues(4);
                                    answerValues.addValue(termOne, "C");
                                    answerValues.addValue(pressOne, "mmRg");
                                    answerValues.addValue(termTwo, "C");
                                    answerValues.addValue(serialNum, "S.N.");
                                    return answerValues;
                                }else{
                                    //System.out.println("Answer doesnt contain dot " + sb.toString());
                                }
                            } else {
                                //System.out.println("Wrong answer length " + response.length);
                            }
                            return answerValues;
                        }, 72)
        );
    }


}
