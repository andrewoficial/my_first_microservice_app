package org.example.gui.mgstest.transport;

import org.apache.log4j.Logger;
import org.example.gui.mgstest.device.AdvancedResponseParser;
import org.example.gui.mgstest.device.ResponseParser;
import org.example.gui.mgstest.tabs.TabInfo;
import org.example.gui.mgstest.transport.commands.GetDeviceInfoCommand;
import org.example.utilites.MyUtilities;
import org.hid4java.HidDevice;
import org.sonatype.aether.transfer.TransferCancelledException;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.CRC32;

public class CradleController {

    private final Logger log = Logger.getLogger(CradleController.class);
    private CradleCommunicationHelper communicator;
    private final Map<String, CradleCommand> commands = new HashMap<>();

    public CradleController() {
        communicator = new CradleCommunicationHelper();
        registerCommands();
    }

    private void registerCommands() {
        commands.put("deviceInfo", new GetDeviceInfoCommand());
        //commands.put("getAllCoef", new GetAllCoefCommand());
        //commands.put("alarmOff", new AlarmOffCommand());
        // ... другие команды
    }




    // Получение данных о приборе (0x2E)
    public byte[] getDeviceInfo(HidDevice device) throws Exception {
        CradleCommand command = commands.get("deviceInfo");
        if (command == null) {
            throw new IllegalArgumentException("Unknown command: " + "deviceInfo");
        }
        return command.execute(device);
    }
    @FunctionalInterface
    public interface Executable {
        void execute() throws TransferCancelledException;
    }

