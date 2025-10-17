package org.example.gui.mgstest.transport.cmd;

import org.example.services.comPort.StringEndianList;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

public abstract class AbstractCommand implements CommandModel {
    private final byte commandNumber = 0x10;
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
    public void addArgument(short arg) {
        ByteBuffer bb = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(arg);
        byte[] bytes = bb.array();
        for (byte b : bytes) {
            arguments.add(b);
        }
    }

    @Override
    public void addArgument(char arg) {
        ByteBuffer bb = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putChar(arg);
        byte[] bytes = bb.array();
        for (byte b : bytes) {
            arguments.add(b);
        }
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

    public void addArgument(String text, StringEndianList endian ) {

    }

    @Override
    public void addArgument(long arg) {
        ByteBuffer bb = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(arg);
        byte[] bytes = bb.array();
        for (byte b : bytes) {
            arguments.add(b);
        }
    }
}
