package org.example.gui.mgstest.transport.commands;

import org.apache.log4j.Logger;
import org.example.gui.mgstest.service.MgsExecutionListener;
import org.example.gui.mgstest.transport.CommandParameters;
import org.example.gui.mgstest.transport.DeviceCommand;
import org.example.gui.mgstest.transport.CradleCommunicationHelper;
import org.example.gui.mgstest.transport.HidCommandName;
import org.hid4java.HidDevice;

public class DoRebootDevice implements DeviceCommand {
    private final CradleCommunicationHelper communicator = new CradleCommunicationHelper();
    private final Logger log = Logger.getLogger(DoRebootDevice.class);

    @Override
    public void execute(HidDevice device, CommandParameters parameters, MgsExecutionListener progress) throws Exception {
        progress.onProgressUpdate(device,2, "Opening device...");
        device.open();
        communicator.doSettingsBytes(device);
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
        progress.onExecutionFinished(device, 100, payloads, this.getName());
    }
    @Override
    public String getDescription() {
        return "Do device reboot 0x17";
    }

    @Override
    public HidCommandName getName() {
        return HidCommandName.DO_REBOOT;
    }
}
