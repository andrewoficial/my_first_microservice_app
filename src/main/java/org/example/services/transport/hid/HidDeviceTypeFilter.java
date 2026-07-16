package org.example.services.transport.hid;

import org.example.utilites.Constants;

/**
 * Combo filter for HID device list (jcbHidMask — «Желаемое устройство»).
 */
public enum HidDeviceTypeFilter {
    ALL("Все известные типы", null),
    MULTIGASSENSE("MultigasSense", Constants.SupportedHidDeviceType.MULTIGASSENSE),
    MIKROSENSE("MikroSense", Constants.SupportedHidDeviceType.MIKROSENSE);

    private final String label;
    private final Constants.SupportedHidDeviceType deviceType;

    HidDeviceTypeFilter(String label, Constants.SupportedHidDeviceType deviceType) {
        this.label = label;
        this.deviceType = deviceType;
    }

    /** null = any known type when knownOnly filter is on. */
    public Constants.SupportedHidDeviceType getDeviceType() {
        return deviceType;
    }

    @Override
    public String toString() {
        return label;
    }
}
