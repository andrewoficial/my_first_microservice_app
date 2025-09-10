package org.example.gui.mgstest.transport;

import org.apache.log4j.Logger;
import org.example.utilites.MyUtilities;
import org.hid4java.HidDevice;

import java.util.Arrays;



public class SomeHidController {
    private final Logger log = Logger.getLogger(SomeHidController.class);

    public int count = 0;


    /**
     *
     * @param device -  HID-устройство куда отправлять
     * @param data -  byte массив данных. От 1 до 64. Будет заполнен 00 до 64.
     */
    public void simpleSend(HidDevice device, byte[] data) {
        byte reportId = data[0];
        byte[] msg = generateMessageConfiguredDropFirst(data);
        int sent = device.write(msg, msg.length, reportId);
        log.info("Полезная нагрузка: " + MyUtilities.bytesToHex(data) + " остальное заполнено 00 до " + sent + " байт");
        //log.info(" Остальное заполнено 00 до " + sent + " байт");
    }

    /**
     *
     * @param device -  HID-устройство куда отправлять
     * @param data -  byte массив данных. От 1 до 64. Будет заполнен CC до 64.
     */
    public void simpleSendInitial(HidDevice device, byte[] data) {
        byte reportId = data[0];
        byte[] msg = generateMessageConfiguredDropFirstInitial(data);
        int sent = device.write(msg, msg.length, reportId);
        log.info("Полезная нагрузка: " + MyUtilities.bytesToHex(data) + " остальное заполнено CC до " + sent + " байт");
    }

    public void printArrayLikeDeviceMonitor(byte[] data) {
        if (data.length > 64) {
            log.warn("Большой массив на вывод");
        }

        for (int i = 0; i < data.length; i++) {
            int unsignedValue = data[i] & 0xFF;
            System.out.printf("%02X ", unsignedValue);
            if ((i + 1) % 16 == 0) {
                System.out.println();
            }
        }

        if (data.length % 16 != 0) {
            System.out.println();
        }
    }

    public byte[] generateMessageConfiguredDropFirst(byte[] data) {
        if (data.length > 64) {
            System.out.println("Слишком большое сообщение");
        }
        byte[] msg = new byte[64];
        Arrays.fill(msg, (byte) 0x00);
        int counterLimit = Math.min(64, data.length);
        for (int i = 1; i < counterLimit; i++) {
            msg[i-1] = data[i];
        }
        //System.out.println("Создана посылка размером " + msg.length);
        return msg;
    }

    public byte[] generateMessageConfiguredDropFirstInitial(byte[] data) {
        if (data.length > 64) {
            System.out.println("Слишком большое сообщение");
        }
        byte[] msg = new byte[64];
        Arrays.fill(msg, (byte) 0xCC);
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
