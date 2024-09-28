package org.example.utilites;


import java.util.*;
public enum DataBitsList {
    B5(5),
    B6(6),
    B7(7),
    B8(8);

    private final int value;
    private static final List<Integer> VALUES;

    static {
        VALUES = new ArrayList<>();
        for (DataBitsList someEnum : DataBitsList.values()) {
            VALUES.add(someEnum.value);
        }
    }
    DataBitsList(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static List<Integer> getValues() {
        return Collections.unmodifiableList(VALUES);
    }

    public static  Integer getLikeArray (int number){
        List<Integer> values = DataBitsList.getValues();
        return values.get(number);
    }


}
