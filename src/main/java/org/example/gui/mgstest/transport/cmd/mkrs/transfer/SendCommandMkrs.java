package org.example.gui.mgstest.transport.cmd.mkrs.transfer;

import org.example.gui.mgstest.model.HidSupportedDevice;
import org.example.gui.mgstest.service.MgsExecutionListener;
import org.example.gui.mgstest.transport.*;
import org.example.gui.mgstest.transport.cmd.AbstractCommand;
import org.example.services.comPort.StringEndianList;
import org.example.utilites.MyUtilities;

public class SendCommandMkrs extends AbstractCommand implements DeviceCommand {
    protected byte commandNumber;
    private byte[] commandBytes;

    @Override
    public void addArgument(String text, StringEndianList endian) {
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

        // Получаем канал из устройства (1 для CH4, 2 для CO2)
        byte channel = (byte) parameters.getIntArgument();

        PayLoadSender sender = new PayLoadSender();
        byte[] payload = PayloadBuilder.buildMkrs(channel, this.getArguments());

        System.out.println("Generated payload: " + MyUtilities.bytesToHex(payload));

        byte[] answer = sender.writeSimpleDataHid(device, payload, progress, getDescription(), this);
        progress.onExecutionFinished(device, 100, answer, this.getName());
    }

    @Override
    public String getDescription() {
        return HidCommandName.MKRS_SEND_UART.getName();
    }

    @Override
    public HidCommandName getName() {
        return HidCommandName.MKRS_SEND_UART;
    }

    @Override
    public byte[] getAnswerOffsets() {
        return new byte[]{0x00};
    }
}