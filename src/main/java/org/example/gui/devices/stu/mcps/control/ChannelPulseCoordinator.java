package org.example.gui.devices.stu.mcps.control;

public interface ChannelPulseCoordinator {
    boolean isAnyPulseActive();
    void onPulseStateChanged(boolean active);
    void setAllControlsEnabled(boolean enabled);
}
