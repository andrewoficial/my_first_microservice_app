package org.example.gui.mgstest.transport.commands;

import org.apache.log4j.Logger;
import org.example.gui.mgstest.service.MgsExecutionListener;
import org.example.gui.mgstest.transport.CommandParameters;
import org.example.gui.mgstest.transport.DeviceCommand;
import org.example.gui.mgstest.transport.CradleCommunicationHelper;
import org.example.gui.mgstest.transport.HidCommandName;
import org.example.utilites.MyUtilities;
import org.hid4java.HidDevice;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.zip.CRC32;

public class SetEChemCoefficients implements DeviceCommand {
    private final CradleCommunicationHelper communicator = new CradleCommunicationHelper();
    private final Logger log = Logger.getLogger(SetEChemCoefficients.class);

    @Override
    public void execute(HidDevice device, CommandParameters parameters, MgsExecutionListener progress) throws Exception {
        if(parameters == null){
            throw new Exception("Parameters are null");
        }
        if(parameters.getStringArgument() == null || parameters.getStringArgument().isEmpty()){
            throw new Exception("String for sending is empty or null");
        }

        if(parameters.getCoefficients() == null){
            throw new Exception("Coefficients are null");
        }

        setStatusExecution(device, progress, "Opening device", 5);
        device.open();

        setCoefForGas(parameters.getStringArgument(),parameters.getCoefficients(), device, progress);

        setStatusExecution(device, progress, "done", 100);
        progress.onExecutionFinished(device, 100, null, this.getName());
    }



