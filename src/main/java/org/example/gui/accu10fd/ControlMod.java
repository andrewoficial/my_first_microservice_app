package org.example.gui.accu10fd;

import lombok.Getter;
import org.example.gui.curve.CurveDataTypes;
import org.example.gui.curve.StateWords;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public enum ControlMod {
    DIGITAL(1, "digital control mode"),
    ANALOG(2, "analog control mode");

    @Getter
    private final int value;
    @Getter
    private final String name;



    private static final List<Integer> VALUES;

    static {
        VALUES = new ArrayList<>();
        for (ControlMod someEnum : ControlMod.values()) {
            VALUES.add(someEnum.value);
        }
    }
    ControlMod(int value, String name) {
        this.value = value;
        this.name = name;
    }


    public static List<Integer> getValues() {
        return Collections.unmodifiableList(VALUES);
    }

    public static  String getNameLikeArray(int number){
        ControlMod[] stateWords  = ControlMod.values();
        return stateWords[number].name;
    }

    public static ControlMod getByValue(int value) {
        for (ControlMod some : values()) {
            if (some.getValue() == value) {
                return some;
            }
        }
        throw new IllegalArgumentException("Unknown mode value: " + value);
    }
}
