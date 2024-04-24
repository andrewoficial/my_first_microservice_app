package org.example.device;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import org.apache.log4j.Logger;
import org.example.gui.ChartWindow;
import org.example.services.AnswerValues;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

public class ERSTEVAK_MTP4D implements SerialPortDataListener, SomeDevice {
    private static final Logger log = Logger.getLogger(ERSTEVAK_MTP4D.class);
    private final SerialPort comPort;
    private volatile boolean hasAnswer = false;
    private volatile StringBuilder lastAnswer;

    private volatile String lastValue;


    private boolean knownCommand = false;

    private  volatile boolean hasValue;

    //Loop Vars
    private int timeout = 0;

    private int received = 0;
    private long millisLimit = 5000L;
    private long millisDela = 0L;
    private long millisPrev = System.currentTimeMillis();
    private long value = 0;
    private long degree = 0;
    private double val;

    //For JUnits
    private StringBuilder strToSend;
    private String deviceAnswer;

    public ERSTEVAK_MTP4D(SerialPort port){
        log.info("Создан объект протокола ERSTEVAK_MTP4D");
        this.comPort = port;
        this.enable();
    }

    public ERSTEVAK_MTP4D(String inpString){
        log.info("Создан объект протокола ERSTEVAK_MTP4D (виртуализация)");
        comPort = null;
    }
    public void enable() {
        comPort.openPort();
        comPort.flushDataListener();
        comPort.removeDataListener();
        int timeout = 25;// !!!
        comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 15, 10);
        if(comPort.isOpen()){
            log.info("Порт открыт, задержки выставлены");
        }
        millisDela = 0L;
    }

    @Override
    public int getListeningEvents() {
        log.info("return Listening Events");
        return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
    }

    @Override
    public void serialEvent(SerialPortEvent event) {
        byte[] newData = event.getReceivedData();
        log.info("Received data via eventListener on ERSTEVAK_MTP4D of size: " + newData.length);
        for (byte newDatum : newData) System.out.print((char) newDatum);
        //System.out.println("\n");
        hasAnswer = true;
    }

    public void sendData(String data){
        //System.out.println("sendData[" + data + "]");
        knownCommand = isKnownCommand(data);
        byte[] buffer = new byte[data.length() + 1];
        for (int i = 0; i < data.length(); i++) {
            buffer[i] = (byte) data.charAt(i);
        }
        buffer [data.length()] = 13;//CR
        if (comPort == null){ //For Tests
            strToSend = new StringBuilder();
            for (byte b : buffer) {
                strToSend.append((char)b);
            }
            return;
        }
        comPort.writeBytes(buffer, buffer.length);
        //comPort.bytesAwaitingWrite();
        this.loop();
    }
    public StringBuilder getForSend(){
        return strToSend;
    }

    public void setReceived(String answer){
        this.received = answer.length();
        deviceAnswer = answer;
        this.loop();
    }

    private void loop() {
        if(comPort != null){
            received = comPort.bytesAvailable();
            millisPrev = System.currentTimeMillis();
        }

        while((received == 0) && (millisLimit > millisDela)){
            millisDela = System.currentTimeMillis() - millisPrev;
            received = comPort.bytesAvailable();
            try {
                Thread.sleep(15);
            } catch (InterruptedException e) {
                //throw new RuntimeException(e);
            }
        }

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
                log.trace("Ответ правильный " + lastAnswer.toString());
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
                System.out.println(val);
                //lastValue = String.valueOf(val);
            }else{
                log.trace("Ответ с ошибкой " + lastAnswer.toString());
                hasValue = false;
            }
        }
    }

    private boolean isKnownCommand(String cmd){
        return cmd != null && cmd.contains("M^") && cmd.length() == ("001M^".length());
    }
    private boolean isCorrectAnswer(){
        if((lastAnswer.length() == 11 || lastAnswer.length() == 12 || lastAnswer.length() == 13)){
            //ToDo add CRC
            return true;
        }
        return false;
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
        return hasValue;
    }

    public AnswerValues getValues(){
        AnswerValues valToSend =  new AnswerValues(1);
        valToSend.addValue(val, "bar");
        return valToSend;
    }
}
