// DeviceStateRepository.java
package org.example.gui.mgstest.repository;

import java.util.HashMap;
import java.util.Map;

public class DeviceStateRepository implements DeviceRepository{
    private static DeviceStateRepository instance;
    private Map<String, DeviceState> storage = new HashMap<>();
    
    public static DeviceStateRepository getInstance() {
        if (instance == null) {
            instance = new DeviceStateRepository();
        }
        return instance;
    }

    @Override
    public void put(String deviceId, DeviceState state) {
        storage.put(deviceId, state);
    }

    @Override
    public DeviceState get(String deviceId) {
        return storage.get(deviceId);
    }

    @Override
    public void remove(String deviceId){
        storage.remove(deviceId);
    }

    @Override
    public boolean contains(String deviceId) {
        return storage.containsKey(deviceId);
    }
}