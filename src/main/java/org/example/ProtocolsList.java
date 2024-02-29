package org.example;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public enum ProtocolsList {


    IGM10ASCII("IGM10ASCII"),
    IGM10MODBUS("IGM10MODBUS"),
    ARD_BAD_VOLTMETER("ARD_BAD_VOLTMETER"),
    ARD_BAD_FEE_BRD("ARD_BAD_FEE_BRD"),
    ARD_FEE_BRD_METER("ARD_FEE_BRD_METER");


    private final String value;
    private static final List<String> VALUES;

    static {
        VALUES = new ArrayList<>();
        for (ProtocolsList someProtocol : ProtocolsList.values()) {
            VALUES.add(someProtocol.value);
        }
    }
    ProtocolsList(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static List<String> getValues() {
        return Collections.unmodifiableList(VALUES);
    }
    public static String getLikeArray (int number){
        List<String> values = ProtocolsList.getValues();
        return values.get(number);
    }

    public static ProtocolsList getLikeArrayEnum(int number){
        List<String> values = ProtocolsList.getValues();
        return ProtocolsList.valueOf(values.get(number));
    }
}
