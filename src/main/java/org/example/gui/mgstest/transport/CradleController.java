package org.example.gui.mgstest.transport;

import org.apache.log4j.Logger;
import org.example.gui.mgstest.transport.commands.GetDeviceInfoCommand;
import org.example.utilites.MyUtilities;
import org.hid4java.HidDevice;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

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

    //Не работает, если делать после GetDeviceInfo. Если команда первая после перезапуска, то рабоатет корректно.
    public byte[] getAllCoef(HidDevice device) throws Exception {
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
    public void alarmOff(HidDevice device) {
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
        communicator.assembleCget(device, offsets, (byte) 0x07);

        // 01 02 02 00 00
        communicator.cradleSwitchOff(device);
    }

    // Включение звука (SwitchBeepByte, command 0x22, X=0x00)
    public void alarmOn(HidDevice device) {
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
    }

    public void setCoefficientsO2(HidDevice device) throws Exception {
        double[] coefs = new double[19];
        for (int i = 0; i < coefs.length; i++) {
            coefs[i] = i + 101.0;
        }
        log.info("Будут заданы " + coefs[0] + "..." + coefs[coefs.length-1] + " для газа ");
        setCoefForGas("o2", coefs, device);
    }

    // Helper to convert double to 4 bytes little-endian float
    private byte[] doubleToLittleEndianBytes(double val) {
        ByteBuffer bb = ByteBuffer.allocate(5).order(ByteOrder.LITTLE_ENDIAN);
        bb.putFloat((float) val);

        byte[] hotFix = new byte[4];
        hotFix[0] = 0;
        hotFix[1] = bb.array()[2];
        hotFix[2] = bb.array()[3];
        hotFix[3] = 0;

        return hotFix;
    }
    // For general setCoef, extend this method with parameters for gas code, command byte, num coefs
    public void setCoefForGas(String gasType, double[] coefs, HidDevice device) throws Exception {
        int gasCode, commandByte;
        int expectedNum;
        switch (gasType.toLowerCase()) {
            case "o2":
                gasCode = 0x61;
                commandByte = 0x06;  // SendCoefO2Byte
                expectedNum = 19;
                break;
            case "co":
                gasCode = 0x62;
                commandByte = 0x07;  // SendCoefCOByte
                expectedNum = 14;
                break;
            case "h2s":
                gasCode = 0x63;
                commandByte = 0x08;  // SendCoefH2SByte
                expectedNum = 14;
                break;
            case "temp":
                gasCode = 0x0A;  // From Definitions.h SendCoefTempByte 0x0A ToDo check it
                commandByte = 0x0A;
                expectedNum = 10;  // ToDo check it
                break;
            default:
                throw new IllegalArgumentException("Unknown gas type: " + gasType);
        }

        if (coefs.length != expectedNum) {
            throw new IllegalArgumentException(gasType + " coefficients must be exactly " + expectedNum + " values");
        }

        log.info("Run set " + gasType + " coef (0x" + Integer.toHexString(commandByte) + ")");

        byte[] answer = null;
        byte[] exceptedAns = new byte[]{0x07, (byte)0x80, 0x04, 0x00, 0x78, (byte)0xF0, 0x00};

        communicator.doSettingsBytes(device);

        communicator.cradleSwitchOn(device);

        communicator.resetZeroOffset(device);

        // Write first magik with gas code: 01 04 07 02 21 01 03 [gasCode] D1 01
        gasCode = 0x61;//ИСПРАВИЛ ДЛЯ КИСЛОРОДА СОГЛАСНО ДАМПУ ПРОГИ
        answer = communicator.waitForResponse(device,
                () -> communicator.cradleWriteBlock(device, (byte) 0x01, new byte[]{0x03, (byte) 0x61, (byte) 0xD1, 0x01}),
                exceptedAns, "", 10, 200);


        // Write second magik: fixed 5D 54 02 65 (ОК)
        answer = communicator.waitForResponse(device,
                () -> communicator.cradleWriteBlock(device, (byte) 0x02, new byte[]{0x5D, 0x54, 0x02, 0x65}),
                exceptedAns, "", 10, 200);

        communicator.writeCountInThirdOffset(device, 0x00); //(ОК)
        communicator.safetySleep(100);

        // Write command: 01 04 07 02 21 04 00 [commandByte] 00 01
        //исправил согласно дампу
        answer = communicator.waitForResponse(device,
                () -> communicator.cradleWriteBlock(device, (byte) 0x04, new byte[]{0x00, (byte) 0x06, 0x00, 0x01}),
                exceptedAns, "", 10, 200);

        // Write magik in fifth: 01 00 00 00 (Исправил)
        answer = communicator.waitForResponse(device,
                () -> communicator.cradleWriteBlock(device, (byte) 0x05, new byte[]{0x01, 0x00, 0x00, (byte) 0x00}),
                exceptedAns, "Write magik in fifth", 10, 250);

        // Write data blocks starting from 0x06
        for (int i = 0; i < expectedNum; i++) {
            byte[] coefBytes = doubleToLittleEndianBytes(coefs[i]);
            final byte address = (byte) (0x06 + i);
            answer = communicator.waitForResponse(device,
                    () -> communicator.cradleWriteBlock(device, (address), coefBytes),
                    exceptedAns, "", 10, 150);
            communicator.safetySleep(100);
        }

        communicator.simpleSend(device, new byte[]{0x01, 0x04, 0x07, 0x02, 0x21, 0x19, (byte)0x97, (byte)0xDD, (byte)0xD8, (byte)0xFE, 0x00, 0x00, 0x00, 0x00, 0x00});
        communicator.safetySleep(100);
        communicator.readResponse(device);
        communicator.safetySleep(150);

        communicator.simpleSend(device, new byte[]{0x01, 0x04, 0x07, 0x02, 0x21, (byte)0x1A, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, 0x00, 0x00, 0x00, 0x00, 0x00});
        communicator.safetySleep(100);
        communicator.readResponse(device);
        communicator.safetySleep(150);

        communicator.simpleSend(device, new byte[]{0x01, 0x04, 0x07, 0x02, 0x21, (byte)0x03, (byte)0x6E, (byte)0x56, (byte)0x00, (byte)0x00, 0x00, 0x00, 0x00, 0x00, 0x00});
        communicator.safetySleep(100);
        communicator.readResponse(device);
        communicator.safetySleep(150);

//        // Write total count: 6E + little-endian (expectedNum * 4)
//        int totalBytes = expectedNum * 4;
//        byte low = (byte) (totalBytes & 0xFF);
//        byte high = (byte) ((totalBytes >> 8) & 0xFF);
//        answer = communicator.waitForResponse(device,
//                () -> communicator.cradleWriteBlock(device, (byte) 0x03, new byte[]{0x6E, low, high, 0x00}),
//                exceptedAns, "", 10, 70);

        //01 04 07 02 21 00 E1 40 FF 01
        communicator.cradleActivateTransmit(device);

        communicator.cradleSwitchOff(device);

        communicator.cradleSwitchOn(device);

        communicator.simpleSend(device, new byte[]{0x01, 0x04, 0x04, 0x02, 0x23, (byte)0x00, (byte)0x07});
        communicator.safetySleep(100);
        communicator.readResponse(device);
        communicator.safetySleep(150);

        communicator.simpleSend(device, new byte[]{0x01, 0x04, 0x04, 0x02, 0x23, (byte)0x00, (byte)0x07});
        communicator.safetySleep(100);
        communicator.readResponse(device);
        communicator.safetySleep(150);

        communicator.simpleSend(device, new byte[]{0x01, 0x04, 0x04, 0x02, 0x23, (byte)0x08, (byte)0x07});
        communicator.safetySleep(100);
        communicator.readResponse(device);
        communicator.safetySleep(150);

        communicator.simpleSend(device, new byte[]{0x01, 0x04, 0x04, 0x02, 0x23, (byte)0x08, (byte)0x07});
        communicator.safetySleep(100);
        communicator.readResponse(device);
        communicator.safetySleep(150);

        communicator.simpleSend(device, new byte[]{0x01, 0x02, 0x02, 0x00, 0x00});
        communicator.safetySleep(100);
        communicator.readResponse(device);
        communicator.safetySleep(150);
    }

    private void prepareForAlarmChange(HidDevice device) {
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
    public void blinkTest(HidDevice device) {
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
    }


    // Тест звукового сигнала (OneBeepByte, command 0x2F)
    public void beepTest(HidDevice device) {
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
    public void rebootCmd(HidDevice device) {
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
    }

    // Сброс счётчика батареи (ReplButtByte, command 0x46)
    public void resetBatteryCounter(HidDevice device) {
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