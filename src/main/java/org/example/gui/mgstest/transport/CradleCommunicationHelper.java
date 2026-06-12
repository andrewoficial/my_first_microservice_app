package org.example.gui.mgstest.transport;

import org.apache.log4j.Logger;
import org.example.gui.mgstest.exception.MessageDoesNotDeliveredToHidDevice;
import org.example.gui.mgstest.model.HidSupportedDevice;
import org.example.gui.mgstest.service.MgsExecutionListener;
import org.example.gui.mgstest.transport.hid.SomeHidController;
import org.example.utilites.MyUtilities;
import org.hid4java.HidDevice;

import java.util.ArrayList;

/**
 * CradleCommunicationHelper — низкоуровневое общение с NFC-кредлом через HID.
 *
 * Все команды и последовательности строго соответствуют C++ классу CNFC_Device.
 *
 * Основные этапы (из C++):
 * - ReaderSequence1() → Проверка наличия кредла
 * - ReaderSequence2() → Включение NFC (cradleSwitchOn)
 * - ReaderSequence3() → Чтение Sensor ID (самая важная команда!)
 */
public class CradleCommunicationHelper {
    SomeHidController hidController = new SomeHidController();
    private final Logger log = Logger.getLogger(CradleCommunicationHelper.class);

    // ====================== СЭМПЛЫ ОТВЕТОВ (оставлены без изменений) ======================
    public final ArrayList<byte[]> WRITE_MAGIK_FIRST_OFFSET_ANSWER_SAMPLES = new ArrayList<>();
    public final ArrayList<byte[]> ASSEMBLE_C_GET_WRITE_ANSWER_SAMPLES = new ArrayList<>();
    public final ArrayList<byte[]> CRADLE_ACTIVATE_TRANSMIT_ANSWER_SAMPLES = new ArrayList<>();

    public CradleCommunicationHelper() {

        WRITE_MAGIK_FIRST_OFFSET_ANSWER_SAMPLES.add(new byte[]{0x07, (byte)0x80, (byte)0x04, 0x00});
        WRITE_MAGIK_FIRST_OFFSET_ANSWER_SAMPLES.add(new byte[]{0x07, (byte)0x8E, (byte)0x00, 0x00});

        ASSEMBLE_C_GET_WRITE_ANSWER_SAMPLES.add(new byte[] {0x07, (byte)0x80, (byte)0x24});

        CRADLE_ACTIVATE_TRANSMIT_ANSWER_SAMPLES.add(new byte[]{0x07, (byte)0x80, (byte)0x04, (byte)0x00, (byte)0x78});
        CRADLE_ACTIVATE_TRANSMIT_ANSWER_SAMPLES.add(new byte[]{0x07, (byte)0x8E, (byte)0x04, (byte)0x00, (byte)0x78});

    }

    // ====================================================================================
    //                          READER SEQUENCES (из C++ CNFC_Device)
    // ====================================================================================

    //=============DeviceCommand================//

    /**
     * NFC_ENABLE:
     * REQ: 01 02 02 01 0D
     * ANS: 07 00 00 00 00
     *
     */
    public void cradleSwitchOn(HidSupportedDevice device) {
        byte [] exceptedAns = null;
        byte [] answer = null;

        //REQ_SAMPLE:  01 02 02 01 0D 00 00 00 00 00 00 00 00 00 00 00
        exceptedAns = new byte[]{0x07, (byte)0x00, (byte)0x00};
        ArrayList <byte []> answers = new ArrayList<>();
        answers.add(exceptedAns);
        waitForResponse(device,
                () -> simpleSendInitial(device.getHidDevice(),new byte[]{(byte) 0x01, (byte) 0x02, (byte) 0x02, (byte) 0x01, (byte) 0x0D}),
                answers,"NFC_ENABLE",
                5, 3, 350, 400);


        //ANS_SAMPLE: 07 00 00 00 78 F0 00 C1 31 12 5A 02 E0 FF 00 7F
    }

