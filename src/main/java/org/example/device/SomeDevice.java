
package org.example.device;

import com.fazecast.jSerialComm.SerialPort;
import org.apache.log4j.Logger;
import org.example.services.AnswerValues;
import org.example.utilites.CommandListClass;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

import static org.example.utilites.MyUtilities.bytesToHex;

public interface SomeDevice {


    public CommandListClass commandList = null;
    byte [] getStrEndian ();
    SerialPort getComPort ();
    boolean isKnownCommand();

    int getReceivedCounter();
    void setReceivedCounter(int cnt);
    long getMillisPrev();
    long getMillisLimit();
    long getRepeatWaitTime();

    void setLastAnswer(byte [] ans);

    StringBuilder getEmulatedAnswer();
    void setEmulatedAnswer (StringBuilder sb);

    void setHasAnswer(boolean hasAnswer);
    boolean enable();
    boolean isBusy();
    String cmdToSend = "";
    void setBusy(boolean busy);
    void setCmdToSend(String str);
    Integer getTabForAnswer();
    CommandListClass commands = null;
    default CommandListClass getCommandListClass(){
        return this.commands;
    }
    static final Logger log = Logger.getLogger(SomeDevice.class);
    default void sendData(String data, byte [] strEndian, SerialPort comPort, boolean knownCommand, int buffClearTimeLimit, SomeDevice device){
        if(device.isBusy()){
            log.warn("Попытка записи при активном соединении");
            return;
        }else {
            device.setBusy(true);
        }
        setCmdToSend(data);
        //comPort.flushDataListener();
        //log.info("  Выполнено flushDataListener ");

        //comPort.removeDataListener();
        //log.info("  Выполнено removeDataListener ");
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
        device.setBusy(false);
    }

    default void receiveData(SomeDevice device) {
        if (device.isBusy()) {
            log.warn("Попытка чтения при активном соединении");
            return;
        }
        device.setBusy(true);
        log.info("  Начинаю receiveData ");

        SerialPort comPort = device.getComPort();
        long startTime = System.currentTimeMillis();
        long timeout = device.getMillisLimit();
        int expectedBytes = device.getExpectedBytes();
        ByteArrayOutputStream receivedData = new ByteArrayOutputStream();

        if (comPort != null) {
            comPort.flushDataListener();
            comPort.removeDataListener();
            try {
                log.info("Собираю данные. Время ожидания ответа: " + timeout + "мс, ожидаемые байты: " + expectedBytes);
                while ((System.currentTimeMillis() - startTime) < timeout && receivedData.size() < expectedBytes) {
                    int available = comPort.bytesAvailable();
                    if (available > 0) {
                        byte[] buffer = new byte[available];
                        int readBytes = comPort.readBytes(buffer, buffer.length);
                        if (readBytes > 0) {
                            receivedData.write(buffer, 0, readBytes);
                            log.info("Добавлено {"+  readBytes +"} байт. Всего: {"+  receivedData.size()+"} Ожидается {"+expectedBytes+"}");
                            // Сброс таймера при поступлении данных
                            startTime = System.currentTimeMillis();
                        }
                    } else {
                        Thread.sleep(device.getRepeatWaitTime());
                    }
                }
            } catch (InterruptedException e) {
                log.error("Ошибка при чтении данных: ", e);
            } finally {
                log.info("Освобождение буфера");
                comPort.flushIOBuffers();
            }
        }

        byte[] responseBytes = receivedData.toByteArray();
        if (responseBytes.length > 0) {
            device.setLastAnswer(responseBytes);
            device.setReceivedCounter(responseBytes.length);
            device.setHasAnswer(true);
        } else {
            device.setLastAnswer(new byte[0]);
            device.setHasAnswer(false);
        }

        if (log.isInfoEnabled()) {
            String asciiResponse = new String(responseBytes).trim();
            String hexResponse = bytesToHex(responseBytes);
            log.info("Parse answer ASCII [{ " + asciiResponse + " }]");
            log.info("Parse answer HEX [{ " + hexResponse + " }]");
        }

        device.setBusy(false);
    }



    void parseData();

    boolean hasAnswer();
    String getAnswer();

    boolean hasValue();
    AnswerValues getValues();

    default int getExpectedBytes() {
        // Возвращает ожидаемое количество байт. Например:
        return 50; // Или любое значение, зависящее от устройства.
    }
}
