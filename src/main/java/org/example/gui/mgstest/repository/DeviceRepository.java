package org.example.gui.mgstest.repository;

public interface DeviceRepository {
    void put(String deviceId, DeviceState state);
    DeviceState get(String deviceId);
    void remove(String deviceId);
    boolean contains(String deviceId);
}