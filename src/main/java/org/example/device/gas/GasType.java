package org.example.device.gas;

/**
 * Gas type enumeration for IGM-10/11 device.
 * M - Метан (Methane/combustible)
 * E - Электрохимический (Electrochemical/toxic)
 * P - PID (Photoionization detector)
 * T - Термокаталитический (Thermocatalytic)
 */
public enum GasType {
    M("Горючие газы", "Combustible gases"),
    E("Токсичные газы", "Toxic gases"),
    P("PID детектор", "PID detector"),
    T("Термокаталитический", "Thermocatalytic");
    
    private final String descriptionRu;
    private final String descriptionEn;
    
    GasType(String descriptionRu, String descriptionEn) {
        this.descriptionRu = descriptionRu;
        this.descriptionEn = descriptionEn;
    }
    
    public String getDescriptionRu() { return descriptionRu; }
    public String getDescriptionEn() { return descriptionEn; }
}