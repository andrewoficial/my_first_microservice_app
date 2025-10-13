package org.example.gui.mgstest.transport.cmd;

import org.example.gui.mgstest.service.MgsExecutionListener;
import org.example.gui.mgstest.transport.*;
import org.example.services.comPort.StringEndianList;
import org.hid4java.HidDevice;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

public class SetDeviceTime implements CommandModel, DeviceCommand {
    private byte commandNumber = 0x03; // Код команды из перехваченного трафика
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
        return new byte[]{0x00, 0x08, 0x10, 0x18, 0x20};
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

    @Override
    public void addArgument(String text, StringEndianList endian) {
        // Не используется для этой команды
        throw new UnsupportedOperationException("String argument not supported for SetDeviceTime");
    }

    @Override
    public void execute(HidDevice device, CommandParameters parameters, MgsExecutionListener progress) throws Exception {
        // Получаем Unix timestamp из параметров
        long unixTime = parameters.getLongArgument();
        
        // Преобразуем в int (32-bit Unix timestamp)
        int timeValue = (int) (unixTime & 0xFFFFFFFFL);
        
        // Добавляем время как 4 байта little-endian
        this.addArgument(timeValue);
        
        // Добавляем загадочный байт 0x01 (возможно часовой пояс или флаг настроек)
        this.addArgument((byte) 0x01);
        
        PayLoadSender sender = new PayLoadSender();
        sender.writeDataHid(device, PayloadBuilder.build(this), progress, getDescription(), this);
    }

    @Override
    public String getDescription() {
        return "Set Device Time";
    }

    @Override
    public HidCommandName getName() {
        return HidCommandName.SET_DEVICE_TIME;
    }
}