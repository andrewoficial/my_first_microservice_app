package org.example.gui.mgstest.transport.cmd.mgs.settings;

import org.example.gui.mgstest.model.HidSupportedDevice;
import org.example.gui.mgstest.service.MgsExecutionListener;
import org.example.gui.mgstest.transport.*;
import org.example.gui.mgstest.transport.cmd.AbstractCommand;

public class SetLogTimeOut extends AbstractCommand implements DeviceCommand {
    private byte commandNumber = 0x43;

    @Override
    public byte getCommandNumber() {
        return commandNumber;
    }

    @Override
    public byte[] getAnswerOffsets() {
        return new byte[]{0x00, 0x08, 0x10, 0x18, 0x20};
    }

    @Override
    public void execute(HidSupportedDevice device, CommandParameters parameters, MgsExecutionListener progress) throws Exception {
        this.addArgument((short)parameters.getIntArgument());
        PayLoadSender sender  = new PayLoadSender();
        sender.writeDataHid(device, PayloadBuilder.buildMgs(this), progress, getDescription(), this);
    }

    @Override
    public String getDescription() {
        return HidCommandName.SET_LOG_TIME_OUT.getName();
    }

    @Override
    public HidCommandName getName() {
        return HidCommandName.SET_LOG_TIME_OUT;
    }
}