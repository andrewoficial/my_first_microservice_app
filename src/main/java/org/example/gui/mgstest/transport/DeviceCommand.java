package org.example.gui.mgstest.transport;

import org.example.gui.mgstest.model.HidSupportedDevice;
import org.example.gui.mgstest.service.MgsExecutionListener;

public interface DeviceCommand {
    void execute(HidSupportedDevice device, CommandParameters parameters, MgsExecutionListener progress) throws Exception;
    String getDescription();
    HidCommandName getName();
}