package org.example.gui.sbpStuMcps;

public interface ChannelPulseCoordinator {
    boolean isAnyPulseActive();
    void onPulseStateChanged(boolean active);
    void setAllControlsEnabled(boolean enabled);
}
