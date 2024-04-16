package org.example.device;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.example.Main.comPorts;

public class IGM_10 implements SerialPortDataListener, SomeDevice {
    private final SerialPort comPort;
    private volatile boolean hasAnswer = false;
    private volatile StringBuilder lastAnswer;
    public IGM_10(SerialPort port){
        System.out.println("Создан объект протокола ИГМ-10");
        this.comPort = port;
        this.enable();
    }

    public void enable() {
        comPort.openPort();
        comPort.flushDataListener();
        comPort.removeDataListener();
        int timeout = 15000 - comPort.getBaudRate();
        comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, timeout, timeout);
        System.out.println("Порт открыт, задержки выставлены");
    }

    @Override
    public int getListeningEvents() {
        System.out.println("return Listening Events");
        //return SerialPort.LISTENING_EVENT_DATA_RECEIVED;
        return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;

    }

    @Override
    public void serialEvent(SerialPortEvent event) {
        byte[] newData = event.getReceivedData();
        System.out.println("Received data via eventListener on IGM_10 of size: " + newData.length);
        for (byte newDatum : newData) System.out.print((char) newDatum);
        //System.out.println("\n");
        hasAnswer = true;
    }

    public void sendData(String data){
        //System.out.println("sendData: [" + data + "]");
        data = data + '\n';

        Charset charset = StandardCharsets.US_ASCII;
        byte[] buffer = data.getBytes(charset);
        comPort.writeBytes(buffer, buffer.length);
        this.loop();
    }


    private void loop() {
        int errCount = 0;
        int errLimit = 5;
        int received = comPort.bytesAvailable();
        long millisLimit = 5000L;
        long millisDela = 0L;
        long millisPrev = System.currentTimeMillis();
        while((received == 0) && (millisLimit > millisDela)){
            millisDela = System.currentTimeMillis() - millisPrev;
            received = comPort.bytesAvailable();
            try {
                Thread.sleep(Math.min((millisLimit / 4), 200L));
            } catch (InterruptedException e) {
                //throw new RuntimeException(e);
            }
        }
            if(received > 0) {
                byte[] buffer = new byte[comPort.bytesAvailable()];
                comPort.readBytes(buffer, comPort.bytesAvailable());
                //for (int i = 0; i < buffer.length; i++) {
                    //System.out.println(buffer[i] + "-");
                //}
                //lastAnswer = new String(buffer, StandardCharsets.US_ASCII);

                try {

                    lastAnswer = new StringBuilder(new String(buffer, "Cp1251"));
                } catch (UnsupportedEncodingException e) {
                    lastAnswer = new StringBuilder(new String(buffer));
                }
                hasAnswer = true;
            }
        //System.out.println("Set flags " + hasAnswer + " received " + received);
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
        return false;
    }

    public String getValue(){
        return null;
    }
}
