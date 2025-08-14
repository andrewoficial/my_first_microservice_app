package org.example.gui.accu10fd.table;

public class GasData {
    private final String name;
    private final String code;
    private final double specificHeat;
    private final double density;
    private final double conversionCoefficient;

    public GasData(String name, String code, double specificHeat, double density, double conversionCoefficient) {
        this.name = name;
        this.code = code;
        this.specificHeat = specificHeat;
        this.density = density;
        this.conversionCoefficient = conversionCoefficient;
    }

    public String getName() { return name; }
    public String getCode() { return code; }
    public double getSpecificHeat() { return specificHeat; }
    public double getDensity() { return density; }
    public double getConversionCoefficient() { return conversionCoefficient; }

    @Override
    public String toString() {
        return name + " (" + code + ")";
    }
}