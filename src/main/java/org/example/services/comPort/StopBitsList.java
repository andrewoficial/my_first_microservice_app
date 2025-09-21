package org.example.services.comPort;
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

    public static  Integer getLikeArrayOrderByValue(StopBitsList value){
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

    public static  Integer getNameLikeArray(int number){
        List<Integer> values = StopBitsList.getValues();
        return values.get(number);
    }
}
