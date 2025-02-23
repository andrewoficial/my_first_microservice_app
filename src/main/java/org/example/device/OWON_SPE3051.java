package org.example.device;

import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;

import com.fazecast.jSerialComm.SerialPort;
import lombok.Setter;
import org.apache.log4j.Logger;
import org.example.services.AnswerValues;
import org.example.utilites.MyUtilities;

import java.nio.charset.Charset;

public class OWON_SPE3051 implements SomeDevice {
    private volatile boolean bisy = false;
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
    private final long millisLimit = 150;
    private final long repeatWaitTime = 60;
    private final long millisPrev = System.currentTimeMillis();
    private final Charset charset = Charset.forName("Cp1251");
    private final CharsetDecoder decoder = charset.newDecoder();
    private final CharBuffer charBuffer = CharBuffer.allocate(512);  // Предполагаемый максимальный размер
    private static AnswerValues answerValues = new AnswerValues(0);
    String cmdToSend;


    public OWON_SPE3051(SerialPort port){
        log.info("Создан объект протокола OWON_SPE3051");
        this.comPort = port;
        this.enable();
    }

    public OWON_SPE3051(){
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
    public int getMillisReadLimit() {
        return 350;
    }

    @Override
    public int getMillisWriteLimit() {
        return 350;
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
            return true;
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
        lastAnswerBytes = MyUtilities.clearAsciiString(lastAnswerBytes);
        if(lastAnswerBytes.length > 0) {

            lastAnswer.setLength(0);
            if (commands.isKnownCommand(cmdToSend)) {
                answerValues = commands.getCommand(cmdToSend).getResult(lastAnswerBytes);
                hasAnswer = true;
                for (int i = 0; i < answerValues.getValues().length; i++) {
                    lastAnswer.append(answerValues.getValues()[i]);
                    lastAnswer.append(" ");
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
                        "MEAS:CURR?", "MEAS:CURR? - запрос измеренного значения силы тока. ",
                        (response) -> {
                            answerValues = null;
                            //System.out.println("Proceed MEAS:CURR?");
                            //String example = "5.229";
                            if (response.length > 1 && response.length < 8) {

                                double outputState = -1.0;
                                StringBuilder sb = new StringBuilder();
                                for (byte b : response) {
                                    sb.append((char) b);
                                }
                                if(sb.toString().contains(".")) {
                                    try {
                                        outputState = Double.parseDouble(sb.toString());
                                    } catch (NumberFormatException e) {
                                        System.out.println(e.getMessage());
                                        outputState = -2;
                                    }


                                    answerValues = new AnswerValues(1);
                                    answerValues.addValue(outputState, "A");
                                    return answerValues;
                                }else{
                                    //System.out.println("Answer doesnt contain dot " + sb.toString());
                                }
                            } else {
                                //System.out.println("Wrong answer length " + response.length);
                            }
                            return answerValues;
                        })
        );
        commands.addCommand(
                new SingleCommand(
                        "MEAS:VOLT?", "MEAS:VOLT? - запрос измеренного значения напряжения. ",
                        (response) -> {
                            answerValues = null;
                            //System.out.println("Proceed MEAS:VOLT?");
                            //String example = "5.229";
                            if (response.length > 1 && response.length < 8) {

                                    double outputState = -1.0;
                                    StringBuilder sb = new StringBuilder();
                                    for (byte b : response) {
                                        sb.append((char) b);
                                    }
                                    if(sb.toString().contains(".")) {
                                        try {
                                            outputState = Double.parseDouble(sb.toString());
                                        } catch (NumberFormatException e) {
                                            System.out.println(e.getMessage());
                                            outputState = -2;
                                        }


                                        answerValues = new AnswerValues(1);
                                        answerValues.addValue(outputState, "V");
                                        return answerValues;
                                    }else{
                                        //System.out.println("Answer doesnt contain dot " + sb.toString());
                                    }
                            } else {
                                //System.out.println("Wrong answer length " + response.length);
                            }
                            return answerValues;
                        })
        );
        commands.addCommand(
                new SingleCommand(
                        "OUTPut?", "OUTPut? - запрос заданного напряжения. ",
                        (response) -> {
                            answerValues = null;
                            //System.out.println("Proceed OUTPut");
                            //String example = "ON\n";
                            if (response.length > 1 && response.length < 6) {
                                if (response[0] == 'O') {
                                    int outputState = -1;
                                    StringBuilder sb = new StringBuilder();
                                    for (byte b : response) {
                                        sb.append((char) b);
                                    }
                                    String rsp = sb.toString();
                                    //System.out.println("Asw value " + rsp);
                                    //log.debug("Parse " + rsp);
                                    try {
                                        if(sb.toString().contains("OFF")){
                                            System.out.println("OOOOOOOFFFFFFFFF");
                                            outputState = 0;
                                        }else if(sb.toString().contains("ON")){
                                            System.out.println("OOOONNNN");
                                            outputState = 1;
                                        }else{
                                            System.out.println("???" + sb.toString());
                                            outputState = -2;
                                        }
                                    } catch (NumberFormatException e) {
                                        System.out.println(e.getMessage());
                                        outputState = -3;
                                    }


                                    answerValues = new AnswerValues(1);
                                    answerValues.addValue(outputState, "bool");
                                    return answerValues;
                                } else {
                                    System.out.println("Wrong O position  ");
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
