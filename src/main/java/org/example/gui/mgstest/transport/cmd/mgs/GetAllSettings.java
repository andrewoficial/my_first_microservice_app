package org.example.gui.mgstest.transport.cmd.mgs;

import org.example.gui.mgstest.model.HidSupportedDevice;
import org.example.gui.mgstest.service.MgsExecutionListener;
import org.example.gui.mgstest.transport.*;
import org.example.gui.mgstest.transport.cmd.AbstractCommand;

public class GetAllSettings extends AbstractCommand implements DeviceCommand{
    private byte commandNumber = 0x66;

    @Override
    public byte getCommandNumber() {
        return commandNumber;
    }

    @Override
    public byte[] getAnswerOffsets() {
        return new byte[]{0x00, 0x08, 0x10, 0x18, 0x20, 0x28};
    }

    @Override
    public void execute(HidSupportedDevice device, CommandParameters parameters, MgsExecutionListener progress) throws Exception {
        PayLoadSender sender  = new PayLoadSender();
        byte [] answer = sender.writeDataHid(device, PayloadBuilder.buildMgs(this), progress, getDescription(), this);
        progress.onExecutionFinished(device, 100, answer, this.getName());
    }

    @Override
    public String getDescription() {
        return HidCommandName.GET_SETTINGS.getName();
    }

    @Override
    public HidCommandName getName() {
        return HidCommandName.GET_SETTINGS;
    }
}