package org.example.gui.mgstest.transport.cmd;

import org.example.gui.mgstest.service.MgsExecutionListener;
import org.example.gui.mgstest.transport.*;
import org.example.services.comPort.StringEndianList;
import org.hid4java.HidDevice;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public abstract class AbstractSendCommand implements CommandModel, DeviceCommand {
    protected byte commandNumber; // Set in subclasses
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

    // Stub unused methods
    @Override
    public void addArgument(byte arg) {
        // Not used for Send commands
    }

    @Override
    public void addArgument(int arg) {
        // Not used for Send commands
    }

    @Override
    public void addArgument(float arg) {
        // Not used for Send commands
    }

    @Override
    public void addArguments(float[] args) {
        // Not used for Send commands
    }

    @Override
    public void addArgument(long arg) {
        // Not used for Send commands
    }

    @Override
    public void execute(HidDevice device, CommandParameters parameters, MgsExecutionListener progress) throws Exception {
        this.addArgument(parameters.getStringArgument(), parameters.getEndian());
        PayLoadSender sender  = new PayLoadSender();
        byte[] answer = sender.writeDataHid(device, PayloadBuilder.build(this), progress, getDescription(), this);

        progress.onExecutionFinished(device, 100, answer, this.getName());
    }

    //return new byte[]{0x00, 0x08, 0x10, 0x18, 0x20};
    @Override
    public byte[] getAnswerOffsets() {
        return new byte[]{0x00, 0x08, 0x10, 0x18, 0x20};
    }

}