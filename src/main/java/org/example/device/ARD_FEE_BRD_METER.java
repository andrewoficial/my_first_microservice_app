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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.example.Main.comPorts;

public class ARD_FEE_BRD_METER implements SerialPortDataListener, SomeDevice {
    private final SerialPort comPort;
    private volatile boolean hasAnswer = false;
    private volatile String lastAnswer = "";

    private boolean subsequentLaunchComWait = false;
    public ARD_FEE_BRD_METER(SerialPort port){
        //System.out.println("Create obj");
        this.comPort = port;
        this.enable();
    }

    public void enable() {
        //SerialPort comPort = SerialPort.getCommPort(portName);
        //System.out.println(comPort.getDescriptivePortName());
        if(! comPort.isOpen()){
            comPort.openPort();
        }
        comPort.flushDataListener();
        comPort.removeDataListener();
        //comPort.addDataListener(this);
        //System.out.println("open port and add listener");
        int timeout = 1000 - comPort.getBaudRate();
        comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, timeout, timeout);
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
        System.out.println("Received data of size: " + newData.length);
        for (byte newDatum : newData) System.out.print((char) newDatum);
        System.out.println("\n");
        hasAnswer = true;
        lastAnswer = "Something";
    }

    public void sendData(String data){
        //byte [] buffer = {0x46, 0x0D};
        System.out.println("sendData[" + data + "]");
        byte[] buffer = new byte[data.length() + 1];
        Charset charset = StandardCharsets.US_ASCII;
        for (int i = 0; i < data.length(); i++) {
            buffer[i] = (byte) data.charAt(i);
        }
        buffer [data.length()] = 13;
        for (int i = 0; i < buffer.length; i++) {
            System.out.print(buffer[i]);
        }
        int timeout = 3000 - comPort.getBaudRate();
        comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, timeout, timeout);
        comPort.writeBytes(buffer, buffer.length);
        comPort.bytesAwaitingWrite();

        comPort.flushDataListener();
        //comPort.removeDataListener();
        comPort.flushIOBuffers();
        this.loop();
    }


    private void loop() {
        int timeout = 3000 - comPort.getBaudRate();
        comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, timeout, timeout);
        int received = comPort.bytesAvailable();
        long millisLimit = 5000L;
        long millisDela = 0L;
        long millisPrev = System.currentTimeMillis();
        String currPart = "";
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
            //System.out.println("Received bytes " + received);
            byte[] buffer = new byte[received];
            comPort.readBytes(buffer, received);
            for (int i = 0; i < received; i++) {
                //System.out.println("Byte " + i + " is " + buffer[i]);
            }

            //System.out.println("Received Str 00 " + lastAnswer);
            //String utf8String = "ff";
            try {
                currPart = new String(buffer, "Cp1251");
                //System.out.println("Received Str 01 " + lastAnswer);
            } catch (UnsupportedEncodingException e) {
                currPart = new String(buffer);
                //System.out.println("Received Str 02 " + lastAnswer);
            }
            hasAnswer = true;
            //System.out.println("CurrPart " + currPart);
        }

        lastAnswer += currPart;
        System.out.println("Set flags ARD " + hasAnswer + " receive count " + received + " part " + currPart);
        System.out.println("Received Str " + lastAnswer);

        millisLimit = 200L;
        millisPrev = System.currentTimeMillis();
        millisDela = 0L;
        received = 0;
        while((received == 0) && (millisLimit > millisDela)){
            millisDela = System.currentTimeMillis() - millisPrev;
            received = comPort.bytesAvailable();
        }
        received = comPort.bytesAvailable();
        System.out.println("Que " + received);
        if(received > 0){
            loop();
        }

    }
    public String getAnswer(){


        String forReturn = new String(lastAnswer);
        /*
        forReturn = "T: " + Integer.parseInt(lastAnswer.substring(1,4)) + "," + lastAnswer.substring(4,6) +
                "   P:  " + Integer.parseInt(lastAnswer.substring(8,11)) + "," + lastAnswer.charAt(11) +
                "   H:  " + Integer.parseInt(lastAnswer.substring(13,18));

         */
        lastAnswer = null;
        hasAnswer = false;
        return forReturn;
    }

    public boolean hasAnswer(){
        System.out.println("return flags" + hasAnswer);
        return hasAnswer;
    }

    public boolean hasValue(){
        return false;
    }

    public String getValue(){
        return null;
    }
}
