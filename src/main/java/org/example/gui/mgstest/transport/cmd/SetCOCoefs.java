package org.example.gui.mgstest.transport.cmd;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

class SetCOCoefs implements CommandModel {
    private byte commandNumber = 0x07;
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
    public void addArguments(float[] args) {
        for (float arg : args) {
            addArgument(arg);
        }
    }
}
