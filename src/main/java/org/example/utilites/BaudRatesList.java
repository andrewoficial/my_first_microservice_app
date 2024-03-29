/*
Перечень типичных скоростей ком-порта
 */
package org.example.utilites;

import java.util.*;

public enum BaudRatesList {

        B75(75),
        B110(110),
        B150(150),
        B300(300),
        B600(600),
        B1200(1200),
        B2400(2400),
        B4800(4800),
        B9600(9600),
        B19200(19200),
        B38400(38400),
        B57600(57600),
        B115200(115200);

        private final int value;
        private static final List<Integer> VALUES;

        static {
                VALUES = new ArrayList<>();
                for (BaudRatesList someEnum : BaudRatesList.values()) {
                        VALUES.add(someEnum.value);
                }
        }
        BaudRatesList(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static List<Integer> getValues() {
                return Collections.unmodifiableList(VALUES);
        }

        public static  Integer getLikeArray (int number){
                List<Integer> values = BaudRatesList.getValues();
                return values.get(number);
        }
}
