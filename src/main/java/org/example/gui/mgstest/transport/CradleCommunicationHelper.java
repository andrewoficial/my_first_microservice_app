package org.example.gui.mgstest.transport;

import org.apache.log4j.Logger;
import org.example.gui.mgstest.transport.hid.SomeHidController;
import org.example.utilites.MyUtilities;
import org.hid4java.HidDevice;


import java.util.ArrayList;

public class CradleCommunicationHelper {
    SomeHidController hidController = new SomeHidController();
    private final Logger log = Logger.getLogger(CradleCommunicationHelper.class);
    public final ArrayList<byte[]> RESET_ZERO_OFFSET_ANSWER_SAMPLES = new ArrayList<>();
    public final ArrayList<byte[]> WRITE_MAGIK_FIRST_OFFSET_ANSWER_SAMPLES = new ArrayList<>();
    public final ArrayList<byte[]> WRITE_MAGIK_FIFTH_OFFSET_ANSWER_SAMPLES = new ArrayList<>();
    public final ArrayList<byte[]> WRITE_MAGIK_SIX_OFFSET_ANSWER_SAMPLES = new ArrayList<>();
    public final ArrayList<byte[]> ASSEMBLE_C_GET_WRITE_ANSWER_SAMPLES = new ArrayList<>();
    public final ArrayList<byte[]> CRADLE_ACTIVATE_TRANSMIT_ANSWER_SAMPLES = new ArrayList<>();


    public CradleCommunicationHelper() {
        RESET_ZERO_OFFSET_ANSWER_SAMPLES.add(new byte[]{0x07, (byte)0x80, (byte)0x04, (byte)0x00});
        RESET_ZERO_OFFSET_ANSWER_SAMPLES.add(new byte[]{0x07, (byte)0x8E, (byte)0x04, (byte)0x00});

        WRITE_MAGIK_FIRST_OFFSET_ANSWER_SAMPLES.add(new byte[]{0x07, (byte)0x80, (byte)0x04, 0x00});
        WRITE_MAGIK_FIRST_OFFSET_ANSWER_SAMPLES.add(new byte[]{0x07, (byte)0x8E, (byte)0x00, 0x00});

        WRITE_MAGIK_FIFTH_OFFSET_ANSWER_SAMPLES.add(new byte[]{0x07, (byte)0x80, (byte)0x04, 0x00});
        WRITE_MAGIK_FIFTH_OFFSET_ANSWER_SAMPLES.add(new byte[]{0x07, (byte)0x8E, (byte)0x04, 0x00});

        WRITE_MAGIK_SIX_OFFSET_ANSWER_SAMPLES.add(new byte[]{0x07, (byte)0x80, (byte)0x04, 0x00});
        WRITE_MAGIK_SIX_OFFSET_ANSWER_SAMPLES.add(new byte[]{0x07, (byte)0x8E, (byte)0x04, 0x00});
        //WRITE_MAGIK_SIX_OFFSET_ANSWER_SAMPLES.add(new byte[]{0x07, (byte)0x87, (byte)0x00, 0x00});

        ASSEMBLE_C_GET_WRITE_ANSWER_SAMPLES.add(new byte[] {0x07, (byte)0x80, (byte)0x24});
        ASSEMBLE_C_GET_WRITE_ANSWER_SAMPLES.add(new byte[] {0x07, (byte)0x87, (byte)0x00});
        //.add(new byte[] {0x07, (byte)0x87, (byte)0x00});

        CRADLE_ACTIVATE_TRANSMIT_ANSWER_SAMPLES.add(new byte[]{0x07, (byte)0x80, (byte)0x04, (byte)0x00, (byte)0x78});
        CRADLE_ACTIVATE_TRANSMIT_ANSWER_SAMPLES.add(new byte[]{0x07, (byte)0x8E, (byte)0x04, (byte)0x00, (byte)0x78});

    }

    //=============DeviceCommand================//

