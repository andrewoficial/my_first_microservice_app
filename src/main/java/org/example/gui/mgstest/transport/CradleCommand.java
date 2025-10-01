package org.example.gui.mgstest.transport;

import org.hid4java.HidDevice;

public interface CradleCommand {
    byte[] execute(HidDevice device, CommandParameters arguments) throws Exception;
    String getDescription();
}