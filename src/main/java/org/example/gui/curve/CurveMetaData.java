package org.example.gui.curve;

import lombok.Getter;
import lombok.Setter;

public class CurveMetaData{
        /*
        Sensor Model:   GD97-BPI
        Serial Number:  G20109
        Data Format:    2      (Volts vs Kelvin)
        SetPoint Limit: 325      (Kelvin)
        Temperature coefficient:  1 (Negative)
        Number of Breakpoints:   160
        
        No.  Units  Temperature (K)
         */
        @Getter @Setter
        String sensorModel;
        @Getter @Setter
        String serialNumber;
        @Getter @Setter
        CurveDataTypes dataFormat;
        @Getter @Setter
        Integer setPointLimit;
        @Getter @Setter
        String temperatureCoefficient;
        @Getter @Setter
        Integer numberOfBreakpoints;
    }