    /**
     * REQ: 02 BC CC CC CC CC CC CC CC CC CC CC CC CC CC CC
     * REQ: 02 B2 CC CC CC CC CC CC CC CC CC CC CC CC CC CC
     * REQ: 02 BD CC CC CC CC CC CC CC CC CC CC CC CC CC CC
     * REQ: 01 55 CC CC CC CC CC CC CC CC CC CC CC CC CC CC
     * REQ: 01 02 02 01 0D CC CC CC CC CC CC CC CC CC CC CC
     * REQ: 01 04 02 02 2B CC CC CC CC CC CC CC CC CC CC CC
     *
     * @param device
     */
    public void doSettingsBytes(HidDevice device){
        log.info("Первичная настройка кредла");
        byte []exceptedAns = new byte[]{0x07, (byte)0x00, (byte)0x03};

        log.warn("Is device closed:"  + device.isClosed());
        log.warn("Run for device: " + device.getManufacturer());
        log.warn("Run for device: " + device.getProductId());
        log.warn("Run for device: " + device.getPath());


        hidController.simpleSend(device, new byte[]{0x02, (byte)0xBC, (byte)0xCC, (byte) 0xCC});
        safetySleep(300);
        hidController.printArrayLikeDeviceMonitor(hidController.readResponse(device));
        safetySleep(1300);
        //02 BC CC CC CC CC CC CC CC CC CC CC CC CC CC CC
        exceptedAns = new byte[]{0x07, (byte)0x00, (byte)0x03};
        waitForResponse(device,
                () -> simpleSendInitial(device, new byte[]{0x02, (byte)0xBC, (byte)0xCC, (byte) 0xCC}),
                exceptedAns,"DO SETTINGS BYTES: 02 BC",5, 300);
        //07 00 03


        //02 B2 CC CC CC CC CC CC CC CC CC CC CC CC CC CC
        exceptedAns = new byte[]{0x07, (byte)0x00, (byte)0x07};
        waitForResponse(device,
                () -> simpleSendInitial(device, new byte[]{0x02, (byte)0xB2}),
                exceptedAns,"DO SETTINGS BYTES: 02 B2",3, 50);
        //07 00 03

        //02 BD CC CC CC CC CC CC CC CC CC CC CC CC CC CC
        exceptedAns = new byte[]{0x07, (byte)0x80, (byte)0x00};
        waitForResponse(device,
                () -> simpleSendInitial(device, new byte[]{0x02, (byte)0xBD}),
                exceptedAns,"DO SETTINGS BYTES: 02 BD",3, 50);
        //07 80 00

        //01 55 CC CC CC CC CC CC CC CC CC CC CC CC CC CC
        exceptedAns = new byte[]{0x07, (byte)0x55, (byte)0x00};
        waitForResponse(device,
                () -> simpleSendInitial(device, new byte[]{0x01, (byte)0x55}),
                exceptedAns,"DO SETTINGS BYTES: 01 55",3, 50);
        //07 55 00

        //01 02 02 01 0D CC CC CC CC CC CC CC CC CC CC CC
        cradleSwitchOn(device);

        //01 04 02 02 2B CC CC CC CC CC CC CC CC CC CC CC
        exceptedAns = new byte[]{0x07, (byte)0x80, (byte)0x12, 0x00};
        waitForResponse(device,
                () -> simpleSendInitial(device, new byte[]{0x01, (byte)0x04, 0x02, 0x02, 0x2B}),
                exceptedAns,"DO SETTINGS BYTES: 01 04 02 02 2B",3, 50);
        //07 80 12 00
    }

    /**
     * NFC_ENABLE:
     * REQ: 01 02 02 01 0D
     * ANS: 07 00 00 00 00
     *
     */
    public void cradleSwitchOn(HidDevice device) {
        byte [] exceptedAns = null;
        byte [] answer = null;

        //REQ_SAMPLE:  01 02 02 01 0D 00 00 00 00 00 00 00 00 00 00 00
        exceptedAns = new byte[]{0x07, (byte)0x00, (byte)0x00};
        answer = waitForResponse(device,
                () -> simpleSend(device, new byte[]{(byte) 0x01, (byte) 0x02, (byte) 0x02, (byte) 0x01, (byte) 0x0D}),
                exceptedAns,"NFC_ENABLE",3, 55);
        //ANS_SAMPLE: 07 00 00 00 78 F0 00 C1 31 12 5A 02 E0 FF 00 7F
    }

