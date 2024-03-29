package org.example.utilites;
import java.util.*;

public enum StopBitsList {
    S1(1),
    S2(2);

    private final int value;
    private static final List<Integer> VALUES;

    static {
        VALUES = new ArrayList<>();
        for (StopBitsList someEnum : StopBitsList.values()) {
            VALUES.add(someEnum.value);
        }
    }
    StopBitsList(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static List<Integer> getValues() {
        return Collections.unmodifiableList(VALUES);
    }

    public static  Integer getLikeArray (int number){
        List<Integer> values = StopBitsList.getValues();
        return values.get(number);
    }
}
