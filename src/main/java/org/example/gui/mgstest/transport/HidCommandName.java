package org.example.gui.mgstest.transport;

public enum  HidCommandName {
    DO_REBOOT("doRebootDevice"),
    DO_BATTERY_COUNTER_RESET("doBatteryCounterReset"),//batteryCounterReset
    DO_TEST_BLINK("doBlinkTest"),
    DO_TEST_BEEP("doBlinkTest"),
    GET_COEFF("getAllCoefficients"),
    GET_SETTINGS("getAllSettings"),
    GET_DEV_INFO("getDeviceInfo"),
    GET_ALARMS("getAlarms"),
    GET_V_RANGE("getVRange"),
    SENT_URT("sendUartCommand"),
    SENT_EXTERNAL_URT("sendExternalUartCommand"),
    SENT_SPI("sendSpiCommand"),
    SET_ALARM_STATE("setAlarmState"),
    SET_SOUND_STATE("setSoundState"),
    SET_VIBRATION_STATE("setVibrationState"),
    SET_SERIAL_NUMBER("setSerialNumber"),
    SET_DEVICE_TIME("setDeviceTime"),
    SET_V_RANGE("setVRange"),
    SET_ALARMS("setAlarms"),
    SET_ECHEM_COEFF("setEChemCoefficients");

    private final String name;

    HidCommandName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
