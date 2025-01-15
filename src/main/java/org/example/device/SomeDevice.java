
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
    long getRepeatWaitTime();

    void setLastAnswer(byte [] ans);

    StringBuilder getEmulatedAnswer();
    void setEmulatedAnswer (StringBuilder sb);

    void setHasAnswer(boolean hasAnswer);
    boolean enable();
    //boolean isBusy();
    String cmdToSend = "";
    void setBusy(boolean busy);
    void setCmdToSend(String str);
    Integer getTabForAnswer();
    CommandListClass commands = null;
    default CommandListClass getCommandListClass(){
        return this.commands;
    }

    default void sendData(String data, byte [] strEndian, SerialPort comPort, boolean knownCommand, int buffClearTimeLimit, SomeDevice device){
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

        //comPort.flushIOBuffers();
        //log.info("  Выполнено flushIOBuffers и теперь bytesAvailable " + comPort.bytesAvailable());
        comPort.writeBytes(buffer, buffer.length);
        //log.info("  Завершена отправка данных");
        device.setBusy(false);
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


    default void sendAndReceiveData(String data, SomeDevice device) throws IOException {

        device.setBusy(true); // Устанавливаем состояние устройства как занятое
        device.setCmdToSend(data);
        SerialPort deviceComPort = device.getComPort();
        if(deviceComPort == null) {
            device.setBusy(false);
            log.warn("Попытка записи или чтения при отсутствии порта");
            return;
        }

        if(! deviceComPort.isOpen()){
            device.setBusy(false);
            log.warn("Попытка записи или чтения при закрытом порте");
            return;
        }


        //deviceComPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING , 100, 100);

        OutputStream outputStream = deviceComPort.getOutputStream();
        InputStream inputStream = deviceComPort.getInputStream();

        byte[] buffer = new byte[data.length() + device.getStrEndian().length];
        for (int i = 0; i < data.length(); i++) {
            buffer[i] = (byte) data.charAt(i);
        }
        System.arraycopy(device.getStrEndian(), 0, buffer, data.length(), device.getStrEndian().length);

        ByteArrayOutputStream receivedData = new ByteArrayOutputStream();
        long timeout = device.getMillisLimit();

        int expectedBytes = device.getExpectedBytes();
        long startTime = 0;
        long firstStartTime = 0;
        int byteRead;
        try {
            startTime = System.currentTimeMillis();
            firstStartTime = System.currentTimeMillis();
            //log.info("Подготовился к отправке" + data);

            outputStream.write(buffer);
            //Thread.sleep(50);
            int available;
            while ((System.currentTimeMillis() - startTime) < device.getMillisLimit() && receivedData.size() < expectedBytes) {
                while (inputStream.available() != 0) { // Считываем по одному байту
                    byteRead = inputStream.read();
                    receivedData.write(byteRead); // Добавляем байт в поток
                    //log.info("Прочитан байт: " + byteRead + ", всего: " + receivedData.size());
                    startTime = System.currentTimeMillis(); // Сброс таймера
                }

                if (inputStream.available() == 0) { // Если данных временно нет, ждём
                    Thread.sleep(device.getRepeatWaitTime());
                }
            }
            log.info("Собрал данные. Предельное время ожидания ответа: " + timeout + "мс, Ждал ответа: " + (System.currentTimeMillis() - firstStartTime) + "мс, время проверки появления данных "+ device.getRepeatWaitTime() +"мс, ожидаемые байты: " + expectedBytes + " получено байт: " + receivedData.size());

            byte[] responseBytes = receivedData.toByteArray();
            StringBuilder sb = new StringBuilder();
            for (byte b : responseBytes) {
                //sb.append((char) b);
                sb.append("[");
                sb.append(b);
                sb.append("], ");
            }
            //log.info("Было передано в объект прибора " + receivedData.size() + " массив" + sb.toString());
            if (responseBytes.length > 0) {
                device.setLastAnswer(responseBytes);
                device.setReceivedCounter(responseBytes.length);
                device.setHasAnswer(true);
            } else {
                device.setLastAnswer(new byte[0]);
                device.setHasAnswer(false);
            }

        } catch (IOException | InterruptedException e) {
            log.error("Ошибка при работе с потоками устройства: ", e);
        } finally {
            try {
                // Освобождаем ресурсы
                deviceComPort.getInputStream().close();
                deviceComPort.getOutputStream().close();
            } catch (IOException e) {
                log.error("Ошибка при закрытии потоков: ", e);
            }
            device.setBusy(false); // Устройство становится доступным
        }

    }
    void parseData();

    boolean hasAnswer();
    String getAnswer();

    boolean hasValue();
    AnswerValues getValues();

    default int getExpectedBytes() {
        // Возвращает ожидаемое количество байт. Например:
        return 55; // Или любое значение, зависящее от устройства.
    }
}
