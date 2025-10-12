package org.example.gui.mgstest.transport.commands;

import org.apache.log4j.Logger;
import org.example.gui.mgstest.service.MgsExecutionListener;
import org.example.gui.mgstest.transport.CommandParameters;
import org.example.gui.mgstest.transport.DeviceCommand;
import org.example.gui.mgstest.transport.CradleCommunicationHelper;
import org.example.gui.mgstest.transport.HidCommandName;
import org.hid4java.HidDevice;

import java.util.ArrayList;

@Deprecated
public class GetDeviceInfoCommandOLD implements DeviceCommand {
    private final CradleCommunicationHelper communicator = new CradleCommunicationHelper();
    private final Logger log = Logger.getLogger(GetDeviceInfoCommandOLD.class);

    @Override
    public void execute(HidDevice device, CommandParameters parameters, MgsExecutionListener progress) throws Exception {
        setStatusExecution(device, progress, "Opening device", 2);
        device.open();

        setStatusExecution(device, progress, "doSettingsBytes", 5);
        communicator.doSettingsBytes(device);

        //01 02 02 01 0D
        setStatusExecution(device, progress, "cradleSwitchOn", 15);
        communicator.cradleSwitchOn(device);

        //01 04 07 02 21 00
        setStatusExecution(device, progress, "resetZeroOffset", 20);
        communicator.resetZeroOffset(device);

        //01 04 07 02 21 00 00 00 00 00
        setStatusExecution(device, progress, "writeMagikInFirstOffset", 21);
        communicator.resetZeroOffset(device);

        //01 04 07 02 21 01 03 11 D1 01
        setStatusExecution(device, progress, "writeMagikInFirstOffset", 23);
        communicator.writeMagikInFirstOffset(device);

        // 01 04 07 02 21 02 0D 54 02 65
        setStatusExecution(device, progress, "writeMagikInSecondOffset", 25);
        communicator.writeMagikInSecondOffset(device);

        // 01 04 07 02 21 03 6E LL HH 00
        setStatusExecution(device, progress, "writeCountInThirdOffset", 27);
        communicator.writeCountInThirdOffset(device, 0x00);

        // 01 04 07 02 21 04 00 2E 00 01
        setStatusExecution(device, progress, "write command block  0x2E in addr 0x04", 30);
        ArrayList<byte[]> answers = new ArrayList<>();
        answers.add(new byte[]{0x07, (byte)0x80, (byte)0x04});
        communicator.waitForResponse(device,
                () -> communicator.cradleWriteBlock(device, (byte) 0x04, new byte[]{0x00, 0x2E, 0x00, 0x01}),
                answers,"",
                5, 3,
                200, 300);

        // 01 04 07 02 21 05 01 00 00 00
        setStatusExecution(device, progress, "writeMagikInFifthOffset", 35);
        communicator.writeMagikInFifthOffset(device);

        //01 04 07 02 21 03 6E LL HH 00
        setStatusExecution(device, progress, "writeCountInThirdOffset", 47);
        communicator.writeCountInThirdOffset(device, 0x06);

//        // 01 04 07 02 21 06 00 00 00 04
//        setStatusExecution(device, progress, "writeMagikInSixthOffset", 50);
//        communicator.writeMagikInSixthOffset(device);

//        // 01 04 07 02 21 03 6E LL HH 00
//        setStatusExecution(device, progress, "writeCountInThirdOffset", 55);
//        communicator.writeCountInThirdOffset(device, 0x06);

        // 01 04 07 02 21 00 E1 40 FF 01 00 00 00 00 00 00
        communicator.cradleActivateTransmit(device);
        setStatusExecution(device, progress, "cradleActivateTransmit", 61);

        // 01 02 02 00 00 00 00 00 00 00 00 00 00 00 00 00
        setStatusExecution(device, progress, "cradleSwitchOff", 65);
        communicator.cradleSwitchOff(device);

        // 01 02 02 01 0D 00 00 00 00 00 00 00 00 00 00 00
        setStatusExecution(device, progress, "cradleSwitchOn", 68);
        communicator.cradleSwitchOn(device);

        // 01 04 04 02 23 00 07
        // 01 04 04 02 23 08 07
        // 01 04 04 02 23 10 07
        setStatusExecution(device, progress, "get device answer", 70);
        byte[] offsets = new byte[] { 0x00, 0x08, 0x10 };
        byte[] assembled = communicator.assembleCget(device, offsets, (byte)0x07);

        // 01 02 02 00 00 00 00 00 00 00 00 00 00 00 00 00
        setStatusExecution(device, progress, "cradleSwitchOff", 95);
        communicator.cradleSwitchOff(device);

//        // 01 02 02 01 0D 00 00 00 00 00 00 00 00 00 00 00
//        setStatusExecution(device, progress, "cradleSwitchOn", 98);
//        communicator.cradleSwitchOn(device);

        setStatusExecution(device, progress, "done", 100);
        progress.onExecutionFinished(device, 100, assembled, this.getName());
    }

    private void setStatusExecution(HidDevice device, MgsExecutionListener progress, String comment, int percent){
        log.info("Do [" + getDescription() + "]... ["+comment+"]:" + percent);
        progress.onProgressUpdate(device, percent, "Do [" + getDescription() + "]... ["+comment+"]");
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