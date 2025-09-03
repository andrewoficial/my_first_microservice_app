//package org.example.gui.mgstest;
//
//import org.apache.log4j.Logger;
//import org.example.utilites.MyUtilities;
//import org.hid4java.HidDevice;
//
//import java.util.Arrays;
//
//import static org.example.gui.mgstest.MgsCommander.generateMessageConfiguredDropFirst;
//import static org.example.gui.mgstest.MgsCommander.generateMessageConfiguredUnmofified;
//
//public class MgsCommandSequence {
//    private Logger log = Logger.getLogger(MgsCommandSequence.class);
//    private MgsCommander commander = new MgsCommander();
//    public void executeTestSequence(HidDevice device){
//        byte[] buffer = new byte[64];
//        byte reportId = 0x3;
//        byte[] msg = null;
//        int bytesRead = 0;
//
//        log.info("Отправка [1] сообщения:");
//        byte[] payload111 = {(byte) 0x02, (byte) 0xb2, (byte) 0xcc, (byte) 0xcc, (byte) 0xcc};
//        commander.generateMessageInitialSimpleAndSend(payload111, device, 500L);
//        commander.readAnswer(device);
//
//        log.info("Отправка [2] сообщения:");
//        byte[] payload2 = {(byte) 0x02, (byte) 0xb2, (byte) 0xcc, (byte) 0xcc, (byte) 0xcc};
//        commander.generateMessageInitialSimpleAndSend(payload2, device, 500L);
//        commander.readAnswer(device);
//
//        log.info("Отправка [3] сообщения:");
//        byte[] payload3 = {(byte) 0x02, (byte) 0xbd, (byte) 0xcc};
//        commander.generateMessageInitialSimpleAndSend(payload3, device, 500L);
//        commander.readAnswer(device);
//
//        log.info("Отправка [4] сообщения:");
//        byte[] payload4 = {(byte) 0x01, (byte) 0x55, (byte) 0xcc};
//        commander.generateMessageInitialSimpleAndSend(payload4, device, 500L);
//        commander.readAnswer(device);
//
//        log.info("Отправка [5] сообщения:");
//        byte[] payload5 = {(byte) 0x01, (byte) 0x02, (byte) 0x02, (byte) 0x01, (byte) 0x0d};
//        commander.generateMessageInitialSimpleAndSend(payload5, device, 500L);
//        commander.readAnswer(device);
//
//        log.info("Отправка [6] сообщения:");
//        byte[] payload6 = {(byte) 0x01, (byte) 0x04, (byte) 0x02, (byte) 0x02, (byte) 0x2b};
//        commander.generateMessageInitialSimpleAndSend(payload6, device, 500L);
//        commander.readAnswer(device);
//
//
//        log.info("Отправка [7] сообщения:");
//        byte[] payload7 = {(byte) 0x01, (byte) 0x02, (byte) 0x02, (byte) 0x00, (byte) 0x00};
//        commander.generateMessageInitialSimpleAndSend(payload7, device, 500L);
//        commander.readAnswer(device);
//
//
//        log.info("Отправка [777] сообщения:");
//        reportId = (byte) 0x01;
//        byte[] payload777 = {(byte) 0x01, (byte) 0x02, (byte) 0x02, (byte) 0x01, (byte) 0x0d};
//        //commander.generateMessageConfigured(payload777, device, 500L);
//        //commander.readAnswer(device);
//        msg = commander.generateMessageConfigured(payload777);
//        commander.printArrayLikeDeviceMonitor(msg);
//        log.info("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
//        safetySleep();
//
//        bytesRead = device.read(buffer, 600);
//        log.info("Прочитано в ответ: " + bytesRead);
//        log.info("Ответ:");
//        commander.printArrayLikeDeviceMonitor(buffer);
//        Arrays.fill(buffer, (byte) 0);
//
//
//        log.info("Отправка [8] сообщения:");
//        reportId = (byte) 0x01;
//        byte[] payload8 = {(byte) 0x04, (byte) 0x07, (byte) 0x02, (byte) 0x21};
//        msg = commander.generateMessageConfigured(payload8);
//        commander.printArrayLikeDeviceMonitor(msg);
//        log.info("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
//        safetySleep();
//
//        bytesRead = device.read(buffer, 600);
//        log.info("Прочитано в ответ: " + bytesRead);
//        log.info("Ответ:");
//        commander.printArrayLikeDeviceMonitor(buffer);
//        Arrays.fill(buffer, (byte) 0);
//
//
//        log.info("Отправка [9] сообщения:");
//        reportId = (byte) 0x01;
//        byte[] payload9 = {(byte) 0x04, (byte) 0x07, (byte) 0x02, (byte) 0x21, (byte) 0x01, (byte) 0x03, (byte) 0x11, (byte) 0xd1, (byte) 0x01};
//        msg = commander.generateMessageConfigured(payload9);
//        commander.printArrayLikeDeviceMonitor(msg);
//        log.info("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
//        safetySleep();
//
//        bytesRead = device.read(buffer, 600);
//        log.info("Прочитано в ответ: " + bytesRead);
//        log.info("Ответ:");
//        commander.printArrayLikeDeviceMonitor(buffer);
//        Arrays.fill(buffer, (byte) 0);
//
//
//        log.info("Отправка [10] сообщения:");
//        reportId = (byte) 0x01;
//        byte[] payload10 = {(byte) 0x04, (byte) 0x07, (byte) 0x02, (byte) 0x21, (byte) 0x02, (byte) 0x0d, (byte) 0x54, (byte) 0x02, (byte) 0x65};
//        msg = commander.generateMessageConfigured(payload10);
//        commander.printArrayLikeDeviceMonitor(msg);
//        log.info("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
//        safetySleep();
//
//        bytesRead = device.read(buffer, 600);
//        log.info("Прочитано в ответ: " + bytesRead);
//        log.info("Ответ:");
//        commander.printArrayLikeDeviceMonitor(buffer);
//        Arrays.fill(buffer, (byte) 0);
//
//
//        log.info("Отправка [11] сообщения:");
//        reportId = (byte) 0x01;
//        byte[] payload11 = {(byte) 0x04, (byte) 0x07, (byte) 0x02, (byte) 0x21, (byte) 0x03, (byte) 0x6e, (byte) 0x00};
//        msg = commander.generateMessageConfigured(payload11);
//        commander.printArrayLikeDeviceMonitor(msg);
//        log.info("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
//        safetySleep();
//
//        bytesRead = device.read(buffer, 600);
//        log.info("Прочитано в ответ: " + bytesRead);
//        log.info("Ответ:");
//        commander.printArrayLikeDeviceMonitor(buffer);
//        Arrays.fill(buffer, (byte) 0);
//
//
//        log.info("Отправка [12] сообщения:");
//        reportId = (byte) 0x01;
//        byte[] payload12 = {(byte) 0x04, (byte) 0x07, (byte) 0x02, (byte) 0x21, (byte) 0x04, (byte) 0x00, (byte) 0x01};
//        msg = commander.generateMessageConfigured(payload12);
//        commander.printArrayLikeDeviceMonitor(msg);
//        log.info("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
//        safetySleep();
//
//        bytesRead = device.read(buffer, 600);
//        log.info("Прочитано в ответ: " + bytesRead);
//        log.info("Ответ:");
//        commander.printArrayLikeDeviceMonitor(buffer);
//        Arrays.fill(buffer, (byte) 0);
//
//
//        log.info("Отправка [13] сообщения:");
//        reportId = (byte) 0x01;
//        byte[] payload13 = {(byte) 0x04, (byte) 0x07, (byte) 0x02, (byte) 0x21, (byte) 0x05, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0xfe};
//        msg = commander.generateMessageConfigured(payload13);
//        commander.printArrayLikeDeviceMonitor(msg);
//        log.info("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
//        safetySleep();
//
//        bytesRead = device.read(buffer, 600);
//        log.info("Прочитано в ответ: " + bytesRead);
//        log.info("Ответ:");
//        commander.printArrayLikeDeviceMonitor(buffer);
//        Arrays.fill(buffer, (byte) 0);
//
//
//        log.info("Отправка [14] сообщения:");
//        reportId = (byte) 0x01;
//        byte[] payload14 = {(byte) 0x04, (byte) 0x07, (byte) 0x02, (byte) 0x21, (byte) 0x06, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xbd};
//        msg = commander.generateMessageConfigured(payload14);
//        commander.printArrayLikeDeviceMonitor(msg);
//        log.info("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
//        safetySleep();
//
//        bytesRead = device.read(buffer, 600);
//        log.info("Прочитано в ответ: " + bytesRead);
//        log.info("Ответ:");
//        commander.printArrayLikeDeviceMonitor(buffer);
//        Arrays.fill(buffer, (byte) 0);
//
//
//        log.info("Отправка [15] сообщения:");
//        reportId = (byte) 0x01;
//        byte[] payload15 = {(byte) 0x04, (byte) 0x07, (byte) 0x02, (byte) 0x21, (byte) 0x03, (byte) 0x6e, (byte) 0x06, (byte) 0x00, (byte) 0x00};
//        msg = commander.generateMessageConfigured(payload15);
//        commander.printArrayLikeDeviceMonitor(msg);
//        log.info("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
//        safetySleep();
//
//        bytesRead = device.read(buffer, 600);
//        log.info("Прочитано в ответ: " + bytesRead);
//        log.info("Ответ:");
//        commander.printArrayLikeDeviceMonitor(buffer);
//        Arrays.fill(buffer, (byte) 0);
//
//
//        log.info("Отправка [16] сообщения:");
//        reportId = (byte) 0x01;
//        byte[] payload16 = {(byte) 0x04, (byte) 0x07, (byte) 0x02, (byte) 0x21, (byte) 0x00, (byte) 0xe1, (byte) 0x40, (byte) 0xff, (byte) 0x01};
//        msg = commander.generateMessageConfigured(payload16);
//        commander.printArrayLikeDeviceMonitor(msg);
//        log.info("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
//        safetySleep();
//
//        bytesRead = device.read(buffer, 600);
//        log.info("Прочитано в ответ: " + bytesRead);
//        log.info("Ответ:");
//        commander.printArrayLikeDeviceMonitor(buffer);
//        Arrays.fill(buffer, (byte) 0);
//
//
//        log.info("Отправка [17] сообщения:");
//        reportId = (byte) 0x01;
//        byte[] payload17 = {(byte) 0x02, (byte) 0x02};
//        msg = commander.generateMessageConfigured(payload17);
//        commander.printArrayLikeDeviceMonitor(msg);
//        log.info("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
//        safetySleep();
//
//        bytesRead = device.read(buffer, 600);
//        log.info("Прочитано в ответ: " + bytesRead);
//        log.info("Ответ:");
//        commander.printArrayLikeDeviceMonitor(buffer);
//        Arrays.fill(buffer, (byte) 0);
//
//
//        log.info("Отправка [18] сообщения:");
//        reportId = (byte) 0x01;
//        byte[] payload18 = {(byte) 0x02, (byte) 0x02, (byte) 0x01, (byte) 0x0D};
//        msg = commander.generateMessageConfigured(payload18);
//        commander.printArrayLikeDeviceMonitor(msg);
//        log.info("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
//        safetySleep();
//
//        bytesRead = device.read(buffer, 600);
//        log.info("Прочитано в ответ: " + bytesRead);
//        log.info("Ответ:");
//        commander.printArrayLikeDeviceMonitor(buffer);
//        Arrays.fill(buffer, (byte) 0);
//
//
//        log.info("Отправка [19] сообщения:");
//        reportId = (byte) 0x01;
//        byte[] payload19 = {(byte) 0x04, (byte) 0x04, (byte) 0x02, (byte) 0x23, (byte) 0x00, (byte) 0x07};
//        msg = commander.generateMessageConfigured(payload19);
//        commander.printArrayLikeDeviceMonitor(msg);
//        log.info("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
//        safetySleep();
//
//        bytesRead = device.read(buffer, 600);
//        log.info("Прочитано в ответ: " + bytesRead);
//        log.info("Ответ:");
//        commander.printArrayLikeDeviceMonitor(buffer);
//        Arrays.fill(buffer, (byte) 0);
//
//
//        log.info("Отправка [20] сообщения:");
//        reportId = (byte) 0x01;
//        byte[] payload20 = {(byte) 0x04, (byte) 0x04, (byte) 0x02, (byte) 0x23, (byte) 0x08, (byte) 0x07};
//        msg = commander.generateMessageConfigured(payload20);
//        commander.printArrayLikeDeviceMonitor(msg);
//        log.info("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
//        safetySleep();
//
//        bytesRead = device.read(buffer, 600);
//        log.info("Прочитано в ответ: " + bytesRead);
//        log.info("Ответ:");
//        commander.printArrayLikeDeviceMonitor(buffer);
//        Arrays.fill(buffer, (byte) 0);
//
//
//        log.info("Отправка [21] сообщения:");
//        reportId = (byte) 0x01;
//        byte[] payload21 = {(byte) 0x04, (byte) 0x04, (byte) 0x02, (byte) 0x23, (byte) 0x10, (byte) 0x07};
//        msg = commander.generateMessageConfigured(payload21);
//        commander.printArrayLikeDeviceMonitor(msg);
//        log.info("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
//        safetySleep();
//
//        bytesRead = device.read(buffer, 600);
//        log.info("Прочитано в ответ: " + bytesRead);
//        log.info("Ответ:");
//        commander.printArrayLikeDeviceMonitor(buffer);
//        Arrays.fill(buffer, (byte) 0);
//
//
//        log.info("Отправка [22] сообщения:");
//        reportId = (byte) 0x01;
//        byte[] payload22 = {(byte) 0x02, (byte) 0x02};
//        msg = commander.generateMessageConfigured(payload22);
//        commander.printArrayLikeDeviceMonitor(msg);
//        log.info("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
//        safetySleep();
//
//        bytesRead = device.read(buffer, 600);
//        log.info("Прочитано в ответ: " + bytesRead);
//        log.info("Ответ:");
//        commander.printArrayLikeDeviceMonitor(buffer);
//        Arrays.fill(buffer, (byte) 0);
//
//        longSleep();
//
//        log.info("==TurnOff device == Отправка [23] сообщения:");
//        reportId = (byte) 0x01;
//        byte[] payload23 = {(byte) 0x02, (byte) 0x02, (byte) 0x01, (byte) 0x0D};
//        msg = commander.generateMessageConfigured(payload23);
//        commander.printArrayLikeDeviceMonitor(msg);
//        log.info("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
//        safetySleep();
//
//        bytesRead = device.read(buffer, 600);
//        log.info("Прочитано в ответ: " + bytesRead);
//        log.info("Ответ:");
//        commander.printArrayLikeDeviceMonitor(buffer);
//        Arrays.fill(buffer, (byte) 0);
//
//        longSleep();
//
//        log.info("==repeat == Отправка [24] сообщения:");
//        reportId = (byte) 0x01;
//        byte[] payload24 = {(byte) 0x02, (byte) 0x02, (byte) 0x01, (byte) 0x0D};
//        msg = commander.generateMessageConfigured(payload24);
//        commander.printArrayLikeDeviceMonitor(msg);
//        log.info("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
//        safetySleep();
//
//        bytesRead = device.read(buffer, 600);
//        log.info("Прочитано в ответ: " + bytesRead);
//        log.info("Ответ:");
//        commander.printArrayLikeDeviceMonitor(buffer);
//        Arrays.fill(buffer, (byte) 0);
//
//        safetySleep();
//
//        log.info(" Отправка [25] сообщения:");
//        reportId = (byte) 0x01;
//        byte[] payload25 = {(byte) 0x04, (byte) 0x07, (byte) 0x02, (byte) 0x21};
//        msg = commander.generateMessageConfigured(payload25);
//        commander.printArrayLikeDeviceMonitor(msg);
//        log.info("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
//        safetySleep();
//
//        bytesRead = device.read(buffer, 600);
//        log.info("Прочитано в ответ: " + bytesRead);
//        log.info("Ответ:");
//        commander.printArrayLikeDeviceMonitor(buffer);
//        Arrays.fill(buffer, (byte) 0);
//
//        safetySleep();
//
//        log.info(" Отправка [26] сообщения:");
//        reportId = (byte) 0x01;
//        byte[] payload26 = {(byte) 0x04, (byte) 0x07, (byte) 0x02, (byte) 0x21, (byte) 0x01, (byte) 0x03, (byte) 0x16, (byte) 0xD1, (byte) 0x01};
//        msg = commander.generateMessageConfigured(payload26);
//        commander.printArrayLikeDeviceMonitor(msg);
//        log.info("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
//        safetySleep();
//
//        bytesRead = device.read(buffer, 600);
//        log.info("Прочитано в ответ: " + bytesRead);
//        log.info("Ответ:");
//        commander.printArrayLikeDeviceMonitor(buffer);
//        Arrays.fill(buffer, (byte) 0);
//
//
//        log.info(" Отправка [27] сообщения:");
//        reportId = (byte) 0x01;
//        byte[] payload27 = {(byte) 0x04, (byte) 0x07, (byte) 0x02, (byte) 0x21, (byte) 0x02, (byte) 0x12, (byte) 0x54, (byte) 0x02, (byte) 0x65};
//        msg = commander.generateMessageConfigured(payload27);
//        commander.printArrayLikeDeviceMonitor(msg);
//        log.info("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
//        safetySleep();
//
//        bytesRead = device.read(buffer, 600);
//        log.info("Прочитано в ответ: " + bytesRead);
//        log.info("Ответ:");
//        commander.printArrayLikeDeviceMonitor(buffer);
//        Arrays.fill(buffer, (byte) 0);
//
//        safetySleep();
//
//        log.info(" Отправка [28] сообщения:");
//        reportId = (byte) 0x01;
//        byte[] payload28 = {(byte) 0x04, (byte) 0x07, (byte) 0x02, (byte) 0x21, (byte) 0x03, (byte) 0x6E};
//        msg = commander.generateMessageConfigured(payload28);
//        commander.printArrayLikeDeviceMonitor(msg);
//        log.info("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
//        safetySleep();
//
//        bytesRead = device.read(buffer, 600);
//        log.info("Прочитано в ответ: " + bytesRead);
//        log.info("Ответ:");
//        commander.printArrayLikeDeviceMonitor(buffer);
//        Arrays.fill(buffer, (byte) 0);
//
//        safetySleep();
//
//        log.info(" Отправка [29] сообщения:");
//        reportId = (byte) 0x01;
//        byte[] payload29 = {(byte) 0x04, (byte) 0x07, (byte) 0x02, (byte) 0x21, (byte) 0x04, (byte) 0x00, (byte) 0x24, (byte) 0x00, (byte) 0x01};
//        msg = commander.generateMessageConfigured(payload29);
//        commander.printArrayLikeDeviceMonitor(msg);
//        log.info("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
//        safetySleep();
//
//        bytesRead = device.read(buffer, 600);
//        log.info("Прочитано в ответ: " + bytesRead);
//        log.info("Ответ:");
//        commander.printArrayLikeDeviceMonitor(buffer);
//        Arrays.fill(buffer, (byte) 0);
//
//        safetySleep();
//
//        log.info(" Отправка [30] сообщения:");
//        reportId = (byte) 0x01;
//        byte[] payload30 = {(byte) 0x04, (byte) 0x07, (byte) 0x02, (byte) 0x21, (byte) 0x05, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x01};
//        msg = commander.generateMessageConfigured(payload30);
//        commander.printArrayLikeDeviceMonitor(msg);
//        log.info("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
//        safetySleep();
//
//        bytesRead = device.read(buffer, 600);
//        log.info("Прочитано в ответ: " + bytesRead);
//        log.info("Ответ:");
//        commander.printArrayLikeDeviceMonitor(buffer);
//        Arrays.fill(buffer, (byte) 0);
//
//        safetySleep();
//
//        log.info(" Отправка [31] сообщения:");
//        reportId = (byte) 0x01;
//        byte[] payload31 = {(byte) 0x04, (byte) 0x07, (byte) 0x02, (byte) 0x21, (byte) 0x06, (byte) 0x1B, (byte) 0xDF, (byte) 0x05, (byte) 0xA5};
//        msg = commander.generateMessageConfigured(payload31);
//        commander.printArrayLikeDeviceMonitor(msg);
//        log.info("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
//        safetySleep();
//
//        bytesRead = device.read(buffer, 600);
//        log.info("Прочитано в ответ: " + bytesRead);
//        log.info("Ответ:");
//        commander.printArrayLikeDeviceMonitor(buffer);
//        Arrays.fill(buffer, (byte) 0);
//
//        safetySleep();
//
//        log.info(" Отправка [32] сообщения:");
//        reportId = (byte) 0x01;
//        byte[] payload32 = {(byte) 0x04, (byte) 0x07, (byte) 0x02, (byte) 0x21, (byte) 0x07, (byte) 0xFE, (byte) 0x00, (byte) 0x00, (byte) 0x00};
//        msg = commander.generateMessageConfigured(payload32);
//        commander.printArrayLikeDeviceMonitor(msg);
//        log.info("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
//        safetySleep();
//
//        bytesRead = device.read(buffer, 600);
//        log.info("Прочитано в ответ: " + bytesRead);
//        log.info("Ответ:");
//        commander.printArrayLikeDeviceMonitor(buffer);
//        Arrays.fill(buffer, (byte) 0);
//
//        safetySleep();
//
//        log.info(" Отправка [33] сообщения:");
//        reportId = (byte) 0x01;
//        byte[] payload33 = {(byte) 0x04, (byte) 0x07, (byte) 0x02, (byte) 0x21, (byte) 0x03, (byte) 0x6E, (byte) 0x0B, (byte) 0x00, (byte) 0x00};
//        msg = commander.generateMessageConfigured(payload33);
//        commander.printArrayLikeDeviceMonitor(msg);
//        log.info("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
//        safetySleep();
//
//        bytesRead = device.read(buffer, 600);
//        log.info("Прочитано в ответ: " + bytesRead);
//        log.info("Ответ:");
//        commander.printArrayLikeDeviceMonitor(buffer);
//        Arrays.fill(buffer, (byte) 0);
//
//        safetySleep();
//
//
//        log.info(" Отправка [34] сообщения:");
//        reportId = (byte) 0x01;
//        byte[] payload34 = {(byte) 0x04, (byte) 0x07, (byte) 0x02, (byte) 0x21, (byte) 0x00, (byte) 0xE1, (byte) 0x40, (byte) 0xFF, (byte) 0x01};
//        msg = commander.generateMessageConfigured(payload34);
//        commander.printArrayLikeDeviceMonitor(msg);
//        log.info("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
//        safetySleep();
//
//        bytesRead = device.read(buffer, 600);
//        log.info("Прочитано в ответ: " + bytesRead);
//        log.info("Ответ:");
//        commander.printArrayLikeDeviceMonitor(buffer);
//        Arrays.fill(buffer, (byte) 0);
//
//        safetySleep();
//
//        log.info(" Отправка [35] сообщения:");
//        reportId = (byte) 0x01;
//        byte[] payload35 = {(byte) 0x02, (byte) 0x02};
//        msg = commander.generateMessageConfigured(payload35);
//        commander.printArrayLikeDeviceMonitor(msg);
//        log.info("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
//        safetySleep();
//
//        bytesRead = device.read(buffer, 600);
//        log.info("Прочитано в ответ: " + bytesRead);
//        log.info("Ответ:");
//        commander.printArrayLikeDeviceMonitor(buffer);
//        Arrays.fill(buffer, (byte) 0);
//
//        safetySleep();
//
//
//        log.info(" Отправка [36] сообщения:");
//        reportId = (byte) 0x01;
//        byte[] payload36 = {(byte) 0x02, (byte) 0x02, (byte) 0x01, (byte) 0x0D};
//        msg = commander.generateMessageConfigured(payload36);
//        commander.printArrayLikeDeviceMonitor(msg);
//        log.info("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
//        safetySleep();
//
//        bytesRead = device.read(buffer, 600);
//        log.info("Прочитано в ответ: " + bytesRead);
//        log.info("Ответ:");
//        commander.printArrayLikeDeviceMonitor(buffer);
//        Arrays.fill(buffer, (byte) 0);
//
//        safetySleep();
//
//        log.info(" Отправка [37] сообщения:");
//        reportId = (byte) 0x01;
//        byte[] payload37 = {(byte) 0x04, (byte) 0x04, (byte) 0x02, (byte) 0x23, (byte) 0x00, (byte) 0x07};
//        msg = commander.generateMessageConfigured(payload37);
//        commander.printArrayLikeDeviceMonitor(msg);
//        log.info("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
//        safetySleep();
//
//        bytesRead = device.read(buffer, 600);
//        log.info("Прочитано в ответ: " + bytesRead);
//        log.info("Ответ:");
//        commander.printArrayLikeDeviceMonitor(buffer);
//        Arrays.fill(buffer, (byte) 0);
//
//        safetySleep();
//
//        log.info(" Отправка [38] сообщения:");
//        reportId = (byte) 0x01;
//        byte[] payload38 = {(byte) 0x04, (byte) 0x04, (byte) 0x02, (byte) 0x23, (byte) 0x08, (byte) 0x07};
//        msg = commander.generateMessageConfigured(payload38);
//        commander.printArrayLikeDeviceMonitor(msg);
//        log.info("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
//        safetySleep();
//
//        bytesRead = device.read(buffer, 600);
//        log.info("Прочитано в ответ: " + bytesRead);
//        log.info("Ответ:");
//        commander.printArrayLikeDeviceMonitor(buffer);
//        Arrays.fill(buffer, (byte) 0);
//
//        safetySleep();
//
//        log.info(" Отправка [39] сообщения:");
//        reportId = (byte) 0x01;
//        byte[] payload39 = {(byte) 0x04, (byte) 0x04, (byte) 0x02, (byte) 0x23, (byte) 0x10, (byte) 0x07};
//        msg = commander.generateMessageConfigured(payload39);
//        commander.printArrayLikeDeviceMonitor(msg);
//        log.info("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
//        safetySleep();
//
//        bytesRead = device.read(buffer, 600);
//        log.info("Прочитано в ответ: " + bytesRead);
//        log.info("Ответ:");
//        commander.printArrayLikeDeviceMonitor(buffer);
//        Arrays.fill(buffer, (byte) 0);
//
//        safetySleep();
//
////        log.info(" Отправка [40] сообщения:");
////        reportId = (byte) 0x01;
////        byte[] payload40 = {(byte) 0x02, (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};
////        msg = commander.generateMessageConfigured(payload40);
////        commander.printArrayLikeDeviceMonitor(msg);
////        log.info("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
////        safetySleep();
////
////        bytesRead = device.read(buffer, 600);
////        log.info("Прочитано в ответ: " + bytesRead);
////        log.info("Ответ:");
////        commander.printArrayLikeDeviceMonitor(buffer);
////        Arrays.fill(buffer, (byte) 0);
////
////        longSleep();
//
//
//
//    }
//
//    public void shutDown(HidDevice device) {
//        count = 0;
//        // 1. Handshake / активация cradle
//        simpleSend(device, new byte[]{(byte) 0x02, (byte) 0xB2});
//        safetySleep();
//
//        simpleSend(device, new byte[]{(byte) 0x02, (byte) 0xB2});
//        safetySleep();
//        simpleSend(device, new byte[]{(byte) 0x02, (byte) 0xBD});
//        safetySleep();
//        simpleSend(device, new byte[]{(byte) 0x01, (byte) 0x55});
//        safetySleep();
//
//        // 2. Перевод в другой режим
//        simpleSend(device, new byte[]{(byte) 0x01, (byte) 0x02, (byte) 0x02, (byte) 0x01, (byte) 0x0D});
//        safetySleep();
//        simpleSend(device, new byte[]{(byte) 0x01, (byte) 0x04, (byte) 0x02, (byte) 0x02, (byte) 0x2B});
//        safetySleep();
//        simpleSend(device, new byte[]{(byte) 0x02, (byte) 0x02, (byte) 0x00, (byte) 0x00});
//        safetySleep();
//
//        // 3. Идентификация (NFC FS2JAST4)
//        simpleSend(device, new byte[]{(byte) 0x01, (byte) 0x02, (byte) 0x02, (byte) 0x01, (byte) 0x0D});
//        safetySleep();
//
//        // 4. Последовательность shutdown-команд (04 07 02 21 …)
//        simpleSend(device, new byte[]{(byte) 0x01, 0x04, 0x07, 0x02, 0x21});
//        safetySleep();
//        simpleSend(device, new byte[]{(byte) 0x01, 0x04, 0x07, 0x02, 0x21, 0x01, 0x03, 0x11, (byte) 0xD1, (byte) 0x01});
//        safetySleep();
//        simpleSend(device, new byte[]{(byte) 0x01, 0x04, 0x07, 0x02, 0x21, 0x02, 0x0D, 0x54, 0x02, 0x65});
//        safetySleep();
//        simpleSend(device, new byte[]{(byte) 0x01, 0x04, 0x07, 0x02, 0x21, 0x03, 0x6E,(byte) 0x00});
//        safetySleep();
//        simpleSend(device, new byte[]{(byte) 0x01, 0x04, 0x07, 0x02, 0x21, 0x04, 0x00, 0x01});
//        safetySleep();
//        simpleSend(device, new byte[]{(byte) 0x01, 0x04, 0x07, 0x02, 0x21, 0x05, 0x01, 0x00, 0x00, (byte) 0xFE});
//        safetySleep();
//        simpleSend(device, new byte[]{(byte) 0x01, 0x04, 0x07, 0x02, 0x21, 0x06, 0x00, 0x00, 0x00, (byte) 0xBD});
//        safetySleep();
//        simpleSend(device, new byte[]{(byte) 0x01, 0x04, 0x07, 0x02, 0x21, 0x03, 0x6E, 0x06, 0x00, 0x00});
//        safetySleep();
//        simpleSend(device, new byte[]{(byte) 0x01, 0x04, 0x07, 0x02, 0x21, 0x00, (byte) 0xE1, 0x40, (byte) 0xFF, 0x01});
//        safetySleep();
//        longSleep();
//
//        // 5. Завершающие команды
//        simpleSend(device, new byte[]{(byte) 0x01, 0x02, 0x02, 0x00, 0x00});
//        safetySleep();
//        longSleep();
//        simpleSend(device, new byte[]{(byte) 0x01, 0x02, 0x02, 0x01, 0x0D});
//
//        // 6. Контрольные запросы состояния
//        simpleSend(device, new byte[]{(byte) 0x01, 0x04, 0x04, 0x02, 0x23, 0x00, 0x07});
//        simpleSend(device, new byte[]{(byte) 0x01, 0x04, 0x04, 0x02, 0x23, 0x08, 0x07});
//    }
//
//    public void soundOff(HidDevice device){
//        count = 0;
//        simpleSend(device, new byte[]{(byte) 0x01 , (byte) 0x02 , (byte) 0x02 , (byte) 0x01 , (byte) 0x0d , (byte) 0x00 , (byte) 0x00 , (byte) 0x00 , (byte) 0x00 , (byte) 0x00});
//        safetySleep();
//        simpleSend(device, new byte[]{(byte) 0x01 , (byte) 0x04 , (byte) 0x07 , (byte) 0x02 , (byte) 0x21 , (byte) 0x00 , (byte) 0x00 , (byte) 0x00 , (byte) 0x00 , (byte) 0x00});
//        safetySleep();
//        simpleSend(device, new byte[]{(byte) 0x01 , (byte) 0x04 , (byte) 0x07 , (byte) 0x02 , (byte) 0x21 , (byte) 0x01 , (byte) 0x03 , (byte) 0x16 , (byte) 0xd1 , (byte) 0x01});
//        safetySleep();
//        simpleSend(device, new byte[]{(byte) 0x01 , (byte) 0x04 , (byte) 0x07 , (byte) 0x02 , (byte) 0x21 , (byte) 0x02 , (byte) 0x12 , (byte) 0x54 , (byte) 0x02 , (byte) 0x65});
//        safetySleep();
//        simpleSend(device, new byte[]{(byte) 0x01 , (byte) 0x04 , (byte) 0x07 , (byte) 0x02 , (byte) 0x21 , (byte) 0x03 , (byte) 0x6e , (byte) 0x00 , (byte) 0x00 , (byte) 0x00});
//        safetySleep();
//        simpleSend(device, new byte[]{(byte) 0x01 , (byte) 0x04 , (byte) 0x07 , (byte) 0x02 , (byte) 0x21 , (byte) 0x04 , (byte) 0x00 , (byte) 0x22 , (byte) 0x00 , (byte) 0x01});
//        safetySleep();
//        simpleSend(device, new byte[]{(byte) 0x01 , (byte) 0x04 , (byte) 0x07 , (byte) 0x02 , (byte) 0x21 , (byte) 0x05 , (byte) 0x01 , (byte) 0x00 , (byte) 0x00 , (byte) 0x01});
//        safetySleep();
//        simpleSend(device, new byte[]{(byte) 0x01 , (byte) 0x04 , (byte) 0x07 , (byte) 0x02 , (byte) 0x21 , (byte) 0x06 , (byte) 0x1b , (byte) 0xdf , (byte) 0x05 , (byte) 0xa5});
//        safetySleep();
//        simpleSend(device, new byte[]{(byte) 0x01 , (byte) 0x04 , (byte) 0x07 , (byte) 0x02 , (byte) 0x21 , (byte) 0x07 , (byte) 0xfe , (byte) 0x00 , (byte) 0x00 , (byte) 0x00});
//        safetySleep();
//        simpleSend(device, new byte[]{(byte) 0x01 , (byte) 0x04 , (byte) 0x07 , (byte) 0x02 , (byte) 0x21 , (byte) 0x03 , (byte) 0x6e , (byte) 0x0b , (byte) 0x00 , (byte) 0x00});
//        safetySleep();
//        simpleSend(device, new byte[]{(byte) 0x01 , (byte) 0x04 , (byte) 0x07 , (byte) 0x02 , (byte) 0x21 , (byte) 0x00 , (byte) 0xe1 , (byte) 0x40 , (byte) 0xff , (byte) 0x01});
//        safetySleep();
//        simpleSend(device, new byte[]{(byte) 0x01 , (byte) 0x02 , (byte) 0x02 , (byte) 0x00 , (byte) 0x00 , (byte) 0x00 , (byte) 0x00 , (byte) 0x00 , (byte) 0x00 , (byte) 0x00});
//        safetySleep();
//        simpleSend(device, new byte[]{(byte) 0x01 , (byte) 0x02 , (byte) 0x02 , (byte) 0x01 , (byte) 0x0d , (byte) 0x00 , (byte) 0x00 , (byte) 0x00 , (byte) 0x00 , (byte) 0x00});
//        safetySleep();
//        simpleSend(device, new byte[]{(byte) 0x01 , (byte) 0x04 , (byte) 0x04 , (byte) 0x02 , (byte) 0x23 , (byte) 0x00 , (byte) 0x07 , (byte) 0x00 , (byte) 0x00 , (byte) 0x00});
//        safetySleep();
//        simpleSend(device, new byte[]{(byte) 0x01 , (byte) 0x04 , (byte) 0x04 , (byte) 0x02 , (byte) 0x23 , (byte) 0x08 , (byte) 0x07 , (byte) 0x00 , (byte) 0x00 , (byte) 0x00});
//        safetySleep();
//        simpleSend(device, new byte[]{(byte) 0x01 , (byte) 0x04 , (byte) 0x04 , (byte) 0x02 , (byte) 0x23 , (byte) 0x10 , (byte) 0x07 , (byte) 0x00 , (byte) 0x00 , (byte) 0x00});
//        safetySleep();
//        simpleSend(device, new byte[]{(byte) 0x01 , (byte) 0x02 , (byte) 0x02 , (byte) 0x00 , (byte) 0x00 , (byte) 0x00 , (byte) 0x00 , (byte) 0x00 , (byte) 0x00 , (byte) 0x00});
//        safetySleep();
//    }
//
//    public void soundOn(HidDevice device) {
//        count = 0;
//        simpleSend(device, new byte[]{(byte) 0x01, (byte) 0x02, (byte) 0x02, (byte) 0x01, (byte) 0x0D, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00});
//        safetySleep();
//        simpleSend(device, new byte[]{(byte) 0x01, (byte) 0x04, (byte) 0x07, (byte) 0x02, (byte) 0x21, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00});
//        safetySleep();
//        simpleSend(device, new byte[]{(byte) 0x01, (byte) 0x04, (byte) 0x07, (byte) 0x02, (byte) 0x21, (byte) 0x01, (byte) 0x03, (byte) 0x16, (byte) 0xD1, (byte) 0x01});
//        safetySleep();
//        simpleSend(device, new byte[]{(byte) 0x01, (byte) 0x04, (byte) 0x07, (byte) 0x02, (byte) 0x21, (byte) 0x02, (byte) 0x12, (byte) 0x54, (byte) 0x02, (byte) 0x65});
//        safetySleep();
//        simpleSend(device, new byte[]{(byte) 0x01, (byte) 0x04, (byte) 0x07, (byte) 0x02, (byte) 0x21, (byte) 0x03, (byte) 0x6E, (byte) 0x00, (byte) 0x00, (byte) 0x00});
//        safetySleep();
//        simpleSend(device, new byte[]{(byte) 0x01, (byte) 0x04, (byte) 0x07, (byte) 0x02, (byte) 0x21, (byte) 0x04, (byte) 0x00, (byte) 0x22, (byte) 0x00, (byte) 0x01});
//        safetySleep();
//        simpleSend(device, new byte[]{(byte) 0x01, (byte) 0x04, (byte) 0x07, (byte) 0x02, (byte) 0x21, (byte) 0x05, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00});
//        safetySleep();
//        simpleSend(device, new byte[]{(byte) 0x01, (byte) 0x04, (byte) 0x07, (byte) 0x02, (byte) 0x21, (byte) 0x06, (byte) 0x8D, (byte) 0xEF, (byte) 0x02, (byte) 0xD2});
//        safetySleep();
//        simpleSend(device, new byte[]{(byte) 0x01, (byte) 0x04, (byte) 0x07, (byte) 0x02, (byte) 0x21, (byte) 0x07, (byte) 0xFE, (byte) 0x00, (byte) 0x00, (byte) 0x00});
//        safetySleep();
//        simpleSend(device, new byte[]{(byte) 0x01, (byte) 0x04, (byte) 0x07, (byte) 0x02, (byte) 0x21, (byte) 0x03, (byte) 0x6E, (byte) 0x0B, (byte) 0x00, (byte) 0x00});
//        safetySleep();
//        simpleSend(device, new byte[]{(byte) 0x01, (byte) 0x04, (byte) 0x07, (byte) 0x02, (byte) 0x21, (byte) 0x00, (byte) 0xE1, (byte) 0x40, (byte) 0xFF, (byte) 0x01});
//        safetySleep();
//        simpleSend(device, new byte[]{(byte) 0x01, (byte) 0x02, (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00});
//        safetySleep();
//        simpleSend(device, new byte[]{(byte) 0x01, (byte) 0x02, (byte) 0x02, (byte) 0x01, (byte) 0x0D, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00});
//        safetySleep();
//        simpleSend(device, new byte[]{(byte) 0x01, (byte) 0x04, (byte) 0x04, (byte) 0x02, (byte) 0x23, (byte) 0x00, (byte) 0x07, (byte) 0x00, (byte) 0x00, (byte) 0x00});
//        safetySleep();
//        simpleSend(device, new byte[]{(byte) 0x01, (byte) 0x04, (byte) 0x04, (byte) 0x02, (byte) 0x23, (byte) 0x08, (byte) 0x07, (byte) 0x00, (byte) 0x00, (byte) 0x00});
//        safetySleep();
//        simpleSend(device, new byte[]{(byte) 0x01, (byte) 0x04, (byte) 0x04, (byte) 0x02, (byte) 0x23, (byte) 0x10, (byte) 0x07, (byte) 0x00, (byte) 0x00, (byte) 0x00});
//        safetySleep();
//        simpleSend(device, new byte[]{(byte) 0x01, (byte) 0x02, (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00});
//        safetySleep();
//    }
//
//
//
//    private void safetySleep() {
//        try {
//            Thread.sleep(400);
//        } catch (InterruptedException ex) {
//            log.error("Sleep error " + ex.getMessage());
//        }
//    }
//
//    private void longSleep() {
//        try {
//            Thread.sleep(2000);
//        } catch (InterruptedException ex) {
//            log.error("Sleep error " + ex.getMessage());
//        }
//    }
//}
