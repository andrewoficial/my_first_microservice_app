package org.example.services.transport.hid;

import lombok.Getter;
import org.example.utilites.Constants;
import org.hid4java.HidDevice;

import java.util.Objects;

/**
 * Snapshot of an attached HID device for lists/combos (main window, scanners).
 * Classification by product id mirrors Multigassens {@code DeviceRepository}.
 */
@Getter
public class HidDeviceEntry {

    private final HidDevice device;
    private final Constants.SupportedHidDeviceType deviceType;
    private final int productId;
    private final int vendorId;
    private final String path;
    private final String serialNumber;

    public HidDeviceEntry(HidDevice device) {
        this.device = Objects.requireNonNull(device, "device");
        this.productId = device.getProductId();
        this.vendorId = device.getVendorId();
        this.path = device.getPath();
        String sn = device.getSerialNumber();
        this.serialNumber = (sn == null || sn.isBlank()) ? null : sn;
        this.deviceType = classifyProductId(this.productId);
    }

    public static Constants.SupportedHidDeviceType classifyProductId(int productId) {
        if (productId == Constants.HidCommunication.MULTIGASSENSE_TARGET_PRODUCT_ID) {
            return Constants.SupportedHidDeviceType.MULTIGASSENSE;
        }
        if (productId == Constants.HidCommunication.MIKROSENSE_TARGET_PRODUCT_ID) {
            return Constants.SupportedHidDeviceType.MIKROSENSE;
        }
        return Constants.SupportedHidDeviceType.UNKNOWN;
    }

    public boolean isKnown() {
        return deviceType != Constants.SupportedHidDeviceType.UNKNOWN;
    }

    /** Human-readable label for JComboBox (like Multigassens list). */
    public String getDisplayLabel() {
        String id = serialNumber != null ? serialNumber : shortPath();
        return switch (deviceType) {
            case MULTIGASSENSE -> "MGS " + id;
            case MIKROSENSE -> "MKRS " + id;
            case UNKNOWN -> String.format("HID vid=0x%04X pid=0x%04X %s", vendorId, productId, id);
        };
    }

    private String shortPath() {
        if (path == null) {
            return "?";
        }
        int slash = Math.max(path.lastIndexOf('\\'), path.lastIndexOf('/'));
        return slash >= 0 && slash < path.length() - 1 ? path.substring(slash + 1) : path;
    }

    @Override
    public String toString() {
        return getDisplayLabel();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof HidDeviceEntry that)) {
            return false;
        }
        return Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path);
    }
}
