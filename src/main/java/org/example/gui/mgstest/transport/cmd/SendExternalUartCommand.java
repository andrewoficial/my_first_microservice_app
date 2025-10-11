package org.example.gui.mgstest.transport.cmd;

class SendExternalUartCommand extends AbstractSendCommand {
    public SendExternalUartCommand() {
        this.commandNumber = 0x73;
    }
}