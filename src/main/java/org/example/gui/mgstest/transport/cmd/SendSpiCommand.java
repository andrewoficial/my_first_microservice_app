package org.example.gui.mgstest.transport.cmd;

import org.example.gui.mgstest.service.MgsExecutionListener;
import org.example.gui.mgstest.transport.CommandParameters;
import org.example.gui.mgstest.transport.DeviceCommand;
import org.example.gui.mgstest.transport.HidCommandName;
import org.hid4java.HidDevice;

public class SendSpiCommand extends AbstractSendCommand {
    public SendSpiCommand() {
        this.commandNumber = 0x74;
    }

    @Override
    public String getDescription() {
        return "send to spi";
    }

    @Override
    public HidCommandName getName() {
        return HidCommandName.SENT_SPI;
    }
}