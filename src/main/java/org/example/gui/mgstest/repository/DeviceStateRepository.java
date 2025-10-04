// DeviceStateRepository.java
package org.example.gui.mgstest.repository;

import org.hid4java.HidDevice;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class DeviceStateRepository implements DeviceRepository {
    private final Map<HidDevice, DeviceState> storage = new ConcurrentHashMap<>();

    @Override
    public void put(HidDevice deviceId, DeviceState state) {
        storage.put(deviceId, state);
    }

    @Override
    public DeviceState get(HidDevice deviceId) {
        return storage.get(deviceId);
    }

    @Override
    public void remove(HidDevice deviceId) {
        storage.remove(deviceId);
    }

    @Override
    public boolean contains(HidDevice deviceId) {
        return storage.containsKey(deviceId);
    }

    // Дополнительные методы, если нужны
    public int size() {
        return storage.size();
    }

    public void clear() {
        storage.clear();
    }
}