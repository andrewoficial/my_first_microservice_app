package org.example.gui.mgstest.transport.cmd.mkrs;

import org.example.gui.mgstest.model.HidSupportedDevice;
import org.example.gui.mgstest.service.MgsExecutionListener;
import org.example.gui.mgstest.transport.*;
import org.example.gui.mgstest.transport.cmd.AbstractCommand;

public class GetInfo extends AbstractCommand implements DeviceCommand{
    private byte commandNumber = 0x2E;

    @Override
    public byte getCommandNumber() {
        return commandNumber;
    }

    @Override
    public byte[] getAnswerOffsets() {
        return new byte[] { 0x00, 0x08, 0x10 };
    }

    @Override
    public void execute(HidSupportedDevice device, CommandParameters parameters, MgsExecutionListener progress) throws Exception {
        PayLoadSender sender  = new PayLoadSender();
        byte [] preambule = new byte[]{(byte) 0x01, (byte) 0x01, (byte) 0x00, (byte) 0x09, (byte) 0x55, (byte) 0x00, (byte) 0x01, (byte) 0xff, (byte) 0xff, (byte) 0x02, (byte) 0x12, (byte) 0xa2, (byte) 0x8c, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xc4, (byte) 0xdf, (byte) 0x19, (byte) 0x00, (byte) 0x5d, (byte) 0xbb, (byte) 0x40, (byte) 0x00, (byte) 0x30, (byte) 0xe0, (byte) 0x19, (byte) 0x00, (byte) 0xa5, (byte) 0x17, (byte) 0x22, (byte) 0x64, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x40, (byte) 0xa0, (byte) 0x66, (byte) 0x01, (byte) 0xe4, (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x1c, (byte) 0xe0, (byte) 0x19, (byte) 0x00};
        byte [] answer = sender.writeSimpleDataHid(device, preambule, progress, getDescription(), this);

        progress.onExecutionFinished(device, 100, answer, this.getName());
    }

    @Override
    public String getDescription() {
        return HidCommandName.MKRS_GET_INFO.getName();
    }

    @Override
    public HidCommandName getName() {
        return HidCommandName.MKRS_GET_INFO;
    }
}