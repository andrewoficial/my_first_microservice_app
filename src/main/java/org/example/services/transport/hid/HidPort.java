package org.example.services.transport.hid;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.example.utilites.Constants;
import org.hid4java.HidDevice;
import org.hid4java.HidManager;
import org.hid4java.HidServices;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * HID inventory — serial twin of {@link org.example.services.transport.serial.ComPort}.
 * <p>
 * Lists attached devices via hid4java (same source as Multigassens {@code DeviceRepository}).
 * Filtering by known product ids:
 * {@link Constants.HidCommunication#MULTIGASSENSE_TARGET_PRODUCT_ID},
 * {@link Constants.HidCommunication#MIKROSENSE_TARGET_PRODUCT_ID}.
 * <p>
 * I/O stays in {@link HidCommunicator} / {@link HidCommunicatorImpl} — not here.
 */
@Component
@Slf4j
public class HidPort {

    private List<HidDeviceEntry> devices = List.of();

    @Getter
    private HidDeviceEntry activeDevice;

    @Getter
    private int activeIndex = -1;

    /**
     * Refresh cache from OS (like {@code ComPort.updatePorts()}).
     */
    public synchronized void updateDevices() {
        HidServices hidServices = HidManager.getHidServices();
        List<HidDevice> attached = hidServices.getAttachedHidDevices();
        List<HidDeviceEntry> next = new ArrayList<>(attached.size());
        for (HidDevice device : attached) {
            if (device == null) {
                continue;
            }
            HidDeviceEntry entry = new HidDeviceEntry(device);
            next.add(entry);
            if (entry.isKnown()) {
                log.debug("HID known: {} path={}", entry.getDisplayLabel(), entry.getPath());
            } else {
                log.trace("HID other: pid={} path={}", entry.getProductId(), entry.getPath());
            }
        }
        devices = List.copyOf(next);
        if (activeDevice != null) {
            int idx = indexOfPath(activeDevice.getPath());
            if (idx >= 0) {
                activeIndex = idx;
                activeDevice = devices.get(idx);
            } else {
                activeIndex = -1;
                activeDevice = null;
            }
        }
        log.info("HID scan: {} device(s), {} known", devices.size(), countKnown());
    }

    public synchronized List<HidDeviceEntry> getAllDevices() {
        return devices;
    }

    /**
     * @param knownOnly if true — only MultigasSense / MikroSense product ids
     * @param typeFilter optional subtype; {@code null} or {@link Constants.SupportedHidDeviceType#UNKNOWN} = no subtype filter
     */
    public synchronized List<HidDeviceEntry> listDevices(boolean knownOnly,
                                                         Constants.SupportedHidDeviceType typeFilter) {
        return devices.stream()
                .filter(e -> !knownOnly || e.isKnown())
                .filter(e -> typeFilter == null
                        || typeFilter == Constants.SupportedHidDeviceType.UNKNOWN
                        || e.getDeviceType() == typeFilter)
                .collect(Collectors.toList());
    }

    public List<HidDeviceEntry> listKnownDevices() {
        return listDevices(true, null);
    }

    public synchronized void setDevice(int index) {
        if (index < 0 || index >= devices.size()) {
            activeIndex = -1;
            activeDevice = null;
            return;
        }
        activeIndex = index;
        activeDevice = devices.get(index);
    }

    public synchronized void setDevice(HidDeviceEntry entry) {
        if (entry == null) {
            activeIndex = -1;
            activeDevice = null;
            return;
        }
        int idx = indexOfPath(entry.getPath());
        if (idx >= 0) {
            setDevice(idx);
        } else {
            activeIndex = -1;
            activeDevice = entry;
        }
    }

    public synchronized void showAllDevices() {
        int i = 0;
        for (HidDeviceEntry entry : devices) {
            System.out.println(i + ". " + entry.getDisplayLabel() + " (" + entry.getPath() + ")");
            i++;
        }
    }

    private int indexOfPath(String path) {
        if (path == null) {
            return -1;
        }
        for (int i = 0; i < devices.size(); i++) {
            if (Objects.equals(devices.get(i).getPath(), path)) {
                return i;
            }
        }
        return -1;
    }

    private long countKnown() {
        return devices.stream().filter(HidDeviceEntry::isKnown).count();
    }

    /** Empty immutable list when never scanned. */
    public List<HidDeviceEntry> snapshot() {
        return Collections.unmodifiableList(devices);
    }
}
