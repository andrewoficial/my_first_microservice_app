package org.example.gui.mgstest.transport.commands;

import org.apache.log4j.Logger;
import org.example.gui.mgstest.device.AdvancedResponseParser;
import org.example.gui.mgstest.transport.CommandParameters;
import org.example.gui.mgstest.transport.CradleCommand;
import org.example.gui.mgstest.transport.CradleCommunicationHelper;
import org.example.gui.mgstest.transport.MipexResponse;
import org.example.utilites.MyUtilities;
import org.hid4java.HidDevice;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.CRC32;

public class SendUartCommand implements CradleCommand {
    private final CradleCommunicationHelper communicator = new CradleCommunicationHelper();
    private final Logger log = Logger.getLogger(SendUartCommand.class);

    @Override
    public byte[] execute(HidDevice device, CommandParameters parameters) throws Exception {
        log.info("Run get device info");
        return sendMipex(parameters.getStringArgument(), device);
    }

    @Override
    public String getDescription() {
        return "Send string to uart";
    }

    /**
     * Sends a MIPEX (optic/UART) command and returns the response.
     * @param commandText The string command to send (e.g., "CALB 0225")
     * @param device HidDevice
     * @return MipexResponse with time and response text
     * @throws Exception
     */
    private byte[] sendMipex(String commandText, HidDevice device) throws Exception {
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
        final ArrayList<byte[]> answerExamples = new ArrayList<>();
        byte[] exceptedAns1 = new byte[]{0x07, (byte)0x80, 0x04, 0x00, 0x78, (byte)0xF0, 0x00};
        byte[] exceptedAns2 = new byte[]{0x07, (byte)0x87, 0x00, 0x00, 0x78};
        answerExamples.add(exceptedAns1);
        answerExamples.add(exceptedAns2);

        //С этого начинается дамп
        //REQ: 01 02 02 01 0D ANS: 07 00 00 00 00 (ОК)
        log.info("Начало отправки команды");
        communicator.cradleSwitchOn(device);

        //REQ: 01 04 07 02 21 00 00 00 00 00 ANS: 07 80 04 (ОК)
        communicator.resetZeroOffset(device);

        // Сверить с дампом
        //ANSWER HEADER: 07 80 04 00 78 F0 00 (ОК)
        //RESPONSES
        // CCS\r [1A] (Complement Checksum:   0x1A) (26) 22 + 4 = 26
        // 01 04 07 02 21 01 03 1A D1 01 00 00 00 00 00 00
        // CALB 09999\r [20] (32) 22 + 11
        // 01 04 07 02 21 01 03 20 D1 01 00 00 00 00 00 00 (len 11;
        // F\r [18] (24)
        // 01 04 07 02 21 01 03 18 D1 01 00 00 00 00 00 00
        // SREV?\r [1C] (28)
        // 01 04 07 02 21 01 03 1C D1 01 00 00 00 00 00 00
        // CALB 0225\r [20] (32)
        // 01 04 07 02 21 01 03 20 D1 01 00 00 00 00 00 00
        // SRAL?\r [1C] (28)
        // 01 04 07 02 21 01 03 1C D1 01 00 00 00 00 00 00
        int gasCode = 22 + commandBytes.length;
        answer = communicator.waitForResponse(device,
                () -> communicator.cradleWriteBlock(device, (byte) 0x01, new byte[]{0x03, (byte) gasCode, (byte) 0xD1, 0x01}),
                answerExamples, "",
                2, 4,
                250, 350);

        //REQ: 01 04 07 02 21 02 5D 54 02 65 ANS: 07 80 04 00 78 F0 00 (ОК)
        // assume same as O2; adjust if needed

        // F =          14 (18+2 = 20 = 14)
        //CCS =         16 (18+4 = 22 = 16)
        //CALB 9999 =   1C (18 + 10 = 28 = 1С)
        //SREV? =       18 (18 + 6 = 24 = 18)
        //CALB 0225 =   1C (18 + 10 = 28 = 1С)
        //SRAL? =       18 (18 + 6 = 24 = 18)
        int afterGasCode = 18 + commandBytes.length;
        answer = communicator.waitForResponse(device,
                () -> communicator.cradleWriteBlock(device, (byte) 0x02, new byte[]{(byte) afterGasCode, 0x54, 0x02, 0x65}),
                answerExamples, "",
                2, 4,
                250, 350);

        //REQ: 01 04 07 02 21 03 6E ANS: 07 80 04
        communicator.writeCountInThirdOffset(device, 0x00);

        //REQ: 01 04 07 02 21 04 00 09 00 01 ANS: 07 80 04 00 78 F0 00 (STATIC VALUE)
        int commandByte = 0x09;
        answer = communicator.waitForResponse(device,
                () -> communicator.cradleWriteBlock(device, (byte) 0x04, new byte[]{0x00, (byte) commandByte, 0x00, 0x01}),
                answerExamples, "",
                2, 4,
                250, 350);


        //Судя по всему длинна посылки
        //REQ: 01 04 07 02 21 05 01 00 00 0A ANS: 07 80 04 00 78 F0 00 (ОК)
        //SRAL? = 01 04 07 02 21 05 01 00 00 06 00 00 00 00 00 00       (06)
        //CALB 0225 = 01 04 07 02 21 05 01 00 00 0A 00 00 00 00 00 00   (0A)
        //SREV? = 01 04 07 02 21 05 01 00 00 06 00 00 00 00 00 00       (06)
        //CALB 9999 = 01 04 07 02 21 05 01 00 00 0A 00 00 00 00 00 00   (0A)
        //CCS = 01 04 07 02 21 05 01 00 00 04 00 00 00 00 00 00         (04)
        //F = 01 04 07 02 21 05 01 00 00 02 00 00 00 00 00 00           (02)
        byte prefixByte = (byte)commandBytes.length;
        answer = communicator.waitForResponse(device,
                () -> communicator.cradleWriteBlock(device, (byte) 0x05, new byte[]{0x01, 0x00, 0x00, prefixByte}),
                answerExamples, "",
                2, 4,
                250, 350);

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
F
 01 04 07 02 21 06 46 0D 42 99 00 00 00 00 00 00
 01 04 07 02 21 07 57 24 FE 00 00 00 00 00 00 00
 01 04 07 02 21 07 57 24 FE 00 00 00 00 00 00 00
CCS
01 04 07 02 21 06 43 43 53 0D 00 00 00 00 00 00
01 04 07 02 21 07 BD E7 F8 27 00 00 00 00 00 00
01 04 07 02 21 08 FE 00 00 00 00 00 00 00 00 00

CALB 9999
 01 04 07 02 21 06 43 41 4C 42 00 00 00 00 00 00
 01 04 07 02 21 07 20 39 39 39 00 00 00 00 00 00
 01 04 07 02 21 07 20 39 39 39 00 00 00 00 00 00
 01 04 07 02 21 08 39 0D 8F B6 00 00 00 00 00 00
  01 04 07 02 21 09 DB E5 FE 00 00 00 00 00 00 00

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
                    answerExamples, "UART_DATA_CHUNK",
                    2, 4,
                    250, 350);
            addr++;
        }

        // REQ: 01 04 07 02 21 03 6E 15 00 00 00 синзронизировано с дампом (ОК)
        // Write total count: 6E + little-endian (expectedNum * 4? But for string, len(commandBytes))
        // Assume totalBytes = commandBytes.length (raw len, not *4 since bytes not floats)
        int nCount = commandBytes.length + 6; // согласно C++ SendCountByte(nLen + 6)
        byte lowByte = (byte) (nCount & 0xFF);
        byte highByte = (byte) ((nCount >> 8) & 0xFF);
        //byte sixEcommand = (byte) (commandBytes.length + 6);  // Исправлено
        answer = communicator.waitForResponse(device,
                () -> communicator.simpleSend(device, new byte[]{0x01, 0x04, 0x07, 0x02, 0x21, (byte)0x03, (byte)0x6E, lowByte, highByte, 0x00}),
                answerExamples, "",
                2, 4,
                250, 350);

        // REQ: 01 04 07 02 21 00 E1 40 FF 01 ANS: 07 08 04 (ОК)
        communicator.cradleActivateTransmit(device);

        // REQ: 01 02 02 ANS: 07 00 00 (ОК)
        communicator.cradleSwitchOff(device);

        // REQ: 01 02 02 01 0D ANS: 07 00 00 00 00 (ОК)
        communicator.cradleSwitchOn(device);

        // Read response: multiple reads as in dump (00 twice, 08, etc.), but parse first valid
        log.info("Начало чтения ответа");
        //byte[] offsets = new byte[]{0x00, 0x08, 0x10};
        byte[] offsets = new byte[]{0x00, 0x08, 0x10};

        //   OK
        // 01 04 04 02 23 00 07
        // 01 04 04 02 23 08 07
        // 01 04 04 02 23 10 07
        byte[] payloads = communicator.assembleCget(device, offsets, (byte) 0x07);



        //REQ: 01 02 02 01 0D ANS: 07 00 00 00 00 (ОК)
        communicator.cradleSwitchOn(device);

//        List<String> allTextStrings = AdvancedResponseParser.extractAllTextResponses(payloads);
//        log.info("Все текстовые строки в ответе: " + allTextStrings);

        //return new MipexResponse(5L, allTextStrings.toString());

        return payloads;
    }

}
