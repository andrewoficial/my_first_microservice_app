package org.example.gui.mgstest.transport.cmd;

import org.example.gui.mgstest.transport.HidCommandName;

public class SendExternalUartCommand extends AbstractSendCommand {
    public SendExternalUartCommand() {
        this.commandNumber = 0x73;
    }

    @Override
    public String getDescription() {
        return "send to external uart";
    }

    @Override
    public HidCommandName getName() {
        return HidCommandName.SENT_EXTERNAL_URT;
    }
}