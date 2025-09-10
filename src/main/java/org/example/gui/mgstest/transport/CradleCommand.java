package org.example.gui.mgstest.transport;

import org.hid4java.HidDevice;

public interface CradleCommand {
    byte[] execute(HidDevice device) throws Exception;
    String getDescription();
}