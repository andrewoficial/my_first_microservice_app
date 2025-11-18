package org.example.device.protEdwardsD397;

import java.util.*;

public enum EdwardsState {
    // Основные состояния устройства (раздел 1.7.6)
    OFF(0, "Off", "Off State"),
    OFF_GOING_ON(1, "OffGoingOn", "Off Going On State"),
    ON_GOING_OFF_SHUTDOWN(2, "OnGoingOffShutdown", "On Going Off Shutdown State"),
    ON_GOING_OFF_NORMAL(3, "OnGoingOffNormal", "On Going Off Normal State"),
    ON(4, "On", "On State"),

    // Состояния насоса (раздел 1.7.8)
    STOPPED(0, "Stopped", "Stopped"),
    STARTING_DELAY(1, "StartingDelay", "Starting Delay"),
    STOPPING_SHORT_DELAY(2, "StoppingShortDelay", "Stopping Short Delay"),
    STOPPING_NORMAL_DELAY(3, "StoppingNormalDelay", "Stopping Normal Delay"),
    RUNNING(4, "Running", "Running"),
    ACCELERATING(5, "Accelerating", "Accelerating"),
    FAULT_BRAKING(6, "FaultBraking", "Fault Braking"),
    BRAKING(7, "Braking", "Braking"),

    // Состояния активных датчиков (раздел 1.7.7)
    GAUGE_NOT_CONNECTED(0, "GaugeNotConnected", "Gauge Not connected"),
    GAUGE_CONNECTED(1, "GaugeConnected", "Gauge Connected"),
    NEW_GAUGE_ID(2, "NewGaugeId", "New Gauge Id"),
    GAUGE_CHANGE(3, "GaugeChange", "Gauge Change"),
    GAUGE_IN_ALERT(4, "GaugeInAlert", "Gauge In Alert"),
    GAUGE_OFF(5, "GaugeOff", "Off"),
    GAUGE_STRIKING(6, "GaugeStriking", "Striking"),
    GAUGE_INITIALISING(7, "GaugeInitialising", "Initialising"),
    GAUGE_CALIBRATING(8, "GaugeCalibrating", "Calibrating"),
    GAUGE_ZEROING(9, "GaugeZeroing", "Zeroing"),
    GAUGE_DEGASSING(10, "GaugeDegassing", "Degassing"),
    GAUGE_ON(11, "GaugeOn", "On"),
    GAUGE_INHIBITED(12, "GaugeInhibited", "Inhibited"),

    UNKNOWN(-1, "Unknown", "Unknown state");

    private final int code;
    private final String name;
    private final String description;

    private static final Map<Integer, List<EdwardsState>> CODE_MAP = new HashMap<>();
    private static List<String> VALUES = new ArrayList<>();

    static {
        for (EdwardsState state : values()) {
            CODE_MAP.computeIfAbsent(state.code, k -> new ArrayList<>()).add(state);
            if (!VALUES.contains(state.name)) {
                VALUES.add(state.name);
            }
        }
        VALUES = Collections.unmodifiableList(VALUES);
    }

    EdwardsState(int code, String name, String description) {
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

    // Получение состояний по коду (может быть несколько состояний с одним кодом)
    public static List<EdwardsState> fromCode(int code) {
        return CODE_MAP.getOrDefault(code, Collections.singletonList(UNKNOWN));
    }

    // Получение состояния по имени
    public static EdwardsState fromName(String name) {
        for (EdwardsState state : values()) {
            if (state.name.equals(name)) {
                return state;
            }
        }
        return UNKNOWN;
    }

    // Фильтрация по типу состояния
    public static List<EdwardsState> getDeviceStates() {
        return Arrays.asList(OFF, OFF_GOING_ON, ON_GOING_OFF_SHUTDOWN, ON_GOING_OFF_NORMAL, ON);
    }

    public static List<EdwardsState> getPumpStates() {
        return Arrays.asList(STOPPED, STARTING_DELAY, STOPPING_SHORT_DELAY, STOPPING_NORMAL_DELAY, 
                           RUNNING, ACCELERATING, FAULT_BRAKING, BRAKING);
    }

    public static List<EdwardsState> getGaugeStates() {
        return Arrays.asList(GAUGE_NOT_CONNECTED, GAUGE_CONNECTED, NEW_GAUGE_ID, GAUGE_CHANGE,
                           GAUGE_IN_ALERT, GAUGE_OFF, GAUGE_STRIKING, GAUGE_INITIALISING,
                           GAUGE_CALIBRATING, GAUGE_ZEROING, GAUGE_DEGASSING, GAUGE_ON, GAUGE_INHIBITED);
    }

    public static List<String> getValues() {
        return VALUES;
    }

    public static EdwardsState getByIndex(int index) {
        if (index >= 0 && index < VALUES.size()) {
            return fromName(VALUES.get(index));
        }
        return UNKNOWN;
    }

    public static int getIndex(EdwardsState state) {
        return VALUES.indexOf(state.name);
    }

    // Проверка типа состояния
    public boolean isDeviceState() {
        return getDeviceStates().contains(this);
    }

    public boolean isPumpState() {
        return getPumpStates().contains(this);
    }

    public boolean isGaugeState() {
        return getGaugeStates().contains(this);
    }

    @Override
    public String toString() {
        return String.format("%s (%d) - %s", name, code, description);
    }
}