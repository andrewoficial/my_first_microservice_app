package org.example.gui.mgstest.transport;

import org.apache.log4j.Logger;
import org.example.gui.mgstest.exception.MessageDoesNotDeliveredToHidDevice;
import org.example.gui.mgstest.service.MgsExecutionListener;
import org.example.gui.mgstest.transport.cmd.CommandModel;
import org.example.gui.mgstest.util.CrcValidator;
import org.hid4java.HidDevice;

import java.util.ArrayList;

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

    public byte[] writeDataHid(HidDevice device, byte [] payload, MgsExecutionListener progress, String description, CommandModel model) throws MessageDoesNotDeliveredToHidDevice {
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
        communicator.cradleSwitchOn(device);
        for (int i = 0; i < parts.size(); i++) {
            percent = (i * 100) / parts.size() / 3;
            byte [] forSend = parts.get(i);
            System.out.println(bytesToHex(forSend));
            communicator.waitForResponse(device,
                    () -> communicator.simpleSendInitial(device, forSend),
                    answers,"part "+ i + " of " + parts.size(),
                    5, 3, 350, 400);
            setStatusExecution(device, progress, description, bytesToHex(forSend), percent);
        }

        communicator.writeCountInThirdOffset(device, (parts.get(2)[6] - 7));
        communicator.cradleActivateTransmit(device);
        setStatusExecution(device, progress, description, "Write to device", 35);
        communicator.cradleSwitchOff(device);
        communicator.cradleSwitchOn(device);
        setStatusExecution(device, progress, description, "Reboot cradle", 40);


        // 01 04 04 02 23 00 07
        // 01 04 04 02 23 08 07
        // 01 04 04 02 23 10 07
        //byte[] offsets = new byte[]{0x00, 0x08, 0x10};
        byte[] payloads = communicator.assembleCgetNew(device, model.getAnswerOffsets(), (byte) 0x07, progress);
        setStatusExecution(device, progress, description, "Done reading device answer", 85);
        communicator.cradleSwitchOff(device);
        setStatusExecution(device, progress, description, "cradleSwitchOff", 100);
        return payloads;


    }
    private void setStatusExecution(HidDevice device, MgsExecutionListener progress,String description, String comment, int percent){
        log.info("Do [" + description + "]... ["+comment+"]:" + percent);
        progress.onProgressUpdate(device, percent, "Do [" + description + "]... ["+comment+"]");
    }
}
