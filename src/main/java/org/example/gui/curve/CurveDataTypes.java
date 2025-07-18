package org.example.gui.curve;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public enum CurveDataTypes {

    MV_V_K(1, "million Volts vs Kelvin"),
    V_V_K(2, "Volts vs Kelvin"),
    O_V_K(3, "Ohms vs Kelvin"),
    LO_V_K(4, "Log Omhs vs Kelvin"),
    sV_V_K(5, "Volts vs Kelvin (spline)"),
    sO_V_K(6, "Ohms vs Kelvin (spline)");

    @Getter
    private final int value;
    @Getter
    private final String name;



    static final public String[] CurveFormats= {
            "million Volts vs Kelvin",
            "Volts vs Kelvin",
            "Ohms vs Kelvin",
            "Log Omhs vs Kelvin",
            "Volts vs Kelvin (spline)",
            "Ohms vs Kelvin (spline)"
    };
    private static final List<Integer> VALUES;

    static {
        VALUES = new ArrayList<>();
        for (CurveDataTypes someEnum : CurveDataTypes.values()) {
            VALUES.add(someEnum.value);
        }
    }
    CurveDataTypes(int value, String name) {
        this.value = value;
        this.name = name;
    }


    public String[] getTypes(){
        return CurveFormats;
    }

    public static List<Integer> getValues() {
        return Collections.unmodifiableList(VALUES);
    }

    public static  String getNameLikeArray(int number){
        CurveDataTypes[] curveFormats  = CurveDataTypes.values();
        return curveFormats[number].name;
    }

    public static CurveDataTypes getByValue(int value) {
        for (CurveDataTypes type : values()) {
            if (type.getValue() == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown format value: " + value);
    }

}
