package org.example.device;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import org.example.services.AnswerValues;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class EDWARDS_D397_00_000 implements SerialPortDataListener, SomeDevice {
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
    private double value = 0;
    private long degree = 0;

    public EDWARDS_D397_00_000(SerialPort port){
        System.out.println("Создан объект протокола ERSTEVAK_MTP4D");
        this.comPort = port;
        this.enable();
    }

    public void enable() {
        comPort.openPort();
        comPort.flushDataListener();
        comPort.removeDataListener();
        int timeout = 25;// !!!
        comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 15, 10);
        if(comPort.isOpen()){
            System.out.println("Порт открыт, задержки выставлены");
        }
        millisDela = 0L;
    }

    @Override
    public int getListeningEvents() {
        System.out.println("return Listening Events");
        return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
    }

    @Override
    public void serialEvent(SerialPortEvent event) {
        byte[] newData = event.getReceivedData();
        System.out.println("Received data via eventListener on ERSTEVAK_MTP4D of size: " + newData.length);
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
        buffer [data.length()] = 13;
        comPort.writeBytes(buffer, buffer.length);
        //comPort.bytesAwaitingWrite();
        this.loop();
    }


    private void loop() {
        received = comPort.bytesAvailable();
        millisPrev = System.currentTimeMillis();
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
            //System.out.println("Начинаю разбор посылки длиною " + received);
            byte[] buffer = new byte[comPort.bytesAvailable()];
            comPort.readBytes(buffer, comPort.bytesAvailable());

            lastAnswer = new StringBuilder(new String(buffer));
            hasAnswer = true;
            if(knownCommand && lastAnswer.length() > 11){
                value = 0;
                degree = 0;
                int firstPart = lastAnswer.lastIndexOf("V913 ")+5;
                //System.out.println(firstPart);
                try{
                    value = Double.parseDouble(lastAnswer.substring(firstPart, lastAnswer.indexOf(";")));
                    //System.out.println("Parsed");
                } catch (NumberFormatException e) {
                    //throw new RuntimeException(e);
                    //System.out.println("Cant parse");
                    hasValue = false;
                    return;
                }
            hasValue = true;
                //System.out.println(value);
            lastValue = String.valueOf(value);
            }else{
                hasValue = false;
            }
        }
    }

    private boolean isKnownCommand(String cmd){
        return cmd != null && cmd.contains("913") && ((cmd.length() == ("#01:01?V00913".length()) || cmd.length() == ("?V00913".length())));
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
        AnswerValues val =  new AnswerValues(1);
        val.addValue((double) value, "bar");
        return val;
    }
}
