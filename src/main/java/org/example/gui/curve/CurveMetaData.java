package org.example.gui.curve;

import lombok.Getter;
import lombok.Setter;

public class CurveMetaData implements Cloneable{
        /*
        (Old Information at 16.07.2025)
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
        @Getter @Setter
        Integer numberInDeviceMemory;
        @Getter @Setter
        Boolean isUserCurve;

        @Override
        public CurveMetaData clone() {
                try {
                        // Поверхностное копирование безопасно, т.к. все поля:
                        // - String/Integer (неизменяемые)
                        // - Enum (константы)
                        // - Boolean (неизменяемый)
                        return (CurveMetaData) super.clone();
                } catch (CloneNotSupportedException e) {
                        // В случае ошибки (невозможно для Cloneable)
                        throw new AssertionError("Cloning failed", e);
                }
        }
    }