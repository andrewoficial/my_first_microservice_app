// DeviceStateRepository.java
package org.example.gui.mgstest.repository;

import org.apache.log4j.Logger;
import org.example.gui.mgstest.MultigassensWindow;
import org.example.gui.mgstest.model.DeviceState;
import org.example.gui.mgstest.model.HidSupportedDevice;
import org.hid4java.HidDevice;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class DeviceStateRepository implements DeviceRepositoryInterface {
    private final Map<HidSupportedDevice, DeviceState> storage = new ConcurrentHashMap<>();
    private final Logger log = Logger.getLogger(DeviceStateRepository.class);
    @Override
    public void put(HidSupportedDevice deviceId, DeviceState state) {
        storage.put(deviceId, state);
    }

    @Override
    public DeviceState get(HidSupportedDevice deviceId) {
        if(!storage.containsKey(deviceId)) {
            storage.put(deviceId, new DeviceState());
        }
        return storage.get(deviceId);
    }

    @Override
    public void remove(HidSupportedDevice deviceId) {
        storage.remove(deviceId);
    }

    @Override
    public boolean contains(HidSupportedDevice deviceId) {
        if(deviceId == null){
            log.error("В поиск передано null");
            return false;
        }
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