    private byte[] executeWithRetry(TabInfo.Executable method, int maxAttempts, long delayMs) {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                ;
                return method.execute(); // Успешное выполнение
            } catch (TransferCancelledException e) {
                if (attempt == maxAttempts) return null; // Все попытки исчерпаны
                if (delayMs > 0) {
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                }
            }
        }
        return null;
    }
    public byte[] getAllCoefGui(HidDevice device) {
        byte[] success = executeWithRetry(
                () -> getAllCoef(device),
                2,  // Количество попыток
                0   // Задержка между попытками (мс)
        );
        if (success == null) {
            log.warn("Failed to getAllCoef after multiple attempts");
        }
        return success;
    }
    public byte[] getAllCoef(HidDevice device) throws TransferCancelledException {
        log.info("Run get all coef (0x05)");

        byte[] answer = null;
        byte [] exceptedAns = null;

        communicator.doSettingsBytes(device);

        // 01 02 02 01 0D
        communicator.cradleSwitchOn(device); //NOTE: 0x07, 0x00, 0x00, 0x00, (byte)0xDC DC Похоже на запрос последней команды
        //07 00 00 00 00

        // 01 04 07 02 21 00 00 00
        communicator.resetZeroOffset(device);

        // 01 04 07 02 21 01 03 11 d1 01
        communicator.writeMagikInFirstOffset(device);

        // 01 04 07 02 21 02 0D 54 02 65
        communicator.writeMagikInSecondOffset(device);

        // 01 04 07 02 21 03 6E 00 00 00
        communicator.writeCountInThirdOffset(device, 0x00);

        // 01 04 07 02 21 04 00 05 00 01
        exceptedAns = new byte[]{0x07, (byte)0x80, 0x04, 0x00, 0x78, (byte)0xF0, 0x00};
        answer = communicator.waitForResponse(device,
                () -> communicator.cradleWriteBlock(device, (byte) 0x04, new byte[]{0x00, 0x05, 0x00, 0x01}),  // Команда 0x05 GetAllCoefByte,
                exceptedAns,"",10, 70);
        //07 80 04 00 78 F0 00

        //01 04 07 02 21 05 01 00 00 FE
        communicator.writeMagikInFifthOffset(device);

        // 01 04 07 02 21 06 00 00 00 00
        communicator.writeZeroInSixthOffset(device);

        // 01 04 07 02 21 03 6E 06
        communicator.writeCountInThirdOffset(device, 0x06);

        // 01 04 07 02 21 00 E1 40 FF 01
        communicator.cradleActivateTransmit(device);

        // 01 02 02 00 00
        communicator.cradleSwitchOff(device);

        // 01 02 02 01 0D
        communicator.cradleSwitchOn(device); //NOTE: 0x07, 0x00, 0x00, 0x00, (byte)0xDC DC Похоже на запрос последней команды
        //07 00 00 00 00

        // 01 04 04 02 23 00 07
        // 01 04 04 02 23 08 07
        // 01 04 04 02 23 10 07
        // 01 04 04 02 23 18 07
        // 01 04 04 02 23 20 07
        // 01 04 04 02 23 28 07
        // 01 04 04 02 23 30 07
        // 01 04 04 02 23 38 07
        // 01 04 04 02 23 40 07
        // 01 04 04 02 23 48 07
        log.info("Начинаю считывание блоков");
        //Добавлено после отладки с 200 до 650
        communicator.safetySleep(650);
        byte[] offsets = new byte[]{0x00, 0x08, 0x10, 0x18, 0x20, 0x28, 0x30, 0x38, 0x40, 0x48};
        byte[] payloads = communicator.assembleCget(device, offsets, (byte) 0x07);
        //Ответ зависит от состояния прибора

        // 01 02 02 00 00
        communicator.cradleSwitchOff(device);

        return payloads;
    }

    // Выключение звука (SwitchBeepByte, command 0x22, X=0x01)
    public byte[] alarmOff(HidDevice device) throws TransferCancelledException {
        // 01 02 02 01 0D
        prepareForAlarmChange(device);

        //01 04 07 02 21 05 01 00 00 01
        communicator.cradleWriteBlock(device, (byte) 0x05, new byte[]{(byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x01});

        //01 04 07 02 21 06 1B DF 05 A5
        communicator.cradleWriteBlock(device, (byte) 0x06, new byte[]{(byte) 0x1B, (byte) 0xDF, (byte) 0x05, (byte) 0xA5});

        //01 04 07 02 21 07 FE 00 00 00
        communicator.writeMagikInSeventhOffset(device);

        //01 04 07 02 21 03 6E yy yy 00
        communicator.writeCountInThirdOffset(device, 0x0B);

        // 01 04 07 02 21 00 E1 40 FF 01
        communicator.cradleActivateTransmit(device);

        // 01 02 02 00 00
        communicator.cradleSwitchOff(device);

        // 01 02 02 01 0D
        communicator.cradleSwitchOn(device);
        //07 00 00 00 00

        // 01 04 04 02 23 00 07
        // 01 04 04 02 23 08 07
        // 01 04 04 02 23 10 07
        byte [] offsets = {(byte) 0x00, (byte) 0x08, (byte) 0x10};
        byte [] answer = communicator.assembleCget(device, offsets, (byte) 0x07);

        // 01 02 02 00 00
        communicator.cradleSwitchOff(device);
        return answer;
    }

    // Включение звука (SwitchBeepByte, command 0x22, X=0x00)
    public byte[] alarmOn(HidDevice device) throws TransferCancelledException {
        prepareForAlarmChange(device);

        //01 04 07 02 21 05 01 00 00 00
        communicator.cradleWriteBlock(device, (byte) 0x05, new byte[]{(byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00});

        //01 04 07 02 21 06 8D EF 02 D2
        communicator.cradleWriteBlock(device, (byte) 0x06, new byte[]{(byte) 0x8D, (byte) 0xEF, (byte) 0x02, (byte) 0xD2});

        //01 04 07 02 21 07 FE 00 00 00
        communicator.writeMagikInSeventhOffset(device);

        // 01 04 07 02 21 03 6E LL HH 00
        communicator.writeCountInThirdOffset(device, 0x0B);

        // 01 04 07 02 21 00 E1 40 FF 01
        communicator.cradleActivateTransmit(device);

        // 01 02 02 00 00
        communicator.cradleSwitchOff(device);

        // 01 02 02 01 0D
        communicator.cradleSwitchOn(device);

        byte[] offsets = new byte[]{0x00, 0x08, 0x10};
        byte[] payloads = communicator.assembleCget(device, offsets, (byte) 0x07);
        communicator.cradleSwitchOff(device);
        return payloads;
    }

    public void setCoefficientsO2(HidDevice device) throws Exception {
        double[] coefs = new double[19];
        for (int i = 0; i < coefs.length; i++) {
            coefs[i] = i + 4620010.5;
        }
        log.info("Будут заданы " + coefs[0] + "..." + coefs[coefs.length-1] + " для газа ");
        setCoefForGas("o2", coefs, device);
    }

    public void setCoefficientsCO(HidDevice device) throws Exception {
        double[] coefs = new double[14];
        for (int i = 0; i < coefs.length; i++) {
            coefs[i] = i + 201.0;
        }
        log.info("Будут заданы " + coefs[0] + "..." + coefs[coefs.length-1] + " для газа ");
        setCoefForGas("co", coefs, device);
    }

    public void setCoefficientsH2S(HidDevice device) throws Exception {
        double[] coefs = new double[14];
        for (int i = 0; i < coefs.length; i++) {
            coefs[i] = i + 401.0;
        }
        log.info("Будут заданы " + coefs[0] + "..." + coefs[coefs.length-1] + " для газа ");
        setCoefForGas("h2s", coefs, device);
    }


    /**
     *
     * @param gasType [o2] - for 19 values; [co] - 14 values; [h2s] - values
     * @param coefs double array
     * @param device HidDeviceObject
     * @throws Exception
     */
    public void setCoefForGas(String gasType, double[] coefs, HidDevice device) throws Exception {
        // Открыть устройство
        device.open();

        // Определить код газа, байт команды и ожидаемое число коэффициента
        int gasCode, commandByte;
        int expectedNum;
        byte sixEcommand;
        int afterGasCode; //5D - 02; 49 -CO afterGasCode
        switch (gasType.toLowerCase()) {
            case "o2":
                gasCode = 0x61;//(OK)
                commandByte = 0x06;  // SendCoefO2Byte
                expectedNum = 19;
                sixEcommand = 0x56;
                afterGasCode = 0x5D;
                break;
            case "co":
                gasCode = 0x4D;//(OK)
                commandByte = 0x07;  // SendCoefCOByte
                expectedNum = 14;
                sixEcommand = 0x42;
                afterGasCode = 0x49;
                break;
            case "h2s":
                gasCode = 0x63;
                commandByte = 0x08;  // SendCoefH2SByte
                expectedNum = 14;
                sixEcommand = 42;
                afterGasCode = 0x49;
                break;
            case "temp":
                gasCode = 0x0A;  // From Definitions.h SendCoefTempByte 0x0A ToDo check it
                commandByte = 0x0A;
                expectedNum = 14;  // ToDo check it
                sixEcommand = 42;
                afterGasCode = 0x49;
                break;
            default:
                throw new IllegalArgumentException("Unknown gas type: " + gasType);
        }

        // Проверка соответствия длины входного массива ожидаемому количеству коэффициента
        if (coefs.length != expectedNum) {
            throw new IllegalArgumentException(gasType + " coefficients must be exactly " + expectedNum + " values");
        }

        log.info("Run set " + gasType + " coef (0x" + Integer.toHexString(commandByte) + ")");

        byte[] answer = null;
        byte[] exceptedAns = new byte[]{0x07, (byte)0x80, 0x04, 0x00, 0x78, (byte)0xF0, 0x00};

        communicator.cradleSwitchOn(device);

        communicator.resetZeroOffset(device);

        // Запись первого магического байта с кодом газа
        //01 04 07 02 21 01 03 61 D1 01 (OK) - 61 - код газа для О2
        // Write first magik with gas code: 01 04 07 02 21 01 03 [gasCode] D1 01
        int finalGasCode = gasCode;
        answer = communicator.waitForResponse(device,
                () -> communicator.cradleWriteBlock(device, (byte) 0x01, new byte[]{0x03, (byte) finalGasCode, (byte) 0xD1, 0x01}),
                exceptedAns, "", 10, 200);


        //01 04 07 02 21 02 5D 54 02 65 (ОК) (FixME 5D - 02; 49 -CO afterGasCode
        int finalMagikNumber = afterGasCode;
        answer = communicator.waitForResponse(device,
                () -> communicator.cradleWriteBlock(device, (byte) 0x02, new byte[]{(byte)finalMagikNumber, 0x54, 0x02, 0x65}),
                exceptedAns, "", 10, 200);

        //01 04 07 02 21 03 6E 00 00 00 00 00 00 00 (OK)
        communicator.writeCountInThirdOffset(device, 0x00); //(ОК)
        communicator.safetySleep(100);

        //01 04 07 02 21 04 00 06 00 01 00 00 00 00 (для О2 - OK)
        // Write command: 01 04 07 02 21 04 00 [commandByte] 00 01
        answer = communicator.waitForResponse(device,
                () -> communicator.cradleWriteBlock(device, (byte) 0x04, new byte[]{0x00, (byte) commandByte, 0x00, 0x01}),
                exceptedAns, "", 10, 200);

        // Подготовка payload — последовательность float (little-endian)
        byte[] payload = new byte[expectedNum * 4];
        ByteBuffer bb = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN);
        for (double c : coefs) {
            bb.putFloat((float) c);
        }
        //log.info("payload= " + MyUtilities.bytesToHex(payload));

        // Вычисление CRC32 по payload
        CRC32 crc = new CRC32();
        crc.update(payload);
        long crcVal = crc.getValue();

        // Формирование финального массива = payload + CRC
        byte[] crcBytes = ByteBuffer.allocate(4)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putInt((int) crcVal)
                .array();

        byte[] finalArray = new byte[payload.length + 5];
        System.arraycopy(payload, 0, finalArray, 0, payload.length);
        System.arraycopy(crcBytes, 0, finalArray, payload.length, 4);
        finalArray[finalArray.length - 1] = (byte) 0xFE;

        // 3. Отправка: адрес 0x05 — первый байт finalArray
        answer = communicator.waitForResponse(device,
                () -> communicator.cradleWriteBlock(device, (byte) 0x05, new byte[]{
                        0x01, 0x00, 0x00, finalArray[0]  // finalArray[0]
                }),
                exceptedAns, "finalArray first byte", 10, 150);
        communicator.safetySleep(100);

        // Адрес для посылки данных начинается с 0x06
        int addr = 0x06;
        for (int i = 0; i < finalArray.length; i += 4) {
            byte[] arrForSend = Arrays.copyOfRange(finalArray, i + 1 , i + 1 + 4); // +1 потому что 0 уже оправлен по 5 адресу
            log.info(String.format("Send addr %02X: %s", addr, MyUtilities.bytesToHex(arrForSend)));

            int finalAddr = addr;
            answer = communicator.waitForResponse(device,
                    () -> communicator.cradleWriteBlock(device, (byte) finalAddr, arrForSend),
                    exceptedAns, "Adr" + addr, 10, 150);

            addr++;
        }


        //01 04 07 02 21 1A 00 00 00 00 00 00 00 00 00 00 (ОК) (для 02, заполнение нулями)
        communicator.simpleSend(device, new byte[]{0x01, 0x04, 0x07, 0x02, 0x21, (byte)addr, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, 0x00, 0x00, 0x00, 0x00, 0x00});
        communicator.safetySleep(100);
        communicator.readResponse(device);
        communicator.safetySleep(150);

        //01 04 07 02 21 03 6E 56 00 00 00 00 00 00 00 00 (ОК) (FixMe: 56 для О2, для CO - 42
        communicator.simpleSend(device, new byte[]{0x01, 0x04, 0x07, 0x02, 0x21, (byte)0x03, (byte)0x6E, (byte)sixEcommand, (byte)0x00, (byte)0x00, 0x00, 0x00, 0x00, 0x00, 0x00});
        communicator.safetySleep(100);
        communicator.readResponse(device);
        communicator.safetySleep(150);


        //01 04 07 02 21 00 E1 40 FF 01 (ОК)
        communicator.cradleActivateTransmit(device);

        //01 02 02 00 00 00 00 00 00 00 00 00 00 00 00 00 (ОК)
        communicator.cradleSwitchOff(device);

        //01 02 02 01 0D 00 00 00 00 00 00 00 00 00 00 00 (ОК)
        communicator.cradleSwitchOn(device);

        //01 04 04 02 23 00 07 00 00 00 00 00 00 00 00 00 (ОК)
        communicator.simpleSend(device, new byte[]{0x01, 0x04, 0x04, 0x02, 0x23, (byte)0x00, (byte)0x07});
        communicator.safetySleep(100);
        communicator.readResponse(device);
        communicator.safetySleep(150);

        //01 04 04 02 23 00 07 00 00 00 00 00 00 00 00 00 (ОК)
        communicator.simpleSend(device, new byte[]{0x01, 0x04, 0x04, 0x02, 0x23, (byte)0x00, (byte)0x07});
        communicator.safetySleep(100);
        communicator.readResponse(device);
        communicator.safetySleep(150);

        //01 04 04 02 23 08 07 00 00 00 00 00 00 00 00 00 (ОК)
        communicator.simpleSend(device, new byte[]{0x01, 0x04, 0x04, 0x02, 0x23, (byte)0x08, (byte)0x07});
        communicator.safetySleep(100);
        communicator.readResponse(device);
        communicator.safetySleep(150);

        //01 02 02 00 00 00 00 00 00 00 00 00 00 00 00 00 (ОК)
        communicator.simpleSend(device, new byte[]{0x01, 0x02, 0x02, 0x00, 0x00});
        communicator.safetySleep(100);
        communicator.readResponse(device);
        communicator.safetySleep(150);
    }

    /**
     * Sends a MIPEX (optic/UART) command and returns the response.
     * @param commandText The string command to send (e.g., "CALB 0225")
     * @param device HidDevice
     * @return MipexResponse with time and response text
     * @throws Exception
     */
    public MipexResponse sendMipex(String commandText, HidDevice device) throws Exception {
        device.open();
        log.info("Начало настройки");
        log.info("Run send MIPEX command (0x09 SetCommandMIPEXByte)");

        byte[] commandBytes = commandText.getBytes("UTF-8");
        log.info("Строка для отправки:");
        log.info("HEX:");
        log.info(MyUtilities.bytesToHex(commandBytes));
        log.info("DEC:");
        log.info(MyUtilities.byteArrayToString(commandBytes));
        if ( commandBytes.length > 255) {
            throw new IllegalArgumentException("MIPEX command too long: " +  commandBytes.length);
        }

        byte[] answer = null;
        byte[] exceptedAns = new byte[]{0x07, (byte)0x80, 0x04, 0x00, 0x78, (byte)0xF0, 0x00};


        //С этого начинается дамп
        //REQ: 01 02 02 01 0D ANS: 07 00 00 00 00 (ОК)
        log.info("Начало отправки команды");
        communicator.cradleSwitchOn(device);

        //REQ: 01 04 07 02 21 00 00 00 00 00 ANS: 07 80 04 (ОК)
        communicator.resetZeroOffset(device);

        // Сверить с дампом
        //REQ: 01 04 07 02 21 01 03 09 D1 01 ANS: 07 80 04 00 78 F0 00 (ОК)
        // Предпологая, что gasCode 0x09 для MIPEX)
        int gasCode = 0x20;
        answer = communicator.waitForResponse(device,
                () -> communicator.cradleWriteBlock(device, (byte) 0x01, new byte[]{0x03, (byte) gasCode, (byte) 0xD1, 0x01}),
                exceptedAns, "", 3, 150);

        //REQ: 01 04 07 02 21 02 5D 54 02 65 ANS: 07 80 04 00 78 F0 00 (ОК)
        // assume same as O2; adjust if needed
        int afterGasCode = 0x1C;
        answer = communicator.waitForResponse(device,
                () -> communicator.cradleWriteBlock(device, (byte) 0x02, new byte[]{(byte) afterGasCode, 0x54, 0x02, 0x65}),
                exceptedAns, "", 3, 150);

        communicator.writeCountInThirdOffset(device, 0x00);

        //REQ: 01 04 07 02 21 04 00 09 00 01 ANS: 07 80 04 00 78 F0 00 (ОК)
        int commandByte = 0x09;
        answer = communicator.waitForResponse(device,
                () -> communicator.cradleWriteBlock(device, (byte) 0x04, new byte[]{0x00, (byte) commandByte, 0x00, 0x01}),
                exceptedAns, "", 3, 150);

        //REQ: 01 04 07 02 21 05 01 00 00 0A ANS: 07 80 04 00 78 F0 00 (ОК)
        byte prefixByte = 0x0A;
        answer = communicator.waitForResponse(device,
                () -> communicator.cradleWriteBlock(device, (byte) 0x05, new byte[]{0x01, 0x00, 0x00, prefixByte}),
                exceptedAns, "", 3, 150);

        // 1. Подготовка данных для расчёта CRC: prefixByte + commandBytes
        byte[] dataForCrc = new byte[1 + commandBytes.length];
        dataForCrc[0] = prefixByte;
        System.arraycopy(commandBytes, 0, dataForCrc, 1, commandBytes.length);

        // 2. Расчёт CRC32
        CRC32 crc = new CRC32();
        crc.update(dataForCrc);
        long crcVal = crc.getValue();
        byte[] crcBytes = ByteBuffer.allocate(4)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putInt((int) crcVal)
                .array();

        // 3. Формирование финального массива: commandBytes + CRC + FE
        byte[] finalArray = new byte[commandBytes.length + 5];
        System.arraycopy(commandBytes, 0, finalArray, 0, commandBytes.length);
        System.arraycopy(crcBytes, 0, finalArray, commandBytes.length, 4);
        finalArray[finalArray.length - 1] = (byte) 0xFE;

        // Write data blocks starting from 0x06 (4-byte chunks, pad 0x00 if short)
        /*
Нужно отправлять:
        01 04 07 02 21 06 43 41 4C 42
        01 04 07 02 21 07 20 30 32 32
        01 04 07 02 21 08 35 0D 12 F3
        01 04 07 02 21 09 2A 9F FE 00
         */
        int addr = 0x06;
        for (int i = 0; i < finalArray.length; i += 4) {
            byte[] chunk = new byte[4];
            int length = Math.min(4, finalArray.length - i);
            System.arraycopy(finalArray, i, chunk, 0, length);

            // Дополняем последний блок нулями при необходимости
            if (length < 4) {
                Arrays.fill(chunk, length, 4, (byte) 0x00);
            }

            log.info(String.format("Send addr %02X: %s", addr, MyUtilities.bytesToHex(chunk)));

            int finalAddr = addr;
            answer = communicator.waitForResponse(device,
                    () -> communicator.cradleWriteBlock(device, (byte) finalAddr, chunk),
                    exceptedAns, "MIPEX data chunk", 10, 150);
            addr++;
        }

        // REQ: 01 04 07 02 21 03 6E 15 00 00 00 синзронизировано с дампом (ОК)
        // Write total count: 6E + little-endian (expectedNum * 4? But for string, len(commandBytes))
        // Assume totalBytes = commandBytes.length (raw len, not *4 since bytes not floats)
        int totalBytes = commandBytes.length;
        byte low = (byte) (totalBytes & 0xFF);
        byte high = (byte) ((totalBytes >> 8) & 0xFF);
        byte sixEcommand = 0x15;  // Исправлено
        answer = communicator.waitForResponse(device,
                () -> communicator.simpleSend(device, new byte[]{0x01, 0x04, 0x07, 0x02, 0x21, (byte)0x03, (byte)0x6E, (byte)sixEcommand}),
                exceptedAns, "", 10, 70);

        // REQ: 01 04 07 02 21 00 E1 40 FF 01 ANS: 07 08 04 (ОК)
        communicator.cradleActivateTransmit(device);

        // REQ: 01 02 02 ANS: 07 00 00 (ОК)
        communicator.cradleSwitchOff(device);

        // REQ: 01 02 02 01 0D ANS: 07 00 00 00 00 (ОК)
        communicator.cradleSwitchOn(device);

        // Read response: multiple reads as in dump (00 twice, 08, etc.), but parse first valid
        log.info("Начало чтения ответа");
        byte[] offsets = new byte[]{0x00, 0x08, 0x10};

        //   OK
        // 01 04 04 02 23 00 07
        // 01 04 04 02 23 08 07
        // 01 04 04 02 23 10 07
        byte[] payloads = communicator.assembleCget(device, offsets, (byte) 0x07);



        //REQ: 01 02 02 01 0D ANS: 07 00 00 00 00 (ОК)
        communicator.cradleSwitchOn(device);

        List<String> allTextStrings = AdvancedResponseParser.extractAllTextResponses(payloads);
        log.info("Все текстовые строки в ответе: " + allTextStrings);

        return new MipexResponse(5L, allTextStrings.toString());
    }

    private void prepareForAlarmChange(HidDevice device) {
        log.info("Начало настройки");
        // 01 02 02 01 0D
        communicator.cradleSwitchOn(device);

        // 01 04 07 02 21 00 00 00 00 00
        communicator.resetZeroOffset(device);

        // 01 04 07 02 21 01 03 16 D1 01
        communicator.cradleWriteBlock(device, (byte) 0x01, new byte[]{(byte) 0x03, (byte) 0x16, (byte) 0xD1, (byte) 0x01});

        // 01 04 07 02 21 02 12 54 02 65
        communicator.cradleWriteBlock(device, (byte) 0x02, new byte[]{(byte) 0x12, (byte) 0x54, (byte) 0x02, (byte) 0x65});

        // 01 04 07 02 21 03 6E 00 00 00
        communicator.writeCountInThirdOffset(device, 0x00);

        // 01 04 07 02 21 04 00 22 00 01
        communicator.cradleWriteBlock(device, (byte) 0x04, new byte[]{(byte) 0x00, (byte) 0x22, (byte) 0x00, (byte) 0x01});
    }

    // Тест мигания (BlinkByte, command 0x27)
    public byte[] blinkTest(HidDevice device) throws TransferCancelledException {
        // 01 02 02 01 0D
        communicator.cradleSwitchOn(device);

        // 01 04 07 02 21 00 00 00 00 00
        communicator.resetZeroOffset(device);

        // 01 04 07 02 21 01 03 11 D1 01
        communicator.writeMagikInFirstOffset(device);

        //01 04 07 02 21 02 0D 54 02 65
        communicator.writeMagikInSecondOffset(device);

        //01 04 07 02 21 03 6E 00 00 00
        communicator.writeCountInThirdOffset(device, 0x00);

        // 01 04 07 02 21 04 00 27 00 01
        communicator.cradleWriteBlock(device, (byte) 0x04, new byte[]{(byte) 0x00, (byte) 0x27, (byte) 0x00, (byte) 0x01});

        // 01 04 07 02 21 05 01 00 00 FE
        communicator.cradleWriteBlock(device, (byte) 0x05, new byte[]{(byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0xFE});

        // 01 04 07 02 21 06 00 00 00 BD
        communicator.cradleWriteBlock(device, (byte) 0x06, new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xBD});

        // 01 04 07 02 21 07 FE 00 00 00
        communicator.cradleWriteBlock(device, (byte) 0x07, new byte[]{(byte) 0xFE, (byte) 0x00, (byte) 0x00, (byte) 0x00});

        //01 04 07 02 21 03 6E YY YY 00
        communicator.writeCountInThirdOffset(device, 0x06);

        communicator.cradleActivateTransmit(device);

        communicator.cradleSwitchOff(device);

        communicator.cradleSwitchOn(device);
        byte[] offsets = new byte[]{0x00, 0x08, 0x10};
        byte[] payloads = communicator.assembleCget(device, offsets, (byte) 0x07);
        communicator.cradleSwitchOff(device);
        return payloads;
    }


    // Тест звукового сигнала (OneBeepByte, command 0x2F)
    public byte[] beepTest(HidDevice device) throws TransferCancelledException {
        // 01 02 02 01 0D
        communicator.cradleSwitchOn(device);

        // 01 04 07 02 21 00 00 00 00 00
        communicator.resetZeroOffset(device);

        // 01 04 07 02 21 01 03 11 D1 01
        communicator.writeMagikInFirstOffset(device);

        //01 04 07 02 21 02 0D 54 02 65
        communicator.writeMagikInSecondOffset(device);

        //01 04 07 02 21 03 6E 00 00 00
        communicator.writeCountInThirdOffset(device, 0x00);

        communicator.cradleWriteBlock(device, (byte) 0x04, new byte[]{(byte) 0x00, (byte) 0x2F, (byte) 0x00, (byte) 0x01});

        // 01 04 07 02 21 05 01 00 00 FE
        communicator.cradleWriteBlock(device, (byte) 0x05, new byte[]{(byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0xFE});

        // 01 04 07 02 21 06 00 00 00 BD
        communicator.cradleWriteBlock(device, (byte) 0x06, new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xBD});

        // 01 04 07 02 21 07 FE 00 00 00
        communicator.cradleWriteBlock(device, (byte) 0x07, new byte[]{(byte) 0xFE, (byte) 0x00, (byte) 0x00, (byte) 0x00});

        //01 04 07 02 21 03 6E YY YY 00
        communicator.writeCountInThirdOffset(device, 0x06);

        communicator.cradleActivateTransmit(device);

        communicator.cradleSwitchOff(device);

        communicator.cradleSwitchOn(device);
        byte[] offsets = new byte[]{0x00, 0x08, 0x10};
        byte[] payloads = communicator.assembleCget(device, offsets, (byte) 0x07);
        communicator.cradleSwitchOff(device);

        return payloads;
    }

    private void prepareForRunAlarmTest(HidDevice device) {
        // 01 02 02 01 0D
        communicator.cradleSwitchOn(device);

        // 01 04 07 02 21 00 00 00 00 00
        communicator.resetZeroOffset(device);

        // 01 04 07 02 21 01 03 11 D1 01
        communicator.writeMagikInFirstOffset(device);

        //01 04 07 02 21 02 0D 54 02 65
        communicator.writeMagikInSecondOffset(device);

        //01 04 07 02 21 03 6E 00 00 00
        communicator.writeCountInThirdOffset(device, 0x00);
    }

    // Перезагрузка устройства (RebootByte, command 0x17)
    public byte[] rebootCmd(HidDevice device) throws TransferCancelledException {
        // 01 02 02 01 0D
        communicator.cradleSwitchOn(device);

        // 01 04 07 02 21 00 00 00 00 00
        communicator.resetZeroOffset(device);

        // 01 04 07 02 21 01 03 17 D1 01
        communicator.cradleWriteBlock(device, (byte) 0x01, new byte[]{(byte) 0x03, (byte) 0x17, (byte) 0xD1, (byte) 0x01});

        // 01 04 07 02 21 02 13 54 02 65
        communicator.cradleWriteBlock(device, (byte) 0x02, new byte[]{(byte) 0x13, (byte) 0x54, (byte) 0x02, (byte) 0x65});

        // 01 04 07 02 21 03 6E 00 00 00
        communicator.writeCountInThirdOffset(device, 0x00);

        // 01 04 07 02 21 04 00 17 00 01
        communicator.cradleWriteBlock(device, (byte) 0x04, new byte[]{(byte) 0x00, (byte) 0x17, (byte) 0x00, (byte) 0x01});

        // 01 04 07 02 21 05 01 00 00 B9
        communicator.cradleWriteBlock(device, (byte) 0x05, new byte[]{(byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0xB9});

        // 01 04 07 02 21 06 A9 42 1C D4
        communicator.cradleWriteBlock(device, (byte) 0x06, new byte[]{(byte) 0xA9, (byte) 0x42, (byte) 0x1C, (byte) 0xD4});

        // 01 04 07 02 21 07 DB FE 00 00
        communicator.cradleWriteBlock(device, (byte) 0x07, new byte[]{(byte) 0xDB, (byte) 0xFE, (byte) 0x00, (byte) 0x00});

        // 01 04 07 02 21 03 6E yy yy 00
        communicator.writeCountInThirdOffset(device, 0x0C);

        //01 04 07 02 21 00 E1 40 FF 01
        communicator.cradleActivateTransmit(device);

        //01 02 02 00 00
        communicator.cradleSwitchOff(device);

        //01 02 02 01 0D
        communicator.cradleSwitchOn(device);

        // 01 04 04 02 23 00 07
        // 01 04 04 02 23 08 07
        // 01 04 04 02 23 10 07
        byte[] offsets = new byte[]{0x00, 0x08, 0x10};
        byte[] payloads = communicator.assembleCget(device, offsets, (byte) 0x07);
        // Ответ зависит от настроек прибора

        // 01 02 02 00 00
        communicator.cradleSwitchOff(device);
        return payloads;
    }

    // Сброс счётчика батареи (ReplButtByte, command 0x46)
    public byte[] resetBatteryCounter(HidDevice device) throws TransferCancelledException {
        prepareForRunAlarmTest(device);

        // 01 04 07 02 21 04 00 46 00 01
        communicator.cradleWriteBlock(device, (byte) 0x04, new byte[]{(byte) 0x00, (byte) 0x46, (byte) 0x00, (byte) 0x01});

        // 01 04 07 02 21 05 01 00 00 FE
        communicator.writeMagikInFifthOffset(device);

        // 01 04 07 02 21 06 00 00 00 00 00
        communicator.writeZeroInSixthOffset(device);

        // 01 04 07 02 21 07 FE 00 00 00
        communicator.writeMagikInSeventhOffset(device);

        // 01 04 07 02 21 03 6E LL HH 00
        communicator.writeCountInThirdOffset(device, 0x06);

        // 01 04 07 02 21 00 E1 40 FF 01
        communicator.cradleActivateTransmit(device);

        // 01 02 02 00 00
        communicator.cradleSwitchOff(device);

        // 01 02 02 01 0D
        communicator.cradleSwitchOn(device);

        // 01 04 04 02 23 00 07
        // 01 04 04 02 23 08 07
        // 01 04 04 02 23 10 07
        byte[] offsets = new byte[]{0x00, 0x08, 0x10};
        byte[] payloads = communicator.assembleCget(device, offsets, (byte) 0x07);
        //Ответ зависит от состояния прибора

        // 01 02 02 00 00
        communicator.cradleSwitchOff(device);
        return payloads;
    }

    // Установка серийного номера (SetSerialNumberByte, command 0x40)
    public void setSerialNumber(HidDevice device, long serialNumber) {
        if (serialNumber < 0 || serialNumber > 0xFFFFFFFFL) {
            throw new IllegalArgumentException("Serial number must be between 0 and 4294967295");
        }

        // Разбиваем serialNumber на байты (little-endian)
        byte byte0 = (byte) (serialNumber & 0xFF);
        byte byte1 = (byte) ((serialNumber >> 8) & 0xFF);
        byte byte2 = (byte) ((serialNumber >> 16) & 0xFF);
        byte byte3 = (byte) ((serialNumber >> 24) & 0xFF);

        // Данные для CRC (только серийный номер, без 01 00 00)
        byte[] crcData = new byte[]{byte0, byte1, byte2, byte3};
        int crc = calculateCRC(crcData);
        byte[] crcBytes = new byte[]{
                (byte) (crc & 0xFF),
                (byte) ((crc >> 8) & 0xFF),
                (byte) ((crc >> 16) & 0xFF),
                (byte) ((crc >> 24) & 0xFF)
        };

        //01 02 02 01 0D 00 00 00 00 00
        communicator.cradleSwitchOn(device);

        // 01 04 07 02 21 00 00 00 00 00
        communicator.resetZeroOffset(device);

        // 01 04 07 02 21 01 03 19 D1 01
        communicator.cradleWriteBlock(device, (byte) 0x01, new byte[]{(byte) 0x03, (byte) 0x19, (byte) 0xD1, (byte) 0x01});

        // 01 04 07 02 21 02 15 54 02 65
        communicator.cradleWriteBlock(device, (byte) 0x02, new byte[]{(byte) 0x15, (byte) 0x54, (byte) 0x02, (byte) 0x65});

        // 01 04 07 02 21 03 6E 00 00 00
        communicator.writeCountInThirdOffset(device, 0x00);

        // 01 04 07 02 21 04 00 40 00 01
        communicator.cradleWriteBlock(device, (byte) 0x04, new byte[]{(byte) 0x00, (byte) 0x40, (byte) 0x00, (byte) 0x01});

        // 01 04 07 02 21 05 01 00 00 yy
        communicator.cradleWriteBlock(device, (byte) 0x05, new byte[]{(byte) 0x01, (byte) 0x00, (byte) 0x00, byte0});

        // 01 04 07 02 21 06 yy yy yy xx
        communicator.cradleWriteBlock(device, (byte) 0x06, new byte[]{byte1, byte2, byte3, crcBytes[0]});

        // 01 04 07 02 21 07 xx xx xx FE
        communicator.cradleWriteBlock(device, (byte) 0x07, new byte[]{crcBytes[1], crcBytes[2], crcBytes[3], (byte) 0xFE});

        // 01 04 07 02 21 08 00 00 00 00
        communicator.cradleWriteBlock(device, (byte) 0x08, new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00});

        // 01 04 07 02 21 03 6E LL HH 00
        communicator.writeCountInThirdOffset(device, 0x0E);

        // 01 04 07 02 21 00 E1 40 FF 01
        communicator.cradleActivateTransmit(device);

        // 01 02 02 00 00
        communicator.cradleSwitchOff(device);

        // 01 02 02 01 0D
        communicator.cradleSwitchOn(device);

        // 01 02 02 00 00
        communicator.cradleSwitchOff(device);

    }

    // Пример CRC-вычисления (адаптировано из C++: CRC-32 reversed, poly 0xEDB88320)
    // Вычисляет над data (без header/FE)
    public int calculateCRC(byte[] data) {
        int crc = 0xFFFFFFFF;
        int poly = 0xEDB88320;
        for (byte b : data) {
            crc ^= (b & 0xFF);
            for (int i = 0; i < 8; i++) {
                if ((crc & 1) != 0) {
                    crc = (crc >>> 1) ^ poly;
                } else {
                    crc = (crc >>> 1);
                }
            }
        }
        return ~crc;
    }

//FixMe: 711 строчек!!!
}