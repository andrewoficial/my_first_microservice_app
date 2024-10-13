
package org.example.device;

import com.fazecast.jSerialComm.SerialPort;
import org.apache.log4j.Logger;
import org.example.services.AnswerValues;
import org.example.utilites.CommandListClass;

import java.util.ArrayList;

public interface SomeDevice {


    public CommandListClass commandList = null;
    byte [] getStrEndian ();
    SerialPort getComPort ();
    boolean isKnownCommand();

    int getReceivedCounter();
    void setReceivedCounter(int cnt);
    long getMillisPrev();
    long getMillisLimit();
    long getRepeatGetAnswerTimeDelay();

    void setLastAnswer(byte [] ans);

    StringBuilder getEmulatedAnswer();
    void setEmulatedAnswer (StringBuilder sb);
    int getBuffClearTimeLimit();
    void setHasAnswer(boolean hasAnswer);
    boolean enable();
    int getRepetCounterLimit();
    boolean isBisy();
    String cmdToSend = "";
    void setBisy(boolean bisy);
    void setCmdToSend(String str);
    Integer getTabForAnswer();
    CommandListClass commands = null;
    default CommandListClass getCommandListClass(){
        return this.commands;
    }
    static final Logger log = Logger.getLogger(SomeDevice.class);
    default void sendData(String data, byte [] strEndian, SerialPort comPort, boolean knownCommand, int buffClearTimeLimit, SomeDevice device){
        if(device.isBisy()){
            log.warn("Попытка записи при активном соединении");
            return;
        }else {
            device.setBisy(true);
        }
        setCmdToSend(data);
        comPort.flushDataListener();
        //log.info("  Выполнено flushDataListener ");

        comPort.removeDataListener();
        //log.info("  Выполнено removeDataListener ");
        setCmdToSend(data);
        byte[] buffer = new byte[data.length() + strEndian.length];
        for (int i = 0; i < data.length(); i++) {
            buffer[i] = (byte) data.charAt(i);
        }
        //buffer [data.length()] = 13;//CR
        System.arraycopy(strEndian, 0, buffer, data.length() , strEndian.length);

        if(log.isInfoEnabled()){
            //System.out.println("Логирование ответа в файл...");
            StringBuilder sb = new StringBuilder();
            for (byte b : buffer) {
                sb.append((char)b);
            }
            log.info("Parse request ASCII [" + sb.toString().trim() + "] ");
            log.info("Parse request HEX [" + buffer.toString() + "] ");
            sb = null;
        }
        comPort.flushIOBuffers();
        log.info("  Выполнено flushIOBuffers и теперь bytesAvailable " + comPort.bytesAvailable());
        comPort.writeBytes(buffer, buffer.length);
        log.info("  Завершена отправка данных");
        device.setBisy(false);
    }

    default void receiveData(SomeDevice device) {
        if(device.isBisy()){
            log.warn("Попытка чтения при активном соединении");
            return;
        }else {
            device.setBisy(true);
        }
        SerialPort comPort = device.getComPort();
        int received = 0;
        long millisPrev = 0;
        long millisDela = 0;
        long millisLimit = device.getMillisLimit();
        long repeatGetAnswerTimeDelay = device.getRepeatGetAnswerTimeDelay();
        ArrayList<Byte> receivedList = new ArrayList<>();
        if (comPort != null) {
            millisPrev = System.currentTimeMillis();
            boolean firstIteration = true;
            int lastReceived = received;

            int repeatLimit = device.getRepetCounterLimit();
            int repeat = 0;
            while (millisDela < millisLimit) {
                try {
                    Thread.sleep(repeatGetAnswerTimeDelay);//300
                } catch (InterruptedException e) {
                    //throw new RuntimeException(e);
                }
                millisDela = System.currentTimeMillis() - millisPrev;
                received = comPort.bytesAvailable();

                if (firstIteration && received > 0) {
                    firstIteration = false;
                    lastReceived = received;
                    //System.out.println("lastReceived " + lastReceived);
                } else if (lastReceived == received) {
                    repeat++;
                    //System.out.println("received " + received);
                    //System.out.println("repeat " + repeat);
                    if (repeat > repeatLimit) {
                        break;
                    }
                } else {
                    repeat = 0;
                    //System.out.println("repeat " + repeat);
                    lastReceived = received;
                }
                if (received > 0) {
                    byte[] buffer = new byte[comPort.bytesAvailable()];
                    comPort.readBytes(buffer, received);
                    for (int i = 0; i < buffer.length; i++) {
                        receivedList.add( buffer[i]);
                    }
                }

            }
            comPort.flushIOBuffers();

            log.info("Завершено получение данных");

            if(log.isInfoEnabled()){
                //System.out.println("Логирование ответа в файл...");
                StringBuilder sb = new StringBuilder();
                for (byte b : receivedList) {
                    sb.append((char)b);
                }
                log.info("Parse answer ASCII [" + sb.toString().trim() + "] ");
                log.info("Parse answer HEX " + receivedList.toString() + " ");
                sb = null;
            }


        }
        int bufSize = receivedList.size();
        if (bufSize > 0) {
            //System.out.println(bufSize);
            byte[] buffer = new byte[bufSize];
            for (int i = 0; i < bufSize; i++) {
                buffer[i] = (byte) receivedList.get(i);
            }
            device.setLastAnswer(buffer);
            device.setReceivedCounter(bufSize);
            device.setHasAnswer(true);
        } else {
            byte[] buffer = new byte[0];
            device.setLastAnswer(buffer);
            device.setHasAnswer(false);
        }
        log.trace("Завершена сборка ответа. Развемер: " + bufSize + " флаг hasAnswer " + device.hasAnswer());
        device.setBisy(false);
    }

    void parseData();

    boolean hasAnswer();
    String getAnswer();

    boolean hasValue();
    AnswerValues getValues();
}
