package org.example.gui.mgstest.transport.cmd.mgs;

import org.example.gui.mgstest.model.HidSupportedDevice;
import org.example.gui.mgstest.service.MgsExecutionListener;
import org.example.gui.mgstest.transport.*;
import org.example.gui.mgstest.transport.cmd.AbstractCommand;

public class SetDeviceTime extends AbstractCommand implements DeviceCommand {
    private byte commandNumber = 0x03; // Код команды из перехваченного трафика

    @Override
    public byte getCommandNumber() {
        return commandNumber;
    }

    @Override
    public byte[] getAnswerOffsets() {
        return new byte[]{0x00, 0x08, 0x10, 0x18, 0x20};
    }

    @Override
    public void execute(HidSupportedDevice device, CommandParameters parameters, MgsExecutionListener progress) throws Exception {
        // Получаем Unix timestamp из параметров
        long unixTime = parameters.getLongArgument();
        
        // Преобразуем в int (32-bit Unix timestamp)
        int timeValue = (int) (unixTime & 0xFFFFFFFFL);
        
        // Добавляем время как 4 байта little-endian
        this.addArgument(timeValue);
        
        // Добавляем загадочный байт 0x01 (возможно часовой пояс или флаг настроек)
        this.addArgument((byte) 0x01);
        
        PayLoadSender sender = new PayLoadSender();
        sender.writeDataHid(device, PayloadBuilder.buildMgs(this), progress, getDescription(), this);
    }

    @Override
    public String getDescription() {
        return HidCommandName.SET_DEVICE_TIME.getName();
    }

    @Override
    public HidCommandName getName() {
        return HidCommandName.SET_DEVICE_TIME;
    }
}