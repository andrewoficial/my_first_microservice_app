package org.example.gui.mgstest.transport.cmd.mgs.transfer;

import org.example.gui.mgstest.transport.HidCommandName;

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