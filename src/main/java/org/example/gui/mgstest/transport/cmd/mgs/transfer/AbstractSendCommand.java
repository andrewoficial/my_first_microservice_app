package org.example.gui.mgstest.transport.cmd.mgs.transfer;

import org.example.gui.mgstest.model.HidSupportedDevice;
import org.example.gui.mgstest.service.MgsExecutionListener;
import org.example.gui.mgstest.transport.*;
import org.example.gui.mgstest.transport.cmd.AbstractCommand;
import org.example.services.comPort.StringEndianList;

public abstract class AbstractSendCommand extends AbstractCommand implements DeviceCommand {
    protected byte commandNumber;
    private byte[] commandBytes;

    @Override
    public void addArgument(String text, StringEndianList endian ) {
        text = text.trim();
        if (!text.endsWith(endian.getString())) {
            text += endian.getString();
        }
        commandBytes = text.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    @Override
    public byte getCommandNumber() {
        return commandNumber;
    }

    @Override
    public byte[] getArguments() {
        return commandBytes != null ? commandBytes : new byte[0];
    }

    @Override
    public void execute(HidSupportedDevice device, CommandParameters parameters, MgsExecutionListener progress) throws Exception {
        this.addArgument(parameters.getStringArgument(), parameters.getEndian());
        PayLoadSender sender  = new PayLoadSender();
        byte[] answer = sender.writeDataHid(device, PayloadBuilder.buildMgs(this), progress, getDescription(), this);

        progress.onExecutionFinished(device, 100, answer, this.getName());
    }

    @Override
    public byte[] getAnswerOffsets() {
        return new byte[]{0x00, 0x08, 0x10, 0x18, 0x20};
    }
}