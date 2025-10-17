package org.example.gui.mgstest.transport.cmd.mgs.metrology;

import org.example.gui.mgstest.model.HidSupportedDevice;
import org.example.gui.mgstest.model.answer.GetSensStatusModel;
import org.example.gui.mgstest.service.MgsExecutionListener;
import org.example.gui.mgstest.transport.*;
import org.example.gui.mgstest.transport.cmd.AbstractCommand;

public class SetSensStatus extends AbstractCommand implements DeviceCommand{
    private byte commandNumber = 0x44;

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
        GetSensStatusModel model = parameters.getSensStatusModel();
        this.addArgument(model.getO2_num());
        this.addArgument(model.getCO_num());
        this.addArgument(model.getH2S_num());
        this.addArgument(model.getCH4_num());

        PayLoadSender sender  = new PayLoadSender();
        sender.writeDataHid(device, PayloadBuilder.buildMgs(this), progress, getDescription(), this);
    }

    @Override
    public String getDescription() {
        return HidCommandName.SET_SENS_STATUS.getName();
    }

    @Override
    public HidCommandName getName() {
        return HidCommandName.SET_SENS_STATUS;
    }
}