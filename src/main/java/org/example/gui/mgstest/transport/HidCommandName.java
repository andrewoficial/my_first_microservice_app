package org.example.gui.mgstest.transport;

public enum  HidCommandName {
    DO_REBOOT("doRebootDevice"),
    DO_BATTERY_COUNTER_RESET("doBatteryCounterReset"),//batteryCounterReset
    DO_TEST_BLINK("doBlinkTest"),
    DO_TEST_BEEP("doBlinkTest"),
    GET_COEFF("getAllCoefficients"),
    GET_DEV_INFO("getDeviceInfo"),
    SENT_URT("sendUartCommand"),
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
