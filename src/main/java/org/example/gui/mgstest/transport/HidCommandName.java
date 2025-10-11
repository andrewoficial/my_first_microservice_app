package org.example.gui.mgstest.transport;

public enum  HidCommandName {
    DO_REBOOT("doRebootDevice"),
    GET_COEFF("getAllCoefficients"),
    GET_DEV_INFO("getDeviceInfo"),
    SENT_URT("sendUartCommand"),
    SET_ALARM_ON("setAlarmOn"),
    SET_ALARM_OFF("setAlarmOff"),
    SET_ALARM_STATE("setAlarmState"),
    SET_SERIAL_NUMBER("setSerialNumber"),
    SET_ECHEM_COEFF("setEChemCoefficients");

    private final String name;

    HidCommandName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
