package org.example.gui.mgstest.transport.cmd;

import org.example.gui.mgstest.transport.HidCommandName;

public class SendUartCommand extends AbstractSendCommand {
    public SendUartCommand() {
        this.commandNumber = 0x09;
    }

    @Override
    public String getDescription() {
        return "send to uart";
    }

    @Override
    public HidCommandName getName() {
        return HidCommandName.SENT_URT;
    }
}