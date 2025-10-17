package org.example.gui.mgstest.service;

import org.example.gui.mgstest.model.HidSupportedDevice;
import org.example.gui.mgstest.transport.HidCommandName;
import org.hid4java.HidDevice;

public interface MgsExecutionListener {
    void onExecutionEvent(HidSupportedDevice deviceId, String answer, boolean isError);
    void onProgressUpdate(HidSupportedDevice deviceId, int progress, String message);
    void onExecutionFinished(HidSupportedDevice deviceId, int progress, byte[] answer, HidCommandName commandName);
}