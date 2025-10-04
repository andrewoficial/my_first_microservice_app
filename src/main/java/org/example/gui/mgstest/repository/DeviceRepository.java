package org.example.gui.mgstest.repository;

import org.hid4java.HidDevice;

public interface DeviceRepository {
    void put(HidDevice deviceId, DeviceState state);
    DeviceState get(HidDevice deviceId);
    void remove(HidDevice deviceId);
    boolean contains(HidDevice deviceId);
}