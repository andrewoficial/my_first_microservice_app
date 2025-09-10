// DeviceStateStorage.java
package org.example.gui.mgstest.pool;

import java.util.HashMap;
import java.util.Map;

public class DeviceStateStorage {
    private static DeviceStateStorage instance;
    private Map<String, DeviceState> storage = new HashMap<>();
    
    public static DeviceStateStorage getInstance() {
        if (instance == null) {
            instance = new DeviceStateStorage();
        }
        return instance;
    }
    
    public void put(String deviceId, DeviceState state) {
        storage.put(deviceId, state);
    }
    
    public DeviceState get(String deviceId) {
        return storage.get(deviceId);
    }

    public void remove(String deviceId){
        storage.remove(deviceId);
    }
    
    public boolean contains(String deviceId) {
        return storage.containsKey(deviceId);
    }
}