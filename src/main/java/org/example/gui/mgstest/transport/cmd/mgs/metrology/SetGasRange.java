package org.example.gui.mgstest.transport.cmd.mgs.metrology;

import org.example.gui.mgstest.model.HidSupportedDevice;
import org.example.gui.mgstest.service.MgsExecutionListener;
import org.example.gui.mgstest.transport.*;
import org.example.gui.mgstest.transport.cmd.AbstractCommand;

public class SetGasRange extends AbstractCommand implements DeviceCommand{
    private byte commandNumber = 0x2B;

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
        for (int coefficient : parameters.getIntArguments()) {
            this.addArgument((char)coefficient);
        }
        PayLoadSender sender  = new PayLoadSender();
        sender.writeDataHid(device, PayloadBuilder.buildMgs(this), progress, getDescription(), this);
    }

    @Override
    public String getDescription() {
        return HidCommandName.SET_GAS_RANGE.getName();
    }

    @Override
    public HidCommandName getName() {
        return HidCommandName.SET_GAS_RANGE;
    }
}