    /**
     * NFC_DISABLE:
     * REQ: 01 02 02
     * ANS: 07 00 00
     *
     */
    public void cradleSwitchOff(HidSupportedDevice device) {
        byte [] exceptedAns = null;
        byte [] answer = null;

        //REQ_SAMPLE:  01 02 02 00 00 00 00 00 00 00 00 00 00 00 00 00
        ArrayList <byte []> answers = new ArrayList<>();
        answers.add(new byte[]{0x07, (byte)0x00, (byte)0x00, (byte)0x00});
        answers.add(new byte[]{0x07, (byte)0x80});
        answers.add(new byte[]{0x07, (byte)0x83});

        waitForResponse(device,
                () -> simpleSendInitial(device.getHidDevice(),new byte[]{(byte) 0x01, (byte) 0x02, (byte) 0x02}),
                answers,"NFC_DISABLE",
                5, 3, 350, 400);
        //ANS_SAMPLE: 07 00 00 00 78 F0 00 C1 31 12 5A 02 E0 FF 00 7F
    }

    /**
     * NFC_TRANSMIT:
     * REQ: 01 04 07 02 21 00 E1 40 FF 01
     * ANS: 07 08 04
     *
     */
    public void cradleActivateTransmit(HidSupportedDevice device) {
        byte [] answer = null;
        //REQ_SAMPLE:  01 04 07 02 21 00 E1 40 FF 01 00 00 00 00 00 00
        answer = waitForResponse(device,
                () -> simpleSend(device.getHidDevice(),
                        new byte[]{(byte) 0x01, (byte) 0x04, (byte) 0x07, (byte) 0x02, (byte) 0x21, (byte) 0x00, (byte) 0xE1, (byte) 0x40, (byte) 0xFF, (byte) 0x01}),
                CRADLE_ACTIVATE_TRANSMIT_ANSWER_SAMPLES,
                "NFC_TRANSMIT",
                5, 3,
                200, 300);
        //ANS_SAMPLE: 07 80 04 00 78 F0 00 C1 31 12 5A 02 E0 FF 00 7F
    }

    //=============MessageBuild================//

    /**
     * WRITE_THIRD_OFFSET:
     * REQ: 01 04 07 02 21 03 6E LL HH 00
     * ANS: 07 80 04
     * @param device
     *
     */
    public void writeCountInThirdOffset(HidSupportedDevice device, int count){
        byte [] exceptedAns = null;
        byte [] answer = null;
        //01 04 07 02 21 03 6E LL HH 00
        exceptedAns = new byte[]{0x07, (byte)0x80, (byte)0x04};
        answer = waitForResponse(device,
                () -> cradleSendCount(device.getHidDevice(), count),
                WRITE_MAGIK_FIRST_OFFSET_ANSWER_SAMPLES,"WRITE_THIRD_OFFSET Установка счётчика",
                5, 3,
                200, 300);
        //07 80 04
    }

    // ====================================================================================
    //                          LOW-LEVEL WRITE OPERATIONS
    // ====================================================================================

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
     * Запись блока (4 байта data). Формирует 01 04 07 02 21 03 6E LL HH 00
     * Отправка счётчика (overwrite блока 0x03)
     * @param device HidDevice
     * @param count
     */
    public void cradleSendCount(HidDevice device, int count) {
        byte low = (byte) (count & 0xFF);
        byte high = (byte) ((count >> 8) & 0xFF);
        simpleSend(device, new byte[]{(byte) 0x01, (byte) 0x04, (byte) 0x07, (byte) 0x02, (byte) 0x21, (byte) 0x03, (byte) 0x6E, low, high, (byte) 0x00});
    }

