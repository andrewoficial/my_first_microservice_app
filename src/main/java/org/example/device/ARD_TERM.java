package org.example.device;

import com.fazecast.jSerialComm.SerialPort;
import lombok.Setter;
import org.apache.log4j.Logger;
import org.example.services.AnswerValues;

import static org.example.utilites.MyUtilities.*;

public class ARD_TERM implements SomeDevice {
    private volatile boolean busy = false;
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
    private final long millisLimit = 800;
    private final long repeatWaitTime = 5;
    private final long millisPrev = System.currentTimeMillis();
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
    public int getMillisReadLimit() {
        return 150;
    }

    @Override
    public int getMillisWriteLimit() {
        return 150;
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
        //ToDo сделать в остальных
        //lastAnswerBytes = MyUtilities.clearAsciiString(lastAnswerBytes);
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

    @Override
    public int getExpectedBytes(){
            return commands.getExpectedBytes(cmdToSend);
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
                                boolean messagIsValid = false;
                                if( ! checkStructureForF(response)){
                                    log.info("Не пройдена проверка по признакам длинны, первого символа, табуляций и последнего символа");
                                    StringBuilder sb = new StringBuilder();
                                    for (byte b : response) {
                                        //sb.append((char) b);
                                        sb.append("[");
                                        sb.append(b);
                                        sb.append("], ");
                                    }
                                    log.warn("Рассматривал массив длинною " + response.length + " " + sb.toString());
                                    return null;
                                }else{
                                    messagIsValid = true;
                                }
                                if (response[70] != calculateCRCforF(response)) {
                                    log.info("ERROR CRC for F");
                                    log.info("Expected CRC F " + calculateCRCforF(response) + " received " + response[70] + " ARD_TERM");
                                    StringBuilder sb = new StringBuilder();
                                    for (byte b : response) {
                                        //sb.append((char) b);
                                        sb.append("[");
                                        sb.append(b);
                                        sb.append("], ");
                                    }
                                    log.warn("Рассматривал массив длинною " + response.length + " " + sb.toString());
                                    return null;

                                }

                                double outputState = -1.0;
                                StringBuilder sb = new StringBuilder();
                                for (byte b : response) {
                                    sb.append((char) b);
                                }
                                sb.replace(0, 1, "");
                                //log.info("F answer " + sb.toString());
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
                                    //System.out.print("termOne " + tmpSb);
                                    //System.out.println(" is Correct");
                                    try {
                                        termOne = Double.parseDouble(tmpSb.toString()) / 100;
                                        isOk = true;
                                    }catch (NumberFormatException e){
                                        log.warn("NumberFormatException" +  e.getMessage());
                                        isOk = false;
                                    }
                                }else {
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
                                    //System.out.print("pressOne " + tmpSb);
                                    //System.out.println(" is Correct");
                                    try {
                                        pressOne = Double.parseDouble(tmpSb.toString()) / 10;
                                        isOk = true;
                                    }catch (NumberFormatException e){
                                        log.warn("NumberFormatException" +  e.getMessage());
                                        isOk = false;
                                    }
                                }else{
                                    //System.out.print("pressOne " + tmpSb);
                                    //System.out.println(" is wrong");
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
                                    try {
                                        termTwo = Double.parseDouble(tmpSb.toString()) / 100;
                                        isOk = true;
                                    }catch (NumberFormatException e){
                                        log.warn("NumberFormatException" +  e.getMessage());
                                        isOk = false;
                                    }

                                }else{

                                    isOk = false;
                                }


                                if(isOk){
                                    tmpSb.setLength(0);
                                    for (int i = 60; i < 67; i++) {
                                        if(sb.charAt(i) > 47 && sb.charAt(i) < 58) {
                                            tmpSb.append(sb.charAt(i));
                                        }
                                    }
                                }
                                if(true){
                                    try {
                                        serialNum = Integer.parseInt(tmpSb.toString());
                                        isOk = true;
                                    }catch (NumberFormatException e){
                                        log.warn("NumberFormatException" +  e.getMessage() + " " + tmpSb.toString());
                                        isOk = false;
                                    }

                                }else{

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
                            return answerValues;
                        }, 73)
        );
    }


}
