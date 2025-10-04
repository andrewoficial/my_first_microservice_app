package org.example.gui.mgstest.transport;

public enum  HidCommandName {
    DO_REBOOT("doRebootDevice"),
    GET_COEFF("getAllCoefficients"),
    GET_DEV_INFO("getDeviceInfo"),
    SENT_URT("sendUartCommand");

    private final String name;

    HidCommandName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