    /**
     *
     * @param gasType [o2] - for 19 values; [co] - 14 values; [h2s] - values
     * @param coefs double array
     * @param device HidDeviceObject
     * @throws Exception
     */
    public void setCoefForGas(String gasType, double[] coefs, HidDevice device, MgsExecutionListener progress) throws Exception {

        // Определить код газа, байт команды и ожидаемое число коэффициента
        int gasCode, commandByte;
        int expectedNum;
        byte sixEcommand;
        int afterGasCode;
        switch (gasType.toLowerCase()) {
            case "o2":
                gasCode = 0x61;//(97 = 93 + 4)
                commandByte = 0x06;  //(06)  //номер команды
                expectedNum = 0x13;  //(19) // колличество double аргументов
                sixEcommand = 0x56;  //(86 = 93-7)
                afterGasCode = 0x5D; //(93)
                break;
            case "co":
                gasCode = 0x4D;//(OK)
                commandByte = 0x07;  // SendCoefCOByte
                expectedNum = 14;//// колличество double аргументов
                sixEcommand = 0x42;
                afterGasCode = 0x49;
                break;
            case "h2s":
                gasCode = 0x63;
                commandByte = 0x08;  // SendCoefH2SByte
                expectedNum = 14;//// колличество double аргументов
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
        setStatusExecution(device, progress, "Set " + gasType + " coef (0x" + Integer.toHexString(commandByte) + ")", 10);

        byte[] answer = null;
        ArrayList <byte []> answers = new ArrayList<>();
        byte[] exceptedAns = new byte[]{0x07, (byte)0x80, 0x04, 0x00, 0x78, (byte)0xF0, 0x00};
        answers.add(exceptedAns);

        setStatusExecution(device, progress, "Switch on cradle", 12);
        communicator.cradleSwitchOn(device);

        setStatusExecution(device, progress, "Reset zero offset", 15);
        communicator.resetZeroOffset(device);

        // 01 04 07 02 21 01 03 [gasCode] D1 01
        // 01 04 07 02 21 01 03 61 D1 01 (OK) - 61 - код газа для О2
        setStatusExecution(device, progress, "Write 0x01 with gas code", 20);
        int finalGasCode = gasCode;
        communicator.waitForResponse(device,
                () -> communicator.cradleWriteBlock(device, (byte) 0x01, new byte[]{0x03, (byte) finalGasCode, (byte) 0xD1, 0x01}),
                answers, "BLOCK 1", 5, 3, 300, 400);


        //01 04 07 02 21 02 5D 54 02 65 (ОК) (5D - 02; 49 -CO afterGasCode)
        setStatusExecution(device, progress, "Write 0x02 with afterGasCode", 25);
        int finalMagikNumber = afterGasCode;
        communicator.waitForResponse(device,
                () -> communicator.cradleWriteBlock(device, (byte) 0x02, new byte[]{(byte)finalMagikNumber, 0x54, 0x02, 0x65}),
                answers, "BLOCK 2", 5, 3, 300, 400);

        //01 04 07 02 21 03 6E 00 00 00 00 00 00 00 (OK)
        int finalMagikNumber2 = afterGasCode - 7;
        setStatusExecution(device, progress, "Write 0x03 with 0x00", 30);
        communicator.writeCountInThirdOffset(device, (byte)finalMagikNumber2); //(ОК)

        //01 04 07 02 21 04 00 [commandByte] 00 01
        //01 04 07 02 21 04 00 06 00 01 00 00 00 00 (для О2 - OK)
        setStatusExecution(device, progress, "Write 0x04 with commandByte", 35);
        communicator.waitForResponse(device,
                () -> communicator.cradleWriteBlock(device, (byte) 0x04, new byte[]{0x00, (byte) commandByte, 0x00, 0x01}),
                answers, "BLOCK 4", 5, 3, 300, 400);

        byte[] payload = new byte[expectedNum * 4];
        ByteBuffer bb = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN);
        for (double c : coefs) {
            bb.putFloat((float) c);
        }
        //log.info("payload= " + MyUtilities.bytesToHex(payload));

        //ToDo кривая костыльнаялогика
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
        // finalArray[0]
        setStatusExecution(device, progress, "Write 0x05 with first byte of finalArray", 40);
        communicator.waitForResponse(device,
                () -> communicator.cradleWriteBlock(device, (byte) 0x05, new byte[]{0x01, 0x00, 0x00, finalArray[0]}),
                answers, "finalArray first byte on address: 0x05", 5, 3, 300, 500);

        // Адрес для посылки данных начинается с 0x06
        int addr = 0x06;
        for (int i = 0; i < finalArray.length; i += 4) {
            byte[] arrForSend = Arrays.copyOfRange(finalArray, i + 1 , i + 1 + 4); // +1 потому что 0 уже оправлен по 5 адресу
            setStatusExecution(device, progress, "Write "+ String.format("%02X", addr) +" with bytes of finalArray", 40+i);

            int finalAddr = addr;
            communicator.waitForResponse(device,
                    () -> communicator.cradleWriteBlock(device, (byte) finalAddr, arrForSend),
                    answers, "finalArray next on byte address" + addr, 5, 3, 300, 500);

            addr++;
        }


        //01 04 07 02 21 1A 00 00 00 00 00 00 00 00 00 00 (ОК) (для 02, заполнение нулями)
        setStatusExecution(device, progress, "Write "+ String.format("%02X", addr) +" with bytes of filled zero", 65);
        communicator.simpleSend(device, new byte[]{0x01, 0x04, 0x07, 0x02, 0x21, (byte)addr, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, 0x00, 0x00, 0x00, 0x00, 0x00});
        communicator.safetySleep(100);
        communicator.readResponse(device);
        communicator.safetySleep(150);

        //01 04 07 02 21 03 6E 56 00 00 00 00 00 00 00 00 (ОК) (FixMe: 56 для О2, для CO - 42
    //communicator.writeCountInThirdOffset(device, (byte)sixEcommand);
    //setStatusExecution(device, progress, "Write "+ String.format("%02X", (byte)0x03) +"", 68);
//        communicator.simpleSend(device, new byte[]{0x01, 0x04, 0x07, 0x02, 0x21, (byte)0x03, (byte)0x6E, (byte)sixEcommand, (byte)0x00, (byte)0x00, 0x00, 0x00, 0x00, 0x00, 0x00});
//        communicator.safetySleep(100);
//        communicator.readResponse(device);
//        communicator.safetySleep(150);


        //01 04 07 02 21 00 E1 40 FF 01 (ОК)
        setStatusExecution(device, progress, "cradleActivateTransmit", 70);
        communicator.cradleActivateTransmit(device);

        //01 02 02 00 00 00 00 00 00 00 00 00 00 00 00 00 (ОК)
        setStatusExecution(device, progress, "cradleSwitchOff", 72);
        communicator.cradleSwitchOff(device);

        //01 02 02 01 0D 00 00 00 00 00 00 00 00 00 00 00 (ОК)
        setStatusExecution(device, progress, "cradleSwitchOn", 74);
        communicator.cradleSwitchOn(device);


        // 01 04 04 02 23 00 07
        // 01 04 04 02 23 08 07
        // 01 04 04 02 23 10 07
        byte[] offsets = new byte[]{0x00, 0x08, 0x10};
        setStatusExecution(device, progress, "get device answer", 70);
        byte[] payloads = communicator.assembleCget(device, offsets, (byte) 0x07);

        //01 02 02 00 00 00 00 00 00 00 00 00 00 00 00 00 (ОК)
        setStatusExecution(device, progress, "cradleSwitchOff", 95);
        communicator.cradleSwitchOff(device);

    }

    private void setStatusExecution(HidDevice device, MgsExecutionListener progress, String comment, int percent){
        log.info("Do [" + getDescription() + "]... ["+comment+"]:" + percent);
        progress.onProgressUpdate(device, percent, "Do [" + getDescription() + "]... ["+comment+"]");
    }

    @Override
    public String getDescription() {
        return "Set some coefficients ";
    }

    @Override
    public HidCommandName getName() {
        return HidCommandName.SET_ECHEM_COEFF;
    }
}
