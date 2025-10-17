package org.example.gui.mgstest.model;

import lombok.Getter;
import lombok.Setter;
import org.example.utilites.Constants;
import org.hid4java.HidDevice;

import java.util.Objects;

@Getter @Setter
public class HidSupportedDevice {
    private HidDevice hidDevice;
    private String displayName;
    private Constants.SupportedHidDeviceType deviceType;
    private boolean alive;

    public HidSupportedDevice(HidDevice hidDevice, String displayName, Constants.SupportedHidDeviceType deviceType) {
        this.hidDevice = hidDevice;
        this.displayName = displayName;
        this.deviceType = deviceType;
    }

    @Override
    public boolean equals(Object o) {
        if(o == null) return false;
        if(this.getClass() != o.getClass()) return false;
        if(this == o) return true;

        HidSupportedDevice that = (HidSupportedDevice) o;
        return Objects.equals(hidDevice.getPath(), that.hidDevice.getPath()) && deviceType == that.deviceType;//DisplayName may be different
    }

    @Override
    public int hashCode() {
        return Objects.hash(hidDevice.getPath(), deviceType);
    }

    @Override
    public String toString() {
        if(deviceType == Constants.SupportedHidDeviceType.MIKROSENSE){
            return "MKRS " + displayName;
        }else if(deviceType == Constants.SupportedHidDeviceType.MULTIGASSENSE){
            return "MGS " + displayName;
        }
        return "Unknown " + displayName;
    }

}
