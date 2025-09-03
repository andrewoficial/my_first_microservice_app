package org.example.gui.mgstest.transport;

import org.apache.log4j.Logger;
import org.example.utilites.MyUtilities;
import org.hid4java.HidDevice;

import java.util.Arrays;



public class SomeHidController {
    private final Logger log = Logger.getLogger(SomeHidController.class);

    public int count = 0;

    public void resetCounter(){
        count = 0;
    }

    public int simpleSend(HidDevice device, byte[] data) {
        count++;
        if (device.isOpen()) {
            //log.info("Перед отправкой все еще открыто");
        } else {
            log.warn("Перед отправкой ЗАКРЫТО");
        }

        //log.info("Отправка пакета номер: " + count);
        log.info("Полезная нагрузка: " + MyUtilities.bytesToHex(data));

        byte reportId = data[0];
        byte[] msg = generateMessageConfiguredDropFirst(data);

        //log.info("Должно совпадать в сниффере с массивом ниже:");
        //printArrayLikeDeviceMonitor(generateMessageConfiguredUnmodified(data));

        int sent = device.write(msg, msg.length, reportId);
        log.info("Отправлено байт: " + sent);

        return sent;
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

    public byte[] generateMessageConfiguredDropFirst(byte[] data) {
        if (data.length > 64) {
            System.out.println("Слишком большое сообщение");
        }
        byte[] msg = new byte[64];
        Arrays.fill(msg, (byte) 0xcc);
        int counterLimit = Math.min(64, data.length);
        for (int i = 1; i < counterLimit; i++) {
            msg[i-1] = data[i];
        }
        //System.out.println("Создана посылка размером " + msg.length);
        return msg;
    }

    public byte[] generateMessageConfiguredUnmodified(byte[] data) {
        if (data.length > 64) {
            System.out.println("Слишком большое сообщение");
        }
        byte[] msg = new byte[64];
        Arrays.fill(msg, (byte) 0xcc);
        int counterLimit = Math.min(64, data.length);
        for (int i = 0; i < counterLimit; i++) {
            msg[i] = data[i];
        }
        //System.out.println("Создана посылка размером " + msg.length);
        return msg;
    }
}
