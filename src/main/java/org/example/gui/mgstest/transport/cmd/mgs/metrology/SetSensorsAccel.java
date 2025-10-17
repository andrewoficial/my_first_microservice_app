package org.example.gui.mgstest.transport.cmd.mgs.metrology;

import org.example.gui.mgstest.model.HidSupportedDevice;
import org.example.gui.mgstest.service.MgsExecutionListener;
import org.example.gui.mgstest.transport.*;
import org.example.gui.mgstest.transport.cmd.AbstractCommand;

public class SetSensorsAccel extends AbstractCommand implements DeviceCommand{
    private byte commandNumber = 0x36;

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
        float[] coefficients = parameters.getCoefficients();
        for (float coefficient : coefficients) {
            addArgument(coefficient);
        }
        PayLoadSender sender  = new PayLoadSender();
        sender.writeDataHid(device, PayloadBuilder.buildMgs(this), progress, getDescription(), this);
    }

    @Override
    public String getDescription() {
        return HidCommandName.SET_SENS_ACCEL.getName();
    }

    @Override
    public HidCommandName getName() {
        return HidCommandName.SET_SENS_ACCEL;
    }
}