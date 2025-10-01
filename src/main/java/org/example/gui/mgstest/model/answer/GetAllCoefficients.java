package org.example.gui.mgstest.model.answer;

import lombok.Data;

import java.util.Arrays;

@Data
public class GetAllCoefficients {
    private final double[] o2Coef = new double[19];
    private final double[] coCoef = new double[14];
    private final double[] h2sCoef = new double[14];
    private final double[] ch4Pressure = new double[7];
    private final double[] acceleration = new double[4];
    private final double[] ppmMgKoefs = new double[4];
    private final double[] vRange = new double[6];

    @Override
    public String toString() {
        return "GetAllCoefficients{\n" +
                "o2Coef=" + Arrays.toString(o2Coef) + "\n" +
                ", coCoef=" + Arrays.toString(coCoef) + "\n" +
                ", h2sCoef=" + Arrays.toString(h2sCoef) + "\n" +
                ", ch4Pressure=" + Arrays.toString(ch4Pressure) + "\n" +
                ", acceleration=" + Arrays.toString(acceleration) + "\n" +
                ", ppmMgKoefs=" + Arrays.toString(ppmMgKoefs) + "\n" +
                ", vRange=" + Arrays.toString(vRange) + "\n" +
                '}';
    }
}
