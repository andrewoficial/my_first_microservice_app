package org.example.gui.mgstest.transport;

public enum  HidCommandName {
    MKRS_GET_INFO("doGetInfoMKRS"),
    MKRS_SEND_UART("sendUartMKRS"),
    DO_REBOOT("doRebootDevice"),
    DO_BATTERY_COUNTER_RESET("doBatteryCounterReset"),//batteryCounterReset
    DO_TEST_BLINK("doBlinkTest"),
    DO_TEST_BEEP("doBlinkTest"),
    GET_COEFF("getAllCoefficients"),
    GET_SETTINGS("getAllSettings"),
    GET_DEV_INFO("getDeviceInfo"),
    GET_ALARMS("getAlarms"),
    GET_V_RANGE("getVRange"),
    GET_GAS_RANGE("getGasRange"),
    GET_SENS_STATUS("getSensStatus"),
    SENT_URT("sendUartCommand"),
    SENT_EXTERNAL_URT("sendExternalUartCommand"),
    SENT_SPI("sendSpiCommand"),
    SET_ALARM_STATE("setAlarmState"),
    SET_SOUND_STATE("setSoundState"),
    SET_VIBRATION_STATE("setVibrationState"),
    SET_SERIAL_NUMBER("setSerialNumber"),
    SET_LOG_TIME_OUT("setSerialNumber"),
    SET_DEVICE_TIME("setDeviceTime"),
    SET_V_RANGE("setVRange"),
    SET_SENS_STATUS("setSensStatus"),
    SET_SENS_ACCEL("setSensAccel"),
    SET_GAS_RANGE("setGasRange"),
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
