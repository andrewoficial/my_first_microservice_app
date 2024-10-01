/*
Перечень протоколов обмена данными с приборами
 */
package org.example.utilites;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public enum ProtocolsList {


    IGM10ASCII("IGM10ASCII"),
    IGM10LORA_P2P("IGM10LORA_P2P"),
    IGM10MODBUS("IGM10MODBUS"),
    ARD_BAD_VOLTMETER("ARD_BAD_VOLTMETER"),
    ARD_FEE_BRD_METER("ARD_FEE_BRD_METER"),
    ERSTEVAK_MTP4D("ERSTEVAK_MTP4D"),
    DEMO_PROTOCOL("DEMO_PROTOCOL"),
    GPS_Test("GPS_Test"),
    ECT_TC290("ECT_TC290"),//East Changing Technologies
    OWON_SPE3051("OWON_SPE3051"),
    EDWARDS_D397_00_000("EDWARDS_D397_00_000");


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

    public static int getNumber (ProtocolsList pr){
        List<String> values = ProtocolsList.getValues();
        for (int i = 0; i < values.size(); i++) {
            //System.out.println("Compare " + values.get(i) + " and " + pr.value);
            if(values.get(i).equalsIgnoreCase(pr.value)){
                return i;
            }
        }
        return -1;

    }


}
