package org.example.gui.mgstest.transport.commands;

import org.apache.log4j.Logger;
import org.example.gui.mgstest.service.MgsExecutionListener;
import org.example.gui.mgstest.transport.CommandParameters;
import org.example.gui.mgstest.transport.DeviceCommand;
import org.example.gui.mgstest.transport.CradleCommunicationHelper;
import org.example.gui.mgstest.transport.HidCommandName;
import org.hid4java.HidDevice;

public class SetAlarmOff implements DeviceCommand {
    private final CradleCommunicationHelper communicator = new CradleCommunicationHelper();
    private final Logger log = Logger.getLogger(SetAlarmOff.class);

    @Override
    public void execute(HidDevice device, CommandParameters parameters, MgsExecutionListener progress) throws Exception {
        setStatusExecution(device, progress, "Opening device", 2);
        device.open();

        setStatusExecution(device, progress, "doSettingsBytes", 7);
        communicator.doSettingsBytes(device);

        // 01 02 02 01 0D
        setStatusExecution(device, progress, "cradleSwitchOn", 10);
        communicator.cradleSwitchOn(device);

        // 01 04 07 02 21 00 00 00 00 00
        setStatusExecution(device, progress, "resetZeroOffset", 15);
        communicator.resetZeroOffset(device);

        // 01 04 07 02 21 01 03 16 D1 01
        setStatusExecution(device, progress, "cradleWriteBlock 01", 19);
        communicator.cradleWriteBlock(device, (byte) 0x01, new byte[]{(byte) 0x03, (byte) 0x16, (byte) 0xD1, (byte) 0x01});

        // 01 04 07 02 21 02 12 54 02 65
        setStatusExecution(device, progress, "cradleWriteBlock 02", 23);
        communicator.cradleWriteBlock(device, (byte) 0x02, new byte[]{(byte) 0x12, (byte) 0x54, (byte) 0x02, (byte) 0x65});

        // 01 04 07 02 21 03 6E 00 00 00
        setStatusExecution(device, progress, "writeCountInThirdOffset", 28);
        communicator.writeCountInThirdOffset(device, 0x00);

        // 01 04 07 02 21 04 00 22 00 01
        setStatusExecution(device, progress, "cradleWriteBlock 04", 35);
        communicator.cradleWriteBlock(device, (byte) 0x04, new byte[]{(byte) 0x00, (byte) 0x22, (byte) 0x00, (byte) 0x01});

        // 01 04 07 02 21 05 01 00 00 01
        setStatusExecution(device, progress, "cradleWriteBlock 05", 37);
        communicator.cradleWriteBlock(device, (byte) 0x05, new byte[]{(byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x01});

        // 01 04 07 02 21 06 1B DF 05 A5
        setStatusExecution(device, progress, "cradleWriteBlock 06", 43);
        communicator.cradleWriteBlock(device, (byte) 0x06, new byte[]{(byte) 0x1B, (byte) 0xDF, (byte) 0x05, (byte) 0xA5});

        // 01 04 07 02 21 07 FE 00 00 00
        setStatusExecution(device, progress, "writeMagikInSeventhOffset", 53);
        communicator.writeMagikInSeventhOffset(device);

        // 01 04 07 02 21 03 6E LL HH 00
        setStatusExecution(device, progress, "writeCountInThirdOffset 0xOB", 58);
        communicator.writeCountInThirdOffset(device, 0x0B);

        //01 04 07 02 21 00 E1 40 FF 01
        setStatusExecution(device, progress, "cradleActivateTransmit", 61);
        communicator.cradleActivateTransmit(device);

        //01 02 02 00 00
        setStatusExecution(device, progress, "cradleSwitchOff", 65);
        communicator.cradleSwitchOff(device);

        //01 02 02 01 0D
        setStatusExecution(device, progress, "cradleSwitchOn", 68);
        communicator.cradleSwitchOn(device);

        // 01 04 04 02 23 00 07
        // 01 04 04 02 23 08 07
        // 01 04 04 02 23 10 07
        byte[] offsets = new byte[]{0x00, 0x08, 0x10};
        setStatusExecution(device, progress, "get device answer", 70);
        byte[] payloads = communicator.assembleCget(device, offsets, (byte) 0x07);

        // 01 02 02 00 00
        setStatusExecution(device, progress, "cradleSwitchOff", 95);
        communicator.cradleSwitchOff(device);

        setStatusExecution(device, progress, "done", 100);
        progress.onExecutionFinished(device, 100, payloads, this.getName());
    }

    private void setStatusExecution(HidDevice device, MgsExecutionListener progress, String comment, int percent){
        log.info("Do [" + getDescription() + "]... ["+comment+"]:" + percent);
        progress.onProgressUpdate(device, percent, "Do [" + getDescription() + "]... ["+comment+"]");
    }

    @Override
    public String getDescription() {
        return "Alarm set OFF (0x22 to 0x01)";
    }

    @Override
    public HidCommandName getName() {
        return HidCommandName.SET_ALARM_OFF;
    }
}
