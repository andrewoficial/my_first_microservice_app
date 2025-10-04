package org.example.gui.mgstest.service;

import org.example.gui.mgstest.transport.HidCommandName;
import org.hid4java.HidDevice;

public interface MgsExecutionListener {
    void onExecutionEvent(HidDevice deviceId, String answer, boolean isError);
    void onProgressUpdate(HidDevice deviceId, int progress, String message);
    void onExecutionFinished(HidDevice deviceId, int progress, byte[] answer, HidCommandName commandName);
}