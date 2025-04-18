
package org.example.device;

import com.fazecast.jSerialComm.SerialPort;
import org.apache.log4j.Logger;
import org.example.services.AnswerValues;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.example.utilites.MyUtilities.bytesToHex;

public interface SomeDevice {

    public static final Logger log = Logger.getLogger(SomeDevice.class);
    public CommandListClass commandList = null;
    byte [] getStrEndian ();
    SerialPort getComPort ();
    boolean isKnownCommand();

    int getReceivedCounter();
    void setReceivedCounter(int cnt);
    long getMillisPrev();
    long getMillisLimit();
    int getMillisReadLimit();
    int getMillisWriteLimit();
    long getRepeatWaitTime();
    boolean busy = false;
    void setLastAnswer(byte [] ans);

    StringBuilder getEmulatedAnswer();
    void setEmulatedAnswer (StringBuilder sb);

    void setHasAnswer(boolean hasAnswer);
    boolean enable();
    //boolean isBusy();
    String cmdToSend = "";
    void setBusy(boolean busy);

    default boolean isBusy() {
        return busy;
    }
    void setCmdToSend(String str);
    Integer getTabForAnswer();
    CommandListClass commands = null;
    default CommandListClass getCommandListClass(){
        return this.commands;
    }

    default void sendData(String data, byte [] strEndian, SerialPort comPort, boolean knownCommand, int buffClearTimeLimit, SomeDevice device){
        setCmdToSend(data);

        byte[] buffer = new byte[data.length() + strEndian.length];
        for (int i = 0; i < data.length(); i++) {
            buffer[i] = (byte) data.charAt(i);
        }
        //buffer [data.length()] = 13;//CR
        System.arraycopy(strEndian, 0, buffer, data.length() , strEndian.length);

        if (!device.isBusy()) {
            device.setBusy(true);
            comPort.writeBytes(buffer, buffer.length);
            device.setBusy(false);
        }
        log.info("Порт всё еще открыт " + device.getComPort().isOpen());
        //comPort.flushIOBuffers();
    }

    default void receiveData(SomeDevice device) {

        //log.info("  Начинаю receiveData ");

        SerialPort comPort = device.getComPort();
        long startTime = System.currentTimeMillis();
        long timeout = device.getMillisLimit();
        int expectedBytes = device.getExpectedBytes();
        ByteArrayOutputStream receivedData = new ByteArrayOutputStream();

        if (comPort != null) {
            startTime = System.currentTimeMillis();
            long firstStartTime = System.currentTimeMillis();
            try {
                while ((System.currentTimeMillis() - startTime) < timeout && receivedData.size() < expectedBytes) {
                    int available = comPort.bytesAvailable();
                    if (available > 0) {
                        byte[] buffer = new byte[available];
                        int readBytes = comPort.readBytes(buffer, buffer.length);
                        if (readBytes > 0) {
                            receivedData.write(buffer, 0, readBytes);
                            //log.info("Добавлено {"+  readBytes +"} байт. Всего: {"+  receivedData.size()+"} Ожидается {"+expectedBytes+"}");
                            // Сброс таймера при поступлении данных
                            startTime = System.currentTimeMillis();
                        }
                    } else {
                        Thread.sleep(device.getRepeatWaitTime());
                    }
                }
                log.info("Собрал данные. Предельное время ожидания ответа: " + timeout + "мс, Ждал ответа: " + (System.currentTimeMillis() - firstStartTime) + "мс, время проверки появления данных "+ device.getRepeatWaitTime() +"мс, ожидаемые байты: " + expectedBytes + " получено байт: " + receivedData.size());
            } catch (InterruptedException e) {
                log.error("Ошибка при чтении данных: ", e);
            } finally {
                //log.info("Освобождение буфера");
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
//            String asciiResponse = new String(responseBytes).trim();
//            String hexResponse = bytesToHex(responseBytes);
//            log.info("Parse answer ASCII [{ " + asciiResponse + " }]");
//            log.info("Parse answer HEX [{ " + hexResponse + " }]");
        }

        device.setBusy(false);
    }


    default void sendAndReceiveData(String data, SomeDevice device){
        device.setCmdToSend(data);
        SerialPort deviceComPort = device.getComPort();
        if(deviceComPort == null) {
            log.warn("Попытка записи или чтения при отсутствии порта");
            return;
        }

        if(! deviceComPort.isOpen()){
            log.warn("Попытка записи или чтения при закрытом порте");
            return;
        }

        // Формирование данных для отправки
        byte[] buffer = new byte[data.length() + device.getStrEndian().length];
        for (int i = 0; i < data.length(); i++) {
            buffer[i] = (byte) data.charAt(i);
        }
        System.arraycopy(device.getStrEndian(), 0, buffer, data.length(), device.getStrEndian().length);

        deviceComPort.writeBytes(buffer, buffer.length);



        // Прием данных
        ByteArrayOutputStream receivedData = new ByteArrayOutputStream();
        long startTime = System.currentTimeMillis();
        long timeout = device.getMillisLimit();
        int expectedBytes = device.getExpectedBytes();
        long firstStartTime = System.currentTimeMillis();


        while ((System.currentTimeMillis() - startTime) < timeout && receivedData.size() < expectedBytes) {
            int available = deviceComPort.bytesAvailable();
            if (available > 0) {
                byte[] readBuffer = new byte[available];
                int readBytes = deviceComPort.readBytes(readBuffer, readBuffer.length);
                if (readBytes > 0) {
                    receivedData.write(readBuffer, 0, readBytes);
                    //log.info("Прочитано байт: " + readBytes + ", всего: " + receivedData.size());
                    startTime = System.currentTimeMillis(); // Сброс таймера
                }
            } else {
                try {
                    Thread.sleep(device.getRepeatWaitTime());
                } catch (InterruptedException e) {
                    log.error("Ошибка попытке уснуть во время приёма данных: ", e);
                }
            }
        }


        log.info("Прием данных завершен. Ожидаемые байты: " + expectedBytes + ", получено байт: " + receivedData.size() + " время получения " + (System.currentTimeMillis() - firstStartTime) + " мс, КОМ-ПОРТ" + deviceComPort.getSystemPortName() + " скорость порта " + deviceComPort.getBaudRate());

        //deviceComPort.flushIOBuffers(); // Освобождение буфера


        // Обработка полученных данных
        byte[] responseBytes = receivedData.toByteArray();
        if (responseBytes.length > 0) {
            device.setLastAnswer(responseBytes);
            device.setReceivedCounter(responseBytes.length);
            device.setHasAnswer(true);
        } else {
            device.setLastAnswer(new byte[0]);
            device.setHasAnswer(false);
        }
        log.info("Завершена установка ответа в объект прибора ");
    }
    void parseData();

    boolean hasAnswer();
    String getAnswer();

    boolean hasValue();
    AnswerValues getValues();

    default int getExpectedBytes() {
        // Возвращает ожидаемое количество байт. Например:
        return 600; // Или любое значение, зависящее от устройства.
    }
}
