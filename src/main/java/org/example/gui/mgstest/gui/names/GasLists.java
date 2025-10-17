package org.example.gui.mgstest.gui.names;

import java.util.HashMap;

public class GasLists {

    public final HashMap<Byte, String> O2;//CH1
    public final HashMap<Byte, String> CO;//CH2
    public final HashMap<Byte, String> H2S;//CH3
    public final HashMap<Byte, String> CH4;//CH4


public GasLists() {
            O2 = new HashMap<>();
            byte i = 1;
            O2.put(i++, "Кислород (О2)");
            O2.put(i++, "CO");
            O2.put(i++, "H2S");
            O2.put(i++, "Cl2");
            O2.put(i++, "NH3"); // 5
            O2.put(i++, "SO2");
            O2.put(i++, "NO2");
            O2.put(i++, "H2");
            O2.put(i++, "CH3SH");
            O2.put(i++, "CH3OH"); // 10
            O2.put(i++, "C2H5SH");
            O2.put(i++, "C2H4O");
            O2.put(i, "NO");

            CO = new HashMap<>();
            CO.put((byte) 0x01, "CO-DT");
            CO.put((byte) 0x02, "CO");
            CO.put((byte) 0x03, "Optical sensor gas");
            CO.put((byte) 0x12, "O2");
            CO.put((byte) 0x22, "CO");
            CO.put((byte) 0x32, "H2S");
            CO.put((byte) 0x42, "Cl2");
            CO.put((byte) 0x52, "NH3");
            CO.put((byte) 0x62, "SO2");
            CO.put((byte) 0x72, "NO2");
            CO.put((byte) 0x82, "H2");
            CO.put((byte) 0x92, "CH3SH");
            CO.put((byte) 0xA2, "CH3OH");
            CO.put((byte) 0xB2, "CO (DT)");
            CO.put((byte) 0xC2, "H2S (DT)");
            CO.put((byte) 0xD2, "C2H5SH");
            CO.put((byte) 0xE2, "C2H4O");
            CO.put((byte) 0xF2, "NO");
            CO.put((byte) 0x11, "SO2+DT");

            H2S = new HashMap<>();
            H2S.put((byte) 0x01, "CO-DT");
            H2S.put((byte) 0x02, "CO");
            H2S.put((byte) 0x03, "Optical sensor gas");
            H2S.put((byte) 0x12, "O2");
            H2S.put((byte) 0x22, "CO");
            H2S.put((byte) 0x32, "H2S");
            H2S.put((byte) 0x42, "Cl2");
            H2S.put((byte) 0x52, "NH3");
            H2S.put((byte) 0x62, "SO2");
            H2S.put((byte) 0x72, "NO2");
            H2S.put((byte) 0x82, "H2");
            H2S.put((byte) 0x92, "CH3SH");
            H2S.put((byte) 0xA2, "CH3OH");
            H2S.put((byte) 0xB2, "CO (DT)");
            H2S.put((byte) 0xC2, "H2S (DT)");
            H2S.put((byte) 0xD2, "C2H5SH");
            H2S.put((byte) 0xE2, "C2H4O");
            H2S.put((byte) 0xF2, "NO");
            H2S.put((byte) 0x11, "SO2+DT");

            CH4 = new HashMap<>();
            i = 1;
            CH4.put(i++, "CH4");
            CH4.put(i++, "C3H8");
            CH4.put(i++, "CO2");
            CH4.put(i++, "C2H6 (Этан)");
            CH4.put(i++, "C4H10");
            CH4.put(i++, "C5H12");
            CH4.put(i++, "C3H6");
            CH4.put(i++, "CH3OH");
            CH4.put(i++, "C6H6");
            CH4.put(i++, "C7H16");
            CH4.put(i++, "Diesel");
            CH4.put(i++, "Kirosene");
            CH4.put(i++, "AV_GASOLINE");
            CH4.put(i++, "UNLEADED_GASOLINE");
            CH4.put(i++, "GASOLINE_VAPOUR");
            CH4.put(i++, "HYDROCARBON_MIX");
            CH4.put(i, "CH3COCH3");
        }

}

