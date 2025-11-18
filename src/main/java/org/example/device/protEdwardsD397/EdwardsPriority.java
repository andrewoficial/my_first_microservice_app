package org.example.device.protEdwardsD397;

import java.util.*;

public enum EdwardsPriority {
    OK(0, "OK", "No issues"),
    WARNING(1, "Warning", "Warning condition"),
    ALARM_LOW(2, "Alarm Low", "Low priority alarm"),
    ALARM_HIGH(3, "Alarm High", "High priority alarm"),
    
    UNKNOWN_PRIORITY(-1, "Unknown", "Unknown priority");

    private final int code;
    private final String name;
    private final String description;

    private static final Map<Integer, EdwardsPriority> CODE_MAP = new HashMap<>();

    static {
        for (EdwardsPriority priority : values()) {
            CODE_MAP.put(priority.code, priority);
        }
    }

    EdwardsPriority(int code, String name, String description) {
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

    public static EdwardsPriority fromCode(int code) {
        return CODE_MAP.getOrDefault(code, UNKNOWN_PRIORITY);
    }

    public boolean isAlarm() {
        return this == ALARM_LOW || this == ALARM_HIGH;
    }

    public boolean isWarningOrHigher() {
        return this.code >= WARNING.code;
    }

    @Override
    public String toString() {
        return String.format("%s (%d) - %s", name, code, description);
    }
}