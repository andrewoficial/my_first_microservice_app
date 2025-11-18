package org.example.device.protEdwardsD397;

import java.util.*;

public enum EdwardsAlert {
    NO_ALERT(0, "No Alert", "No Alert"),
    ADC_FAULT(1, "ADC Fault", "ADC Fault"),
    ADC_NOT_READY(2, "ADC Not Ready", "ADC Not Ready"),
    OVER_RANGE(3, "Over Range", "Over Range"),
    UNDER_RANGE(4, "Under Range", "Under Range"),
    ADC_INVALID(5, "ADC Invalid", "ADC Invalid"),
    NO_GAUGE(6, "No Gauge", "No Gauge"),
    UNKNOWN_7(7, "Unknown", "Unknown"),
    NOT_SUPPORTED(8, "Not Supported", "Not Supported"),
    NEW_ID(9, "New ID", "New ID"),
    OVER_RANGE_10(10, "Over Range", "Over Range"),
    UNDER_RANGE_11(11, "Under Range", "Under Range"),
    OVER_RANGE_12(12, "Over Range", "Over Range"),
    ION_EM_TIMEOUT(13, "Ion Em Timeout", "Ion Em Timeout"),
    NOT_STRUCK_14(14, "Not Struck", "Not Struck"),
    FILAMENT_FAIL_15(15, "Filament Fail", "Filament Fail"),
    MAG_FAIL(16, "Mag Fail", "Mag Fail"),
    STRIKER_FAIL(17, "Striker Fail", "Striker Fail"),
    NOT_STRUCK_18(18, "Not Struck", "Not Struck"),
    FILAMENT_FAIL_19(19, "Filament Fail", "Filament Fail"),
    CAL_ERROR(20, "Cal Error", "Cal Error"),
    INITIALISING(21, "Initialising", "Initialising"),
    EMISSION_ERROR(22, "Emission Error", "Emission Error"),
    OVER_PRESSURE(23, "Over Pressure", "Over Pressure"),
    ASG_CANT_ZERO(24, "ASG Cant Zero", "ASG Cant Zero"),
    RAMPUP_TIMEOUT(25, "RampUp Timeout", "RampUp Timeout"),
    DROOP_TIMEOUT(26, "Droop Timeout", "Droop Timeout"),
    RUN_HOURS_HIGH(27, "Run Hours High", "Run Hours High"),
    SC_INTERLOCK(28, "SC Interlock", "SC Interlock"),
    ID_VOLTS_ERROR(29, "ID Volts Error", "ID Volts Error"),
    SERIAL_ID_FAIL(30, "Serial ID Fail", "Serial ID Fail"),
    UPLOAD_ACTIVE(31, "Upload Active", "Upload Active"),
    DX_FAULT(32, "DX Fault", "DX Fault"),
    TEMP_ALERT(33, "Temp Alert", "Temp Alert"),
    SYSI_INHIBIT(34, "SYSI Inhibit", "SYSI Inhibit"),
    EXT_INHIBIT(35, "Ext Inhibit", "Ext Inhibit"),
    TEMP_INHIBIT(36, "Temp Inhibit", "Temp Inhibit"),
    NO_READING(37, "No Reading", "No Reading"),
    NO_MESSAGE(38, "No Message", "No Message"),
    NOV_FAILURE(39, "NOV Failure", "NOV Failure"),
    UPLOAD_TIMEOUT(40, "Upload Timeout", "Upload Timeout"),
    DOWNLOAD_FAILED(41, "Download Failed", "Download Failed"),
    NO_TUBE(42, "No Tube", "No Tube"),
    USE_GAUGES_4_6(43, "Use Gauges 4-6", "Use Gauges 4-6"),
    DEGAS_INHIBITED(44, "Degas Inhibited", "Degas Inhibited"),
    IGC_INHIBITED(45, "IGC Inhibited", "IGC Inhibited"),
    BROWNOUT_SHORT(46, "Brownout/Short", "Brownout/Short"),
    SERVICE_DUE(47, "Service due", "Service due"),

    UNKNOWN_ALERT(-1, "Unknown Alert", "Unknown Alert");

    private final int code;
    private final String name;
    private final String description;

    private static final Map<Integer, EdwardsAlert> CODE_MAP = new HashMap<>();
    private static List<String> VALUES = new ArrayList<>();

    static {
        for (EdwardsAlert alert : values()) {
            CODE_MAP.put(alert.code, alert);
            VALUES.add(alert.name);
        }
        VALUES = Collections.unmodifiableList(VALUES);
    }

    EdwardsAlert(int code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public static EdwardsAlert fromCode(int code) {
        return CODE_MAP.getOrDefault(code, UNKNOWN_ALERT);
    }

    public static EdwardsAlert fromName(String name) {
        for (EdwardsAlert alert : values()) {
            if (alert.name.equals(name)) {
                return alert;
            }
        }
        return UNKNOWN_ALERT;
    }

    public static List<String> getValues() {
        return VALUES;
    }

    public static EdwardsAlert getByIndex(int index) {
        if (index >= 0 && index < VALUES.size()) {
            return fromName(VALUES.get(index));
        }
        return UNKNOWN_ALERT;
    }

    public static int getIndex(EdwardsAlert alert) {
        return VALUES.indexOf(alert.name);
    }

    // Группировка алертов по категориям
    public static List<EdwardsAlert> getADCAlerts() {
        return Arrays.asList(ADC_FAULT, ADC_NOT_READY, OVER_RANGE, UNDER_RANGE, ADC_INVALID);
    }

    public static List<EdwardsAlert> getGaugeAlerts() {
        return Arrays.asList(NO_GAUGE, UNKNOWN_7, NOT_SUPPORTED, NEW_ID, OVER_RANGE_10, 
                           UNDER_RANGE_11, OVER_RANGE_12, ION_EM_TIMEOUT, NOT_STRUCK_14,
                           FILAMENT_FAIL_15, MAG_FAIL, STRIKER_FAIL, NOT_STRUCK_18,
                           FILAMENT_FAIL_19, CAL_ERROR, INITIALISING, EMISSION_ERROR,
                           OVER_PRESSURE, ASG_CANT_ZERO);
    }

    public static List<EdwardsAlert> getPumpAlerts() {
        return Arrays.asList(RAMPUP_TIMEOUT, DROOP_TIMEOUT, RUN_HOURS_HIGH, SC_INTERLOCK,
                           ID_VOLTS_ERROR, SERIAL_ID_FAIL, UPLOAD_ACTIVE, DX_FAULT);
    }

    public static List<EdwardsAlert> getSystemAlerts() {
        return Arrays.asList(TEMP_ALERT, SYSI_INHIBIT, EXT_INHIBIT, TEMP_INHIBIT,
                           NO_READING, NO_MESSAGE, NOV_FAILURE, UPLOAD_TIMEOUT,
                           DOWNLOAD_FAILED, NO_TUBE, USE_GAUGES_4_6, DEGAS_INHIBITED,
                           IGC_INHIBITED, BROWNOUT_SHORT, SERVICE_DUE);
    }

    // Проверка категории алерта
    public boolean isADCAlert() {
        return getADCAlerts().contains(this);
    }

    public boolean isGaugeAlert() {
        return getGaugeAlerts().contains(this);
    }

    public boolean isPumpAlert() {
        return getPumpAlerts().contains(this);
    }

    public boolean isSystemAlert() {
        return getSystemAlerts().contains(this);
    }

    @Override
    public String toString() {
        return String.format("%s (%d) - %s", name, code, description);
    }
}