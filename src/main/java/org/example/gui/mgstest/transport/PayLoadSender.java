package org.example.gui.mgstest.transport;

import org.apache.log4j.Logger;
import org.example.gui.mgstest.exception.MessageDoesNotDeliveredToHidDevice;
import org.example.gui.mgstest.model.HidSupportedDevice;
import org.example.gui.mgstest.service.MgsExecutionListener;
import org.example.gui.mgstest.transport.cmd.CommandModel;
import org.example.gui.mgstest.util.CrcValidator;
import org.hid4java.HidDevice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.example.gui.mgstest.util.CrcValidator.bytesToHex;

public class PayLoadSender {
    private final CradleCommunicationHelper communicator = new CradleCommunicationHelper();
    private final Logger log = Logger.getLogger(PayLoadSender.class);

    public void writeDataEmulator(byte [] payload) {
        ArrayList<byte[]> parts = CrcValidator.splitIntoParts(payload, 64);
        for (int i = 0; i < parts.size(); i++) {
            String partNumber = String.format("%02X", i);
            System.out.println("01 04 07 02 21 " + partNumber + " " + bytesToHex(parts.get(i)));
        }
    }

    public byte[] writeDataHid(HidSupportedDevice supportedDevice, byte [] payload, MgsExecutionListener progress, String description, CommandModel model) throws MessageDoesNotDeliveredToHidDevice {
        HidDevice device = supportedDevice.getHidDevice();
        device.open();
        ArrayList<byte[]> parts = CrcValidator.addPrefixWriteToParts(payload);
        int percent = 0;
        ArrayList <byte []> answers = new ArrayList<>();
        answers.add(new byte[]{0x07, (byte)0x00, (byte)0x03});
        answers.add(new byte[]{0x07, (byte)0x00, (byte)0x07});
        answers.add(new byte[]{0x07, (byte)0x55, (byte)0x00});
        answers.add(new byte[]{0x07, (byte)0x80, (byte)0x00});
        answers.add(new byte[]{0x07, (byte)0x80, (byte)0x03});
        answers.add(new byte[]{0x07, (byte)0x80, (byte)0x04, 0x00});
        answers.add(new byte[]{0x07, (byte)0x80, (byte)0x07});
        answers.add(new byte[]{0x07, (byte)0x80, (byte)0x12});
        answers.add(new byte[]{0x07, (byte)0x87});
        answers.add(new byte[]{0x07, (byte)0x83});
        answers.add(new byte[]{0x07, (byte)0x80, (byte)0x04, (byte)0x00});
        answers.add(new byte[]{0x07, (byte)0x8E, (byte)0x04, (byte)0x00});
        communicator.cradleSwitchOn(supportedDevice);
        for (int i = 0; i < parts.size(); i++) {
            percent = (i * 100) / parts.size() / 3;
            byte [] forSend = parts.get(i);
            System.out.println(bytesToHex(forSend));
            communicator.waitForResponse(supportedDevice,
                    () -> communicator.simpleSendInitial(device, forSend),
                    answers,"part "+ i + " of " + parts.size(),
                    5, 3, 350, 400);
            setStatusExecution(supportedDevice, progress, description, bytesToHex(forSend), percent);
        }

        communicator.writeCountInThirdOffset(supportedDevice, (parts.get(2)[6] - 7));
        communicator.cradleActivateTransmit(supportedDevice);
        setStatusExecution(supportedDevice, progress, description, "Write to device", 35);
        communicator.cradleSwitchOff(supportedDevice);
        communicator.cradleSwitchOn(supportedDevice);
        setStatusExecution(supportedDevice, progress, description, "Reboot cradle", 40);


        // 01 04 04 02 23 00 07
        // 01 04 04 02 23 08 07
        // 01 04 04 02 23 10 07
        //byte[] offsets = new byte[]{0x00, 0x08, 0x10};
        byte[] payloads = communicator.assembleCgetNew(supportedDevice, model.getAnswerOffsets(), (byte) 0x07, progress);
        setStatusExecution(supportedDevice, progress, description, "Done reading device answer", 85);
        communicator.cradleSwitchOff(supportedDevice);
        setStatusExecution(supportedDevice, progress, description, "cradleSwitchOff", 100);
        return payloads;


    }

    public byte[] writeSimpleDataHid(HidSupportedDevice supportedDevice, byte[] payload,
                                     MgsExecutionListener progress, String description,
                                     CommandModel model) throws MessageDoesNotDeliveredToHidDevice {

        HidDevice device = supportedDevice.getHidDevice();
        device.open();

        // Подготавливаем ответы для ожидания
        ArrayList <byte []> answers = new ArrayList<>();
        answers.add(new byte[]{0x04, (byte)0x01});
        answers.add(new byte[]{0x02});


        // Отправляем команду и получаем первый пакет ответа
        byte[] firstResponse = communicator.waitForResponse(supportedDevice,
                () -> communicator.simpleSendInitial(device, payload),
                answers,
                "Single command execution",
                5, 3, 350, 400);

        // Собираем все части ответа
        List<Byte> allResponseData = new ArrayList<>(64);

        // Добавляем первый пакет
        for (byte b : firstResponse) {
            allResponseData.add(b);
        }

        // Читаем дополнительные пакеты если нужно (count находится во втором байте)
        byte packetCount = firstResponse[1];
        for (int i = 1; i < packetCount; i++) {
            log.info("Считываю дополнительную часть {"+ i +"}/{"+(packetCount - 1)+"}");
            byte[] additionalPacket = communicator.readResponse(device);
            for (byte b : additionalPacket) {
                allResponseData.add(b);
            }
        }

        // Конвертируем List<Byte> в byte[]
        byte[] finalResponse = new byte[allResponseData.size()];
        for (int i = 0; i < finalResponse.length; i++) {
            finalResponse[i] = allResponseData.get(i);
        }

        // Логируем статус
        setStatusExecution(supportedDevice, progress, description,
                "Command: " + bytesToHex(payload), 50);
        setStatusExecution(supportedDevice, progress, description,
                "Done reading device answer", 100);

        log.info("Полный ответ ({"+ finalResponse.length +"} байт): {" + bytesToHex(finalResponse) + "}");

        return finalResponse;
    }
    private void setStatusExecution(HidSupportedDevice device, MgsExecutionListener progress,String description, String comment, int percent){
        log.info("Do [" + description + "]... ["+comment+"]:" + percent);
        progress.onProgressUpdate(device, percent, "Do [" + description + "]... ["+comment+"]");
    }
}
