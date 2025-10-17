package org.example.gui.mgstest.repository;

import org.example.gui.mgstest.model.DeviceState;
import org.example.gui.mgstest.model.HidSupportedDevice;
import org.hid4java.HidDevice;

public interface DeviceRepositoryInterface {
    void put(HidSupportedDevice deviceId, DeviceState state);
    DeviceState get(HidSupportedDevice deviceId);
    void remove(HidSupportedDevice deviceId);

    boolean contains(HidSupportedDevice deviceId);
}