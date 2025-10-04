// DeviceManager.java
package org.example.gui.mgstest.service;

import lombok.Getter;
import org.hid4java.HidDevice;
import org.hid4java.HidServices;
import org.hid4java.HidManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class DeviceManager {
    private static final int TARGET_PRODUCT_ID = 53456;
    private HidServices hidServices;
    @Getter
    private Map<String, HidDevice> deviceMap = new HashMap<>();
    private Map<String, String> deviceIdToSerialMap = new HashMap<>();
    private Map<String, String> serialToDeviceIdMap = new HashMap<>();

    public DeviceManager() {
        this.hidServices = HidManager.getHidServices();
    }

    public void updateDeviceList() {
        deviceMap.clear();
        deviceIdToSerialMap.clear();
        serialToDeviceIdMap.clear();

        List<HidDevice> devices = hidServices.getAttachedHidDevices();
        hidServices.stop();

        for (HidDevice device : devices) {
            if (device != null && device.getProductId() == 53456) {
                String deviceKey = device.getPath();
                String serialNumber = device.getSerialNumber();

                String displayName = String.format("%s (%s)",
                        device.getProduct(),
                        serialNumber != null && !serialNumber.isEmpty() ? serialNumber : "No SN");

                deviceMap.put(displayName, device);

                String storageKey = generateStorageKey(device);
                deviceIdToSerialMap.put(deviceKey, storageKey);
                if (serialNumber != null) {
                    serialToDeviceIdMap.put(serialNumber, deviceKey);
                }
            }
        }
    }
        public List<HidDevice> findTargetDevices() {
        List<HidDevice> targetDevices = new ArrayList<>();
        List<HidDevice> allDevices = hidServices.getAttachedHidDevices();

        for (HidDevice device : allDevices) {
            if (device != null && device.getProductId() == TARGET_PRODUCT_ID) {
                targetDevices.add(device);
                registerDevice(device);
            }
        }
        hidServices.stop();
        return targetDevices;
    }

    private void registerDevice(HidDevice device) {
        String deviceKey = device.getPath();
        String serialNumber = device.getSerialNumber();
        String displayName = createDisplayName(device);

        deviceMap.put(displayName, device);

        String storageKey = generateStorageKey(device);
        deviceIdToSerialMap.put(deviceKey, storageKey);

        if (serialNumber != null) {
            serialToDeviceIdMap.put(serialNumber, deviceKey);
        }
    }

    public String generateStorageKey(HidDevice device) {
        String deviceKey = device.getPath();
        String serialNumber = device.getSerialNumber();
        return (serialNumber != null && !serialNumber.isEmpty()) ? serialNumber : deviceKey;
    }

    private String createDisplayName(HidDevice device) {
        String serialNumber = device.getSerialNumber();
        return String.format("%s (%s)",
                device.getProduct(),
                serialNumber != null && !serialNumber.isEmpty() ? serialNumber : "No SN");
    }

    public HidDevice getDeviceByDisplayName(String displayName) {
        return deviceMap.get(displayName);
    }

    public String getStorageKeyForDevice(HidDevice device) {
        String deviceKey = device.getPath();
        return deviceIdToSerialMap.get(deviceKey);
    }

    public void cleanup() {
        deviceMap.clear();
        deviceIdToSerialMap.clear();
        serialToDeviceIdMap.clear();
    }
}