    /**
     * NFC_DISABLE:
     * REQ: 01 02 02
     * ANS: 07 00 00
     *
     */
    public void cradleSwitchOff(HidDevice device) {
        byte [] exceptedAns = null;
        byte [] answer = null;

        //REQ_SAMPLE:  01 02 02 00 00 00 00 00 00 00 00 00 00 00 00 00
        exceptedAns = new byte[]{0x07, (byte)0x00, (byte)0x00, (byte)0x00};
        answer = waitForResponse(device,
                () -> simpleSend(device, new byte[]{(byte) 0x01, (byte) 0x02, (byte) 0x02}),
                exceptedAns,"NFC_DISABLE",5, 25);
        //ANS_SAMPLE: 07 00 00 00 78 F0 00 C1 31 12 5A 02 E0 FF 00 7F
    }

    /**
     * NFC_TRANSMIT:
     * REQ: 01 04 07 02 21 00 E1 40 FF 01
     * ANS: 07 08 04
     *
     */
    public void cradleActivateTransmit(HidDevice device) {
        byte [] answer = null;
        //REQ_SAMPLE:  01 04 07 02 21 00 E1 40 FF 01 00 00 00 00 00 00
        answer = waitForResponse(device,
                () -> simpleSend(device,
                        new byte[]{(byte) 0x01, (byte) 0x04, (byte) 0x07, (byte) 0x02, (byte) 0x21, (byte) 0x00, (byte) 0xE1, (byte) 0x40, (byte) 0xFF, (byte) 0x01}),
                CRADLE_ACTIVATE_TRANSMIT_ANSWER_SAMPLES,
                "NFC_TRANSMIT",
                3, 3,
                70, 110);
        //ANS_SAMPLE: 07 80 04 00 78 F0 00 C1 31 12 5A 02 E0 FF 00 7F
    }

    //=============MessageBuild================//

