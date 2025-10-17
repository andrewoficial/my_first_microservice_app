package org.example.gui.mgstest.transport.cmd.mgs;

import org.example.gui.mgstest.model.HidSupportedDevice;
import org.example.gui.mgstest.service.MgsExecutionListener;
import org.example.gui.mgstest.transport.*;
import org.example.gui.mgstest.transport.cmd.AbstractCommand;

public class DoBlinkTest extends AbstractCommand implements DeviceCommand{
    private byte commandNumber = 0x27;

    @Override
    public byte getCommandNumber() {
        return commandNumber;
    }

    @Override
    public byte[] getAnswerOffsets() {
        return new byte[]{0x00};
    }

    @Override
    public void execute(HidSupportedDevice device, CommandParameters parameters, MgsExecutionListener progress) throws Exception {
        PayLoadSender sender  = new PayLoadSender();
        byte [] answer = sender.writeDataHid(device, PayloadBuilder.buildMgs(this), progress, getDescription(), this);
        progress.onExecutionFinished(device, 100, answer, this.getName());
    }

    @Override
    public String getDescription() {
        return HidCommandName.DO_TEST_BLINK.getName();
    }

    @Override
    public HidCommandName getName() {
        return HidCommandName.DO_TEST_BLINK;
    }
}