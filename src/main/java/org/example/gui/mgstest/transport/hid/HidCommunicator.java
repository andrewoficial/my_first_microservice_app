package org.example.gui.mgstest.transport.hid;

import org.hid4java.HidDevice;

public interface HidCommunicator {
    void simpleSend(HidDevice device, byte[] data);
    void simpleSendInitial(HidDevice device, byte[] data);
    byte[] readResponse(HidDevice device);
    void printArrayLikeDeviceMonitor(byte[] data);
}