    /**
     * RESET_ZERO_OFFSET:
     * REQ: 01 04 07 02 21 00 00 00 00 00
     * ANS: 07 80 04
     *
     */
    public byte[] resetZeroOffset(HidDevice device){
        byte [] answer = null;
        answer = waitForResponse(device,
                () -> cradleWriteBlock(device,
                        (byte) 0x00,
                        new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00}),//Команда из дампа,
                RESET_ZERO_OFFSET_ANSWER_SAMPLES,"RESET_ZERO_OFFSET",
                3, 3,
                70, 110);
        return answer;
    }

    /**
     * WRITE_FIRST_OFFSET:
     * REQ: 01 04 07 02 21 01 03 11 D1 01
     * ANS: 07 80 04
     * @param device
     *
     */
    public void writeMagikInFirstOffset(HidDevice device){
        byte [] answer = null;
        //REQ_SAMPLE:  01 04 07 02 21 01 03 11 D1 01
        answer = waitForResponse(device,
                () -> cradleWriteBlock(device, (byte) 0x01,
                        new byte[]{(byte) 0x03, (byte) 0x11, (byte) 0xD1, (byte) 0x01}),
                WRITE_MAGIK_FIRST_OFFSET_ANSWER_SAMPLES,
                "WRITE_FIRST_OFFSET",
                3, 3,
                70, 110);
        //ANS_SAMPLE: 07 80 04 00 78 F0 00
    }

    /**
     * WRITE_SECOND_OFFSET:
     * REQ: 01 04 07 02 21 02 0D 54 02 65
     * ANS: 07 80 04 00 78 F0 00
     * @param device
     *
     */
    public void writeMagikInSecondOffset(HidDevice device){
        byte [] answer = null;
        //REQ_SAMPLE:  01 04 07 02 21 02 0D 54 02 65
        answer = waitForResponse(device,
                () -> cradleWriteBlock(device, (byte) 0x02, new byte[]{0x0D, 0x54, 0x02, 0x65}),
                WRITE_MAGIK_FIFTH_OFFSET_ANSWER_SAMPLES,"WRITE_SECOND_OFFSET",
                3, 3,
                70, 110);
        //ANS_SAMPLE: 07 80 04 00 78 F0 00
    }

    /**
     * WRITE_THIRD_OFFSET:
     * REQ: 01 04 07 02 21 03 6E LL HH 00
     * ANS: 07 80 04
     * @param device
     *
     */
    public void writeCountInThirdOffset(HidDevice device, int count){
        byte [] exceptedAns = null;
        byte [] answer = null;
        //01 04 07 02 21 03 6E LL HH 00
        exceptedAns = new byte[]{0x07, (byte)0x80, (byte)0x04};
        answer = waitForResponse(device,
                () -> cradleSendCount(device, count),
                WRITE_MAGIK_FIRST_OFFSET_ANSWER_SAMPLES,"WRITE_THIRD_OFFSET Установка счётчика",
                3, 3,
                70, 110);
        //07 80 04
    }


    /**
     * WRITE_FIFTH_OFFSET:
     * REQ: 01 04 07 05 01 00 00 FE
     * ANS: 07 80 04 00 78 F0 00
     * @param device
     *
     */
    public void writeMagikInFifthOffset(HidDevice device){

        byte [] answer = null;
        //Меняется??? 01 04 07 02 21 05 00 00 00 FE || 01 04 07 02 21 05 01 00 00 FE  //05 01 - Новая версия LongGas; 05 00 - Старая версия LongGas
        answer = waitForResponse(device,
                () -> cradleWriteBlock(device, (byte) 0x05,
                        new byte[]{0x01, 0x00, 0x00, (byte) 0xFE}),
                WRITE_MAGIK_FIFTH_OFFSET_ANSWER_SAMPLES,
                "WRITE_FIFTH_OFFSET",
                2, 2,
                50, 80);
        //07 80 04 00 78 F0 00
    }


    /**
     * RESET_SIXTH_OFFSET:
     * REQ: 01 04 07 02 21 06 00 00 00 00 00 00 00
     * ANS: 07 80 04 00 78 F0 00
     * @param device
     *
     */
    public void writeZeroInSixthOffset(HidDevice device){
        byte [] exceptedAns = null;
        byte [] answer = null;
        // 01 04 07 02 21 06 00 00 00 00 00 00 00 00 00 00
        exceptedAns = new byte[]{0x07, (byte)0x80, 0x04, 0x00, 0x78, (byte)0xF0, 0x00};
        answer = waitForResponse(device,
                () -> cradleWriteBlock(device, (byte) 0x06, new byte[]{0x00, 0x00, 0x00, 0x00}),
                exceptedAns,"RESET_SIXTH_OFFSET",3, 25);
        //07 80 04 00 78 F0 00
    }

    /**
     * WRITE_SIXTH_OFFSET:
     * REQ: 01 04 07 02 21 06 00 00 00 04 00 00 00 00 00 00
     * ANS: 07 80 04 00 78 F0 00 C1 31 12 5A 02 E0 FF 00 7F
     * @param device
     *
     */
    public void writeMagikInSixthOffset(HidDevice device){
        byte [] exceptedAns = null;
        byte [] answer = null;
        // 01 04 07 02 21 06 00 00 00 04 00 00 00 00 00 00

        answer = waitForResponse(device,
                () -> cradleWriteBlock(device, (byte) 0x06, new byte[]{0x00, 0x00, 0x00, (byte) 0x04}),
                WRITE_MAGIK_SIX_OFFSET_ANSWER_SAMPLES,"WRITE_SIXTH_OFFSET",
                2, 4,
                70, 100);
        //07 80 04 00 78 F0 00 C1 31 12 5A 02 E0 FF 00 7F
    }

    /**
     * WRITE_SEVENTH_OFFSET:
     * REQ: 01 04 07 02 21 07 FE 00 00 00 00 00 00 00 00 00
     * ANS: ToDO уточнить
     * @param device
     *
     */
    public void writeMagikInSeventhOffset(HidDevice device){
        byte [] exceptedAns = null;
        byte [] answer = null;
        // 01 04 07 02 21 06 00 00 00 04 00 00 00 00 00 00
        exceptedAns = new byte[]{0x07, (byte)0x80, (byte)0x04};
        answer = waitForResponse(device,
                () -> cradleWriteBlock(device, (byte) 0x07, new byte[]{(byte) 0xFE, (byte) 0x00, (byte) 0x00, (byte) 0x00}),
                exceptedAns,"WRITE_SEVENTH_OFFSET",3, 25);
        //
    }

    /**
     *
     * @param device
     * @param msg
     */
    public void simpleSend(HidDevice device, byte[] msg){
        hidController.simpleSend(device, msg);
    }

    /**
     *
     * @param device
     * @param msg
     */
    public void simpleSendInitial(HidDevice device, byte[] msg){
        hidController.simpleSendInitial(device, msg);
    }

    /**
     *
     * @param msg
     */
    public void printArrayLikeDeviceMonitor(byte[] msg){
        hidController.printArrayLikeDeviceMonitor(msg);
    }


    /**
     *
     * Запись блока (4 байта data). Формирует 01 04 07 02 21 03 6E LL HH 00
     * Отправка счётчика (overwrite блока 0x03)
     * @param device HidDevice
     * @param count
     */
    public void cradleSendCount(HidDevice device, int count) {
        byte low = (byte) (count & 0xFF);
        byte high = (byte) ((count >> 8) & 0xFF);
        simpleSend(device, new byte[]{(byte) 0x01, (byte) 0x04, (byte) 0x07, (byte) 0x02, (byte) 0x21, (byte) 0x03, (byte) 0x6E, low, high, (byte) 0x00});
        safetySleep(50);
    }

    /**
     * Запись блока (4 байта data). Формирует 01 04 07 02 21 xx YY YY YY YY
     * xx - blockId
     * YY byte of data
     * @param device
     * @param blockId
     * @param data
     */
    public void cradleWriteBlock(HidDevice device, byte blockId, byte[] data) {
        if (data.length != 4) {
            throw new IllegalArgumentException("Data must be 4 bytes! Got:" + data.length + " blockId:" + blockId +" Array:" + MyUtilities.bytesToHex(data));
        }
        hidController.simpleSend(device, new byte[]{(byte) 0x01, (byte) 0x04, (byte) 0x07, (byte) 0x02, (byte) 0x21, blockId, data[0], data[1], data[2], data[3]});
        safetySleep(25);
    }




    /**
     * Универсальный метод ожидания корректного ответа.
     *
     * @param device    объект устройства
     * @param action    команда, которую нужно выполнить (лямбда: () -> sendCommand())
     * @param expected  эталонный ответ
     * @param comment   строка для логов
     * @param attemptsLimit  макс. число попыток
     * @param sleepMs   пауза между попытками
     * @return          полученный ответ
     */
    public byte[] waitForResponse(HidDevice device,
                                   Runnable action,
                                   byte[] expected,
                                   String comment,
                                   int attemptsLimit,
                                   long sleepMs) {
        int attempts = 0;
        byte[] received = null;
        log.info("Начинаю отправку [" + comment + "]. Ожидаю заголовок " + MyUtilities.bytesToHex(expected));

        action.run();
        for (int i = 0; i < attemptsLimit; i++) {
            attempts++;
            safetySleep(sleepMs);
            received = hidController.readResponse(device);
            if(isEqual(received, expected, comment)){
                break;
            }
        }

        if (attempts >= attemptsLimit) {
            log.warn("Превышен предел попыток (" + attemptsLimit + ")");
        }
        if(isEqual(received, expected, comment)) {
            log.info("Получен корректный ответ после " + (attempts + 1) + " попыток");
        }
        return received;
    }

    /**
     * Универсальный метод ожидания корректного ответа.
     *
     * @param device    объект устройства
     * @param action    команда, которую нужно выполнить (лямбда: () -> sendCommand())
     * @param expectedValues  набор ожидаемых ответов (варианты)
     * @param comment   строка для логов
     * @param readRepeatLimit  макс. число попыток чтения
     * @param writeRepeatLimit  макс. число попыток записи
     * @param delayRead   пауза между попытками чтения
     * @param delayWrite   пауза между попытками записи
     * @return          полученный ответ
     */
    public byte[] waitForResponse(HidDevice device,
                                  Runnable action,
                                  ArrayList<byte[]> expectedValues,
                                  String comment,
                                  int readRepeatLimit,
                                  int writeRepeatLimit,
                                  long delayRead,
                                  long delayWrite) {
        log.info("Начинаю отправку [" + comment + "] команды и чтения ответа readRepeatLimit " + readRepeatLimit +" writeRepeatLimit " + writeRepeatLimit + " delayRead " + delayRead + " delayWrite " + delayWrite);
        int attemptsRead = 0;
        int attemptsWrite = 0;
        byte[] received = null;
        //log.info("Начинаю отправку, [" + comment + "]. Ожидаю варианты ответов");
        //for (byte[] value : expectedValues) {
            //MyUtilities.bytesToHex(value);
        //}

        for (int i = 0; i < writeRepeatLimit; i++) {
            action.run();
            attemptsWrite++;
            attemptsRead = 0;
            for(int j = 0; j < readRepeatLimit; j++){
                attemptsRead++;
                safetySleep(delayRead);
                received = hidController.readResponse(device);
                if(isEqual(received, expectedValues, comment)){
                    break;
                }
            }
            if(isEqual(received, expectedValues, comment)){
                break;
            }else{
                log.warn("Превышен предел попыток чтения attemptsRead: " +  attemptsRead + " readRepeatLimit: " + readRepeatLimit);
            }
            safetySleep(delayWrite);
        }

        if(isEqual(received, expectedValues, comment)) {
            log.info("Получен корректный ответ после " + (attemptsRead ) + " попыток чтения и " + (attemptsWrite)+ " попыток записи");
            return received;
        }else{
            log.warn("Превышен предел попыток повтора отправки команды (" + readRepeatLimit + ")");
        }

        return received;
    }

    /**
     * Проверка ответа на соответствие заголовка требуемому
     * @param received - Принятый пакет
     * @param expectedValues - Требуемый заголовок
     * @param comment - Комментраий для печати в отладку
     * @return - boolean. True if first part is equal.
     */
    public boolean isEqual(byte[] received, ArrayList<byte []> expectedValues, String comment) {
        if (expectedValues == null || received == null) {
            log.warn("[" + comment + "] передан null аргумент");
            return false;
        }
        if (expectedValues.isEmpty() || received.length == 0) {
            log.warn("[" + comment + "] пустой набор массивов в аргументах");
            return false;
        }

        for (byte[] expectedValue : expectedValues) {
            if (received.length < expectedValue.length) {
                log.warn("[" + comment + "] полученный массив короче эталонного");
                continue;
            }
            boolean isDifferent = false;
            for (int i = 0; i < expectedValue.length; i++) {
                if (received[i] != expectedValue[i]) {
                    log.info("[" + comment + "] несовпадение на позиции для одного из вариантов ответа" + i +
                            " (ожидал " + String.format("%02X", expectedValue[i]) +
                            ", получил " + String.format("%02X", received[i]) + ")");
                    isDifferent = true;
                    break;
                }

            }
            if(!isDifferent){
                return true;
            }
        }
        return false;
    }

    /**
     * Проверка ответа на соответствие заголовка требуемому
     * @param received - Принятый пакет
     * @param expected - Требуемый заголовок
     * @param comment - Комментраий для печати в отладку
     * @return - boolean. True if first part is equal.
     */
    public boolean isEqual(byte[] received, byte[] expected, String comment) {
        if (expected == null || received == null) {
            log.warn("[" + comment + "] передан null аргумент");
            return false;
        }
        if (expected.length == 0 || received.length == 0) {
            log.warn("[" + comment + "] пустой массив в аргументах");
            return false;
        }
        if (received.length < expected.length) {
            log.warn("[" + comment + "] полученный массив короче эталонного");
            return false;
        }

        for (int i = 0; i < expected.length; i++) {
            if (received[i] != expected[i]) {
                log.info("[" + comment + "] несовпадение на позиции " + i +
                        " (ожидал " + String.format("%02X", expected[i]) +
                        ", получил " + String.format("%02X", received[i]) + ")");
                return false;
            }
        }
        return true;
    }

    public void safetySleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ex) {
            log.error("Sleep error " + ex.getMessage());
        }
    }

        /*
    Пример использования
    byte[] assembled = assembleCget(device, offsets, (byte)0x07);

    Данные для написания теста.
2025-09-09 11:35:09.376 INFO  [AWT-EventQueue-0] org.example.gui.mgstest.transport.hid.SomeHidController - Полезная нагрузка: 01 04 04 02 23 00 07 остальное заполнено 00 до 64 байт
2025-09-09 11:35:09.505 INFO  [AWT-EventQueue-0] org.example.gui.mgstest.transport.CradleController - Ответ на команду:
07 80 24 00 E1 40 FF 01 03 3D D1 01 39 54 02 65
6E 06 00 00 00 2E 01 00 00 00 01 01 30 30 47 0C
32 39 38 32 13 14 00 00 61 50 00 08 0C 00 00 20
06 06 06 06 03 00 00 00 04 10 00 20 00 00 00 00
2025-09-09 11:35:09.506 INFO  [AWT-EventQueue-0] org.example.gui.mgstest.transport.CradleController - [] получен корректный ответ после 1 попыток
2025-09-09 11:35:09.508 INFO  [AWT-EventQueue-0] org.example.gui.mgstest.transport.hid.SomeHidController - Полезная нагрузка: 01 04 04 02 23 08 07 остальное заполнено 00 до 64 байт
2025-09-09 11:35:09.637 INFO  [AWT-EventQueue-0] org.example.gui.mgstest.transport.CradleController - Ответ на команду:
07 80 24 00 03 B4 FC 4B 4D 61 BC 00 67 00 00 00
0E 02 5D 03 27 20 BF 68 00 00 00 00 0E EE E7 61
01 F0 00 00 C2 5D 00 00 61 50 00 08 0C 00 00 20
06 06 06 06 03 00 00 00 04 10 00 20 00 00 00 00
2025-09-09 11:35:09.637 INFO  [AWT-EventQueue-0] org.example.gui.mgstest.transport.CradleController - [] получен корректный ответ после 1 попыток
2025-09-09 11:35:09.641 INFO  [AWT-EventQueue-0] org.example.gui.mgstest.transport.hid.SomeHidController - Полезная нагрузка: 01 04 04 02 23 10 07 остальное заполнено 00 до 64 байт
2025-09-09 11:35:09.770 INFO  [AWT-EventQueue-0] org.example.gui.mgstest.transport.CradleController - Ответ на команду:
07 80 24 00 A7 F0 CF FF FE 00 E0 42 00 00 E2 42
00 00 E4 42 00 00 E6 42 00 00 E8 42 00 00 EA 42
00 00 EC 42 40 92 00 00 61 50 00 08 0C 00 00 20
06 06 06 06 03 00 00 00 04 10 00 20 00 00 00 00
(Из каждого ответа извлекается payload)
07 80 24 00 | A7 F0 CF FF FE 00 E0 42 00 00 E2 42
00 00 E4 42 00 00 E6 42 00 00 E8 42 00 00 EA 42
00 00 EC 42 | 40 92 00 00 61 50 00 08 0C 00 00 20
06 06 06 06 03 00 00 00 04 10 00 20 00 00 00 00
2025-09-09 11:35:09.770 INFO  [AWT-EventQueue-0] org.example.gui.mgstest.transport.CradleController - [] получен корректный ответ после 1 попыток
2025-09-09 11:35:09.771 WARN  [AWT-EventQueue-0] org.example.gui.mgstest.transport.hid.SomeHidController - Большой массив на вывод
00 00 00 E1 40 FF 01 03 3D D1 01 39 54 02 65 6E
06 00 00 00 2E 01 00 00 00 01 01 30 30 47 0C 32
39 38 32 03 B4 FC 4B 4D 61 BC 00 67 00 00 00 0E
02 5D 03 27 20 BF 68 00 00 00 00 0E EE E7 61 01
F0 00 00 A7 F0 CF FF FE 00 E0 42 00 00 E2 42 00
00 E4 42 00 00 E6 42 00 00 E8 42 00 00 EA 42 00
00 EC 42 00 00 00 00 00 00 00 00 00 00 00 00 00
00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
00 00 00 00
     */
    /**
     *
     * @param device - HidDevice
     * @param offsets - Перечень адрессов для считвания, например 01 04 04 02 23 20 07; 01 04 04 02 23 28 07; 01 04 04 02 23 30 07
     * @param size - Некий размер, в примерах выше это цифра 07
     * @return - массив, со вставками
     */
    public byte[] assembleCget(HidDevice device, byte[] offsets, byte size) throws Exception {
        /*
        Пример возвращаемого от устройства
        07 80 24 00 00 00 97 43 00 80 97 43 00 00 98 43
        00 80 98 43 00 00 99 43 00 80 99 43 00 00 9A 43
        00 80 9A 43 5D 26 00 00 61 50 00 08 0C 00 00 20
        06 06 06 06 03 00 00 00 04 10 00 20 00 00 00 00
         */

        //Задаётся размер блока. Библиотека hid устройвств возвращает 4 ряда по 16 чисел. Первые четрые отбрасываются.
        final int nBlockSize = 32;

        //Задаёт число, сколько раз нужно считывать. Например, команды: 01 04 04 02 23 20 07; 01 04 04 02 23 28 07; 01 04 04 02 23 30 07 нужно три раза вызвать чтение для адресов 20 28 30
        int nBlocks = offsets.length; // в C++ для GetAllCoefByte nBlockRead = 9

        // Вычисляется размер требуемого массива.
        // Количество циклов чтения плюс один, умножить на размер блока, прибавить 4 ячейки для хранения первого заголовка (не уверен)
        int totalLen = (nBlocks + 1) * nBlockSize + 4;

        //Создаётся массив, заполненый нулями.  аналог memset
        byte[] result = new byte[totalLen];

        //Начинается цикл опросв
        for (int i = 0; i < offsets.length; i++) {
            //Выставляется адрес для чтения
            byte off = offsets[i];

            //Мой метод подачи команды и чтения ответа. Проверяет, что заголовок как в примере.
            //Аргументы: HidDevice; function(отправляющая и возвращающая ответ), byte[]{требуемое начало ответа}, String "коммент в для отладки", int количетсов повторов чтения, int сон между попытками ms
            byte[] arrRead = waitForResponse(device,
                    () -> hidController.simpleSend(device,
                            new byte[]{(byte) 0x01, (byte) 0x04, (byte) 0x04, (byte) 0x02, (byte) 0x23, off, size}),
                    ASSEMBLE_C_GET_WRITE_ANSWER_SAMPLES,
                    "ASSEMBLE_C_GET_"+off,
                    2, 4,
                    250, 350);
            log.info("Получена валидная часть ответа ");
            //printArrayLikeDeviceMonitor(arrRead);
            if(! isEqual(arrRead, ASSEMBLE_C_GET_WRITE_ANSWER_SAMPLES, "Проверка на выброс исключения")){
                log.warn("В процессе отправки адресов:");
                hidController.printArrayLikeDeviceMonitor(offsets);
                log.warn("В процессе отправки off:" + off + " и переменной " + size + " получен непредвиденный ответ:");
                hidController.printArrayLikeDeviceMonitor(arrRead);
                log.warn("Ожидаемые заголовки:");
                for (byte[] assembleCGetWriteAnswerSample : ASSEMBLE_C_GET_WRITE_ANSWER_SAMPLES) {
                    hidController.printArrayLikeDeviceMonitor(assembleCGetWriteAnswerSample);
                }

                throw new Exception("Ошибка выполнения пакета команд");
            }

            //Начинается склейка массива
            for (int k = 0; k < nBlockSize; k++) {
                // Пропускаем 4 байта заголовка в ответе
                // srcIdx = source index (индекс в массиве-источнике, т.е. в arrRead)
                int srcIdx = k + 4; // !! Отличие от CPP: +4 получилось из суммы: 3 из исходников cpp + 1 hid operation id !!

                // Смещение в result: 3 начальных нуля + предыдущие блоки
                // dstIdx = destination index (индекс в целевом массиве, т.е. в result)
                int dstIdx = i * nBlockSize + k + 3;// +3 из исходников cpp

                //Если ?????,  то не смещаем???
                if (srcIdx < arrRead.length && dstIdx < result.length) {
                    result[dstIdx] = arrRead[srcIdx];
                }
            }
        }
        hidController.printArrayLikeDeviceMonitor(result);
        return result;
    }

    public void readResponse(HidDevice device) {
        hidController.readResponse(device);
    }
}
