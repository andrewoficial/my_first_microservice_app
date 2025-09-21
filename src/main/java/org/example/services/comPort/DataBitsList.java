package org.example.services.comPort;


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

    public static  Integer getLikeArrayOrderByValue(DataBitsList value){
        if(value == null){
            return -1;
        }
        List<Integer> values = getValues();
        int num = 0;
        for (Integer i : values) {
            if(value.getValue() == i)
                return num;

            num++;
        }
        return -1;
    }

    public int getValue() {
        return value;
    }

    public static List<Integer> getValues() {
        return Collections.unmodifiableList(VALUES);
    }

    public static Integer getNameLikeArray(int number){
        List<Integer> values = DataBitsList.getValues();
        return values.get(number);
    }


}
