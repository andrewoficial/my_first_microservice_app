package org.example.gui.mgstest.transport;

public enum  HidCommandName {
    DO_REBOOT("doRebootDevice"),
    GET_COEFF("getAllCoefficients"),
    GET_DEV_INFO("getDeviceInfo"),
    SENT_URT("sendUartCommand"),
    SET_ALARM_ON("setAlarmOn"),
    SET_ALARM_OFF("setAlarmOff");

    private final String name;

    HidCommandName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