    // ====================================================================================
    //                          WAIT + VALIDATION
    // ====================================================================================

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
    public byte[] waitForResponse(HidSupportedDevice device,
                                  Runnable action,
                                  ArrayList<byte[]> expectedValues,
                                  String comment,
                                  int readRepeatLimit,
                                  int writeRepeatLimit,
                                  long delayRead,
                                  long delayWrite) throws MessageDoesNotDeliveredToHidDevice{
        log.info("Начинаю отправку [" + comment + "] команды и чтения ответа readRepeatLimit " + readRepeatLimit +" writeRepeatLimit " + writeRepeatLimit + " delayRead " + delayRead + " delayWrite " + delayWrite);
        int attemptsRead = 0;
        int attemptsWrite = 0;
        byte[] received = null;

        for (int i = 0; i < writeRepeatLimit; i++) {
            action.run();
            attemptsWrite++;
            attemptsRead = 0;
            for(int j = 0; j < readRepeatLimit; j++){
                attemptsRead++;
                safetySleep(delayRead);
                received = hidController.readResponse(device.getHidDevice());
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
        }else{
            log.warn("Превышен предел попыток повтора отправки команды (" + readRepeatLimit + ")");
            throw new MessageDoesNotDeliveredToHidDevice(expectedValues, readRepeatLimit, writeRepeatLimit, comment, device.getHidDevice());
        }
        return received;
    }

    /**
     * Проверка ответа на соответствие заголовка требуемому
     * @param received - Принятый пакет
     * @param expectedValues - Требуемый заголовок
     * @param comment - Комментарий для печати в отладку
     * @return - boolean. True if first part is equal to any expected value.
     */
    public boolean isEqual(byte[] received, ArrayList<byte[]> expectedValues, String comment) {
        if (expectedValues == null || received == null) {
            log.warn("[" + comment + "] передан null аргумент");
            return false;
        }
        if (expectedValues.isEmpty() || received.length == 0) {
            log.warn("[" + comment + "] пустой набор массивов в аргументах");
            return false;
        }
        log.info("Полученный ответ для сравнение с маской: " + MyUtilities.bytesToHexString(received));
        for (byte[] expectedValue : expectedValues) {
            if (received.length < expectedValue.length) {
                log.warn("[" + comment + "] полученный массив короче эталонного");
                continue;
            }


            log.info("В сравнении просматриваю вариант: " + MyUtilities.bytesToHexString(expectedValue));
            boolean isDifferent = false;
            for (int i = 0; i < expectedValue.length; i++) {
                if (received[i] != expectedValue[i]) {
                    log.info("[" + comment + "] несовпадение на позиции для одного из вариантов ответа" + i +
                            " (ожидал " + String.format("%02X", expectedValue[i]) +
                            ", получил " + String.format("%02X", received[i]) + ")");
                    isDifferent = true;
                    break;  // Early exit on first mismatch
                }
            }
            if (!isDifferent) {
                //log.info("Прерываю проверку и отдаю результат true");
                return true;
            }
        }
        return false;
    }

    public void safetySleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ex) {
            log.error("SLEEP ERROR!!! " + ex.getMessage());
            System.out.println("SLEEP ERROR!! "  + ex.getMessage());
        }
    }

    // ====================================================================================
    //                          ASSEMBLE CGET (большой сбор данных)
    // ====================================================================================

    /**
     * assembleCgetNew — сборка больших ответов (например, GetAllCoefByte, GetLog и т.д.)
     * Используется для чтения нескольких блоков по 32 байта.
     *
     * @param device - HidSupportedDevice
     * @param offsets - Перечень адрессов для считвания, например 01 04 04 02 23 20 07; 01 04 04 02 23 28 07; 01 04 04 02 23 30 07
     * @param size - Некий размер, в примерах выше это цифра 07
     * @param progress - слушатель прогресса
     * @return - массив, со вставками
     */
    public byte[] assembleCgetNew (HidSupportedDevice device, byte[] offsets, byte size, MgsExecutionListener progress) throws MessageDoesNotDeliveredToHidDevice {
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
                    () -> hidController.simpleSend(device.getHidDevice(),
                            new byte[]{(byte) 0x01, (byte) 0x04, (byte) 0x04, (byte) 0x02, (byte) 0x23, (byte) off, (byte) size}),
                    ASSEMBLE_C_GET_WRITE_ANSWER_SAMPLES,
                    "ASSEMBLE_C_GET_"+off,
                    3, 7,
                    380, 500);
            log.info("Получена валидная часть ответа ");
            int percent = (((i * 100) / offsets.length) / 3) + 50; // 50 .. 80%
            log.info("Calculated percent: " + percent);
            progress.onProgressUpdate(device, percent, " [ read device answer ] " + off);
            //printArrayLikeDeviceMonitor(arrRead);
            if(! isEqual(arrRead, ASSEMBLE_C_GET_WRITE_ANSWER_SAMPLES, "Проверка на выброс исключения")){
                throw new MessageDoesNotDeliveredToHidDevice(ASSEMBLE_C_GET_WRITE_ANSWER_SAMPLES, 6, 10, "ASSEMBLE_C_GET_"+off, device.getHidDevice());
            }

            // Сборка ответа
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

    public byte[] readResponse(HidDevice device) {
        return hidController.readResponse(device);
    }
}
