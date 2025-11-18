package org.example.device.protEdwardsD397;

import java.util.*;

public enum EdwardsUnits {
    PRESSURE_PASCAL(59, "Pa", "Pressure in Pascals"),
    VOLTAGE(66, "V", "Voltage"),
    PERCENT(81, "%", "Percentage"),
    UNKNOWN(-1, "Unknown", "Unknown unit");

    private final int code;
    private final String symbol;
    private final String description;

    private static final Map<Integer, EdwardsUnits> CODE_MAP = new HashMap<>();
    private static List<String> VALUES = new ArrayList<>();

    static {
        for (EdwardsUnits unit : values()) {
            CODE_MAP.put(unit.code, unit);
            VALUES.add(unit.name());
        }
        VALUES = Collections.unmodifiableList(VALUES);
    }

    EdwardsUnits(int code, String symbol, String description) {
        this.code = code;
        this.symbol = symbol;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getDescription() {
        return description;
    }

    public static EdwardsUnits fromCode(int code) {
        return CODE_MAP.getOrDefault(code, UNKNOWN);
    }

    public static EdwardsUnits fromSymbol(String symbol) {
        for (EdwardsUnits unit : values()) {
            if (unit.symbol.equals(symbol)) {
                return unit;
            }
        }
        return UNKNOWN;
    }

    public static List<String> getValues() {
        return VALUES;
    }

    public static EdwardsUnits getByIndex(int index) {
        if (index >= 0 && index < values().length) {
            return values()[index];
        }
        return UNKNOWN;
    }

    public static int getIndex(EdwardsUnits unit) {
        EdwardsUnits[] units = values();
        for (int i = 0; i < units.length; i++) {
            if (units[i] == unit) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public String toString() {
        return String.format("%s (%s) - %s", symbol, code, description);
    }
}