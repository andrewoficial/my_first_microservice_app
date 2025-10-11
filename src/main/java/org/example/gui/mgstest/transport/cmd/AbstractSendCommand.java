package org.example.gui.mgstest.transport.cmd;

public abstract class AbstractSendCommand implements CommandModel {
    protected byte commandNumber; // Set in subclasses
    private byte[] commandBytes;

    public void addCommandText(String text) {
        text = text.trim();
        if (!text.endsWith("\r")) {
            text += "\r";
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

    // Stub unused methods
    @Override
    public void addArgument(byte arg) {
        // Not used for Send commands
    }

    @Override
    public void addArgument(int arg) {
        // Not used for Send commands
    }

    @Override
    public void addArgument(float arg) {
        // Not used for Send commands
    }

    @Override
    public void addArguments(float[] args) {
        // Not used for Send commands
    }
}