package org.example.gui.mgstest.transport.cmd;

import org.example.gui.mgstest.service.MgsExecutionListener;
import org.example.gui.mgstest.transport.*;
import org.example.services.comPort.StringEndianList;
import org.hid4java.HidDevice;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

public class DoBlinkTest implements CommandModel, DeviceCommand {
    private byte commandNumber = 0x27;
    private ArrayList<Byte> arguments = new ArrayList<>();

    @Override
    public byte getCommandNumber() {
        return commandNumber;
    }

    @Override
    public byte[] getArguments() {
        byte[] args = new byte[arguments.size()];
        for (int i = 0; i < arguments.size(); i++) {
            args[i] = arguments.get(i);
        }
        return args;
    }

    @Override
    public byte[] getAnswerOffsets() {
        return new byte[]{0x00};
    }

    @Override
    public void addArgument(byte arg) {
        arguments.add(arg);
    }

    @Override
    public void addArgument(int arg) {
        ByteBuffer bb = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(arg);
        byte[] bytes = bb.array();
        for (byte b : bytes) {
            arguments.add(b);
        }
    }

    @Override
    public void addArgument(float arg) {
        ByteBuffer bb = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(arg);
        byte[] bytes = bb.array();
        for (byte b : bytes) {
            arguments.add(b);
        }
    }

    @Override
    public void addArgument(long arg) {
        ByteBuffer bb = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(arg);
        byte[] bytes = bb.array();
        for (byte b : bytes) {
            arguments.add(b);
        }
    }

    @Override
    public void addArguments(float[] args) {
        for (float arg : args) {
            addArgument(arg);
        }
    }

    public void addArgument(String text, StringEndianList endian ) {

    }

    @Override
    public void execute(HidDevice device, CommandParameters parameters, MgsExecutionListener progress) throws Exception {
        PayLoadSender sender  = new PayLoadSender();
        byte [] answer = sender.writeDataHid(device, PayloadBuilder.build(this), progress, getDescription(), this);
        progress.onExecutionFinished(device, 100, answer, this.getName());
    }

    @Override
    public String getDescription() {
        return "Do Blink Test";
    }

    @Override
    public HidCommandName getName() {
        return HidCommandName.DO_TEST_BLINK;
    }
}