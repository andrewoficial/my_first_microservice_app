package org.example.services.comPort;
import java.util.*;


public enum StringEndianList {

    NO(new char[0]),
    CR(new char[] {13}),
    LF(new char[] {10}),
    CR_LF(new char[] {13, 10});


    private final char [ ] value;
    private static final List<char []> VALUES;

    static {
        VALUES = new ArrayList<>();
        for (StringEndianList someEnum : StringEndianList.values()) {
            VALUES.add(someEnum.value);
        }
    }

    StringEndianList(char [] value) {
        this.value = value;
    }

    public char[] getValue() {
        return value;
    }

    public static List<char []> getValues() {
        return Collections.unmodifiableList(VALUES);
    }

    public static  char[] getNameLikeArray(int number){
        List<char []> values = StringEndianList.getValues();
        return values.get(number);
    }

    public byte[] getBytes() {
        byte [] result = new byte[value.length];
        for (int i = 0; i < value.length; i++) {
            result[i] = (byte)value[i];
        }
        return result;
    }

    public char[] getChars() {
        return value;
    }

    public String getString() {
        return new String(value);
    }
}
