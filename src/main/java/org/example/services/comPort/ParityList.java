package org.example.services.comPort;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public enum ParityList {

    P_NO(0, "NO"),
    P_OD(1, "ODD"),
    P_EV(2, "EVEN"),
    P_MR(3, "MARK"),
    P_SP(4, "SPACE");

    @Getter
    private final int value;
    @Getter
    private final String name;



    static final public String[] ParityTypes= {
        "NO_PARITY", "ODD_PARITY", "EVEN_PARITY", "MARK_PARITY", "SPACE_PARITY"
    };
    private static final List<Integer> VALUES;

    static {
        VALUES = new ArrayList<>();
        for (ParityList someEnum : ParityList.values()) {
            VALUES.add(someEnum.value);
        }
    }
    ParityList(int value, String name) {
        this.value = value;
        this.name = name;
    }


    public String[] getTypes(){
        return ParityTypes;
    }

    public static List<Integer> getValues() {
        return Collections.unmodifiableList(VALUES);
    }

    public static  String getNameLikeArray(int number){
        ParityList[] parityLists = ParityList.values();
        return parityLists[number].name;
    }


}
