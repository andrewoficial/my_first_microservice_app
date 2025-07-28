package org.example.gui.curve;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public enum StateWords {

    SLP(0, "Sleeping"),
    WKP(1, "Is waking up"),
    RNG(2, "Running"),
    SHD(3, "Is shutting down"),
    S04(4, "State 4"),
    S05(5, "State 5"),
    S06(6, "State 6"),
    S07(7, "State 7");

    @Getter
    private final int value;
    @Getter
    private final String name;



    private static final List<Integer> VALUES;

    static {
        VALUES = new ArrayList<>();
        for (StateWords someEnum : StateWords.values()) {
            VALUES.add(someEnum.value);
        }
    }
    StateWords(int value, String name) {
        this.value = value;
        this.name = name;
    }


    public static List<Integer> getValues() {
        return Collections.unmodifiableList(VALUES);
    }

    public static  String getNameLikeArray(int number){
        StateWords[] stateWords  = StateWords.values();
        return stateWords[number].name;
    }

    public static StateWords getByValue(int value) {
        for (StateWords stateWord : values()) {
            if (stateWord.getValue() == value) {
                return stateWord;
            }
        }
        throw new IllegalArgumentException("Unknown state value: " + value);
    }

}