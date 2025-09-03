//package org.example.gui.mgstest;
//
//import org.apache.log4j.Logger;
//import org.hid4java.HidDevice;
//
//import java.util.Arrays;
//
//public class MgsCommander {
//    private Logger log = Logger.getLogger(MgsCommander.class);
//
//    //Generate message, filled with 0xcc without cutting
//    public byte[] generateMessageInitial(byte[] data) {
//        if (data.length > 64) {
//            log.warn("Слишком большое сообщение");
//        }
//        byte[] msg = new byte[64];
//        Arrays.fill(msg, (byte) 0xcc);
//        int counterLimit = Math.min(64, data.length);
//        for (int i = 0; i < counterLimit; i++) {
//            msg[i] = data[i];
//        }
//        return msg;
//    }
//
//    //Generate message, filled with 0xcc without cutting
//    public static byte[] generateMessageInitialDropFirst(byte[] data) {
//        if (data.length > 64) {
//            System.out.println("Слишком большое сообщение");
//        }
//        byte[] msg = new byte[64];
//        Arrays.fill(msg, (byte) 0xcc);
//        int counterLimit = Math.min(64, data.length);
//        for (int i = 1; i < counterLimit; i++) {
//            msg[i-1] = data[i];
//        }
//        return msg;
//    }
//
//    //Generate message, filled with 0xcc without cutting

//
//    public static byte[] generateMessageConfiguredUnmofified(byte[] data) {
//        if (data.length > 64) {
//            System.out.println("Слишком большое сообщение");
//        }
//        byte[] msg = new byte[64];
//        Arrays.fill(msg, (byte) 0xcc);
//        int counterLimit = Math.min(64, data.length);
//        for (int i = 0; i < counterLimit; i++) {
//            msg[i] = data[i];
//        }
//        System.out.println("Создана посылка размером " + msg.length);
//        return msg;
//    }
//
//    public void generateMessageInitialSimpleAndSend(byte[] data, HidDevice device, Long sleep) {
//        byte[] msg = generateMessageInitialDropFirst(data);
//        log.info("Отправлено: " + msg.length);
//        printArrayLikeDeviceMonitor(msg);
//        log.info("Статус отправки " + device.write(msg, msg.length, data[0]));
//        safetySleep(sleep);
//    }
//
//    public void generateMessageConfiguredSimpleAndSend(byte[] data, HidDevice device, Long sleep) {
//        byte[] msg = generateMessageInitialDropFirst(data);
//        log.info("Отправлено: " + msg.length);
//        printArrayLikeDeviceMonitor(msg);
//        log.info("Статус отправки " + device.write(msg, msg.length, data[0]));
//        safetySleep(sleep);
//    }
//
//    //Generate message, filled with 0
//    public byte[] generateMessageConfigured(byte[] data) {
//        if (data.length > 64) {
//            log.warn("Слишком большое сообщение");
//        }
//        byte[] msg = new byte[64];
//        Arrays.fill(msg, (byte) 0);
//        int counterLimit = Math.min(64, data.length);
//        for (int i = 0; i < counterLimit; i++) {
//            msg[i] = data[i];
//        }
//        return msg;
//    }
//
//    public void readAnswer(HidDevice device){
//        byte[] buffer = new byte[64];
//        int bytesRead = device.read(buffer, 600);
//        log.info("Прочитано:" + bytesRead);
//        printArrayLikeDeviceMonitor(buffer);
//        Arrays.fill(buffer, (byte) 0);
//    }
//
//
//
//
//

//}
