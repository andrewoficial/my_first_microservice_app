package org.example.gui.mgstest;

import org.apache.log4j.Logger;
import org.hid4java.HidDevice;

import java.util.Arrays;

public class MgsCommander {
    private Logger log = Logger.getLogger(MgsCommander.class);

    //Generate message, filled with 0xcc without cutting
    public byte[] generateMessageInitial(byte[] data) {
        if (data.length > 64) {
            log.warn("Слишком большое сообщение");
        }
        byte[] msg = new byte[64];
        Arrays.fill(msg, (byte) 0xcc);
        int counterLimit = Math.min(64, data.length);
        for (int i = 0; i < counterLimit; i++) {
            msg[i] = data[i];
        }
        return msg;
    }

    //Generate message, filled with 0xcc without cutting
    public byte[] generateMessageInitialDropFirst(byte[] data) {
        if (data.length > 64) {
            log.warn("Слишком большое сообщение");
        }
        byte[] msg = new byte[64];
        Arrays.fill(msg, (byte) 0xcc);
        int counterLimit = Math.min(64, data.length);
        for (int i = 1; i < counterLimit; i++) {
            msg[i-1] = data[i];
        }
        return msg;
    }

    //Generate message, filled with 0xcc without cutting
    public byte[] generateMessageConfiguredDropFirst(byte[] data) {
        if (data.length > 64) {
            log.warn("Слишком большое сообщение");
        }
        byte[] msg = new byte[64];
        Arrays.fill(msg, (byte) 0xcc);
        int counterLimit = Math.min(64, data.length);
        for (int i = 1; i < counterLimit; i++) {
            msg[i-1] = data[i];
        }
        return msg;
    }

    public void generateMessageInitialSimpleAndSend(byte[] data, HidDevice device, Long sleep) {
        byte[] msg = generateMessageInitialDropFirst(data);
        log.info("Отправлено: " + msg.length);
        printArrayLikeDeviceMonitor(msg);
        log.info("Статус отправки " + device.write(msg, msg.length, data[0]));
        safetySleep(sleep);
    }

    public void generateMessageConfiguredSimpleAndSend(byte[] data, HidDevice device, Long sleep) {
        byte[] msg = generateMessageInitialDropFirst(data);
        log.info("Отправлено: " + msg.length);
        printArrayLikeDeviceMonitor(msg);
        log.info("Статус отправки " + device.write(msg, msg.length, data[0]));
        safetySleep(sleep);
    }

    //Generate message, filled with 0
    public byte[] generateMessageConfigured(byte[] data) {
        if (data.length > 64) {
            log.warn("Слишком большое сообщение");
        }
        byte[] msg = new byte[64];
        Arrays.fill(msg, (byte) 0);
        int counterLimit = Math.min(64, data.length);
        for (int i = 0; i < counterLimit; i++) {
            msg[i] = data[i];
        }
        return msg;
    }

    public void readAnswer(HidDevice device){
        byte[] buffer = new byte[64];
        int bytesRead = device.read(buffer, 600);
        log.info("Прочитано:" + bytesRead);
        printArrayLikeDeviceMonitor(buffer);
        Arrays.fill(buffer, (byte) 0);
    }

    public void printArrayLikeDeviceMonitor(byte[] data) {
        if (data.length > 64) {
            log.warn("Большой массив на вывод");
        }

        for (int i = 0; i < data.length; i++) {
            // Преобразование в беззнаковое значение (0-255)
            int unsignedValue = data[i] & 0xFF;
            // Форматирование в HEX с ведущим нулем
            System.out.printf("%02X ", unsignedValue);

            // Перенос строки каждые 16 байт (после текущего элемента)
            if ((i + 1) % 16 == 0) {
                System.out.println();
            }
        }

        // Добавляем перенос в конце, если последняя строка не полная
        if (data.length % 16 != 0) {
            System.out.println();
        }
    }

    private void safetySleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ex) {
            log.error("Sleep error " + ex.getMessage());
        }
    }
}
