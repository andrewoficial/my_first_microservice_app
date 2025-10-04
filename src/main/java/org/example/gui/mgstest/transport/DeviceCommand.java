package org.example.gui.mgstest.transport;

import org.example.gui.mgstest.service.MgsExecutionListener;
import org.hid4java.HidDevice;

public interface DeviceCommand {
    void execute(HidDevice device, CommandParameters parameters, MgsExecutionListener progress) throws Exception;
    String getDescription();
    HidCommandName getName();
}