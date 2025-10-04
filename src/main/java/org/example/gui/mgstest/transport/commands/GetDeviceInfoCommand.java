package org.example.gui.mgstest.transport.commands;

import org.apache.log4j.Logger;
import org.example.gui.mgstest.service.MgsExecutionListener;
import org.example.gui.mgstest.transport.CommandParameters;
import org.example.gui.mgstest.transport.DeviceCommand;
import org.example.gui.mgstest.transport.CradleCommunicationHelper;
import org.example.gui.mgstest.transport.HidCommandName;
import org.hid4java.HidDevice;

import java.util.ArrayList;

public class GetDeviceInfoCommand implements DeviceCommand {
    private final CradleCommunicationHelper communicator = new CradleCommunicationHelper();
    private final Logger log = Logger.getLogger(GetDeviceInfoCommand.class);

    @Override
    public void execute(HidDevice device, CommandParameters parameters, MgsExecutionListener progress) throws Exception {
        log.info("Run get device info");
        device.open();
        progress.onProgressUpdate(device, 2, "Opening device...");
        byte[] answer = null;
        byte [] exceptedAns = null;

        communicator.doSettingsBytes(device);
        progress.onProgressUpdate(device, 20, "Finish setup cradle");
        //01 02 02 01 0D
        communicator.cradleSwitchOn(device);

        //01 04 07 02 21 00
        communicator.resetZeroOffset(device);
        progress.onProgressUpdate(device, 25, "Getting info...");

        //01 04 07 02 21 01 03 11 D1 01
        communicator.writeMagikInFirstOffset(device);

        // 01 04 07 02 21 02 0D 54 02 65
        communicator.writeMagikInSecondOffset(device);

        // 01 04 07 02 21 03 6E 00 00 00
        communicator.writeCountInThirdOffset(device, 0x00);
        progress.onProgressUpdate(device, 34, "Getting info...");
        // 01 04 07 02 21 04 00 2E 00 01
        ArrayList<byte[]> answers = new ArrayList<>();
        answers.add(new byte[]{0x07, (byte)0x80, (byte)0x04});
        answer = communicator.waitForResponse(device,
                () -> communicator.cradleWriteBlock(device, (byte) 0x04, new byte[]{0x00, 0x2E, 0x00, 0x01}),
                answers,"",
                4, 3,
                60, 40);
        //07 80 04

        // 01 04 07 02 21 05 01 00 00 FE
        communicator.writeMagikInFifthOffset(device);
        progress.onProgressUpdate(device, 47, "Getting info...");
        //01 04 07 02 21 03 6E LL HH 00
        communicator.writeCountInThirdOffset(device, 6);

        // 01 04 07 02 21 06 00 00 00 04 00 00 00 00 00 00
        communicator.writeMagikInSixthOffset(device);

        // 01 04 07 02 21 03 6E 06 00 00 00 00 00 00 00 00
        communicator.writeCountInThirdOffset(device, 0x06);

        // 01 04 07 02 21 00 E1 40 FF 01 00 00 00 00 00 00
        communicator.cradleActivateTransmit(device);

        // 01 02 02 00 00 00 00 00 00 00 00 00 00 00 00 00
        communicator.cradleSwitchOff(device);
        progress.onProgressUpdate(device, 68, "Getting info...");
        // 01 02 02 01 0D 00 00 00 00 00 00 00 00 00 00 00
        communicator.cradleSwitchOn(device);

        // 01 04 04 02 23 00 07
        // 01 04 04 02 23 08 07
        // 01 04 04 02 23 10 07
        byte[] offsets = new byte[] { 0x00, 0x08, 0x10 };
        byte[] assembled = communicator.assembleCget(device, offsets, (byte)0x07);
        // Ответ меняется в зависимости от данных
        progress.onProgressUpdate(device, 89, "Getting info...");
        // 01 02 02 00 00 00 00 00 00 00 00 00 00 00 00 00
        communicator.cradleSwitchOff(device);

        // 01 02 02 01 0D 00 00 00 00 00 00 00 00 00 00 00
        communicator.cradleSwitchOn(device);
        progress.onProgressUpdate(device, 100, "Complete");
        progress.onExecutionFinished(device, 95, assembled, this.getName());
    }

    @Override
    public String getDescription() {
        return "Get device information (0x2E)";
    }

    @Override
    public HidCommandName getName() {
        return HidCommandName.GET_DEV_INFO;
    }
}