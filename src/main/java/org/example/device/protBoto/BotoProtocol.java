package org.example.device.protBoto;

/**
 * Названия и параметры протоколов термокамер BOTO.
 *
 * <p><b>Не путать две камеры — у них РАЗНЫЕ протоколы.</b> Подробности и история
 * в {@code boto_cameras.md} (главный источник правды), карты регистров —
 * в {@code boto_120_register_map.md} и {@code boto_800_register_map.md}.
 *
 * <p>Мнемоника: адреса {@code 12/100/105} и масштаб {@code ×100} — это
 * {@link #CHINA_MODBUS_120} (B-TH-120 E), а адреса {@code 10/60/63} и масштаб
 * {@code ×10} — это {@link #BOTO_800_MODBUS} (B-TH-800 F).
 */
public enum BotoProtocol {

    /**
     * {@code China Modbus} — термокамера <b>B-TH-120 E</b>, прошивка {@code V3.4.2}.
     *
     * <p>Modbus RTU 9600 8N1, slave 1, <b>только COM</b> (Ethernet нет).
     * Масштаб {@code ×100}: рег 12 = PV, рег 100 = уставка, рег 105 = вкл/выкл.
     * Это название («China Modbus») используется и в других проектах.
     */
    CHINA_MODBUS_120(
            "China Modbus", "B-TH-120 E", "V3.4.2", "своя программа (название неизвестно)",
            100, 12, 100, 105, "boto_120_register_map.md"),

    /**
     * {@code BOTO-800 Modbus} (синонимы: {@code UApp}, {@code U-680}) — термокамера
     * <b>B-TH-800 F</b>, прошивка {@code v4.2.4}, программа {@code UApp_boxed.exe}.
     *
     * <p>Modbus RTU 9600 8N1 по COM <b>и</b> Modbus TCP по Ethernet (порт 8000).
     * Масштаб {@code ×10}: рег 10 = PV, рег 60 = уставка, рег 63 = вкл/выкл.
     * Блок реального времени читается одним запросом: рег 10..49.
     */
    BOTO_800_MODBUS(
            "BOTO-800 Modbus", "B-TH-800 F", "v4.2.4", "UApp_boxed.exe (U-680)",
            10, 10, 60, 63, "boto_800_register_map.md");

    private final String displayName;
    private final String cameraModel;
    private final String firmware;
    private final String programName;
    private final int scale;
    private final int tempReg;
    private final int setpointReg;
    private final int modeReg;
    private final String docFile;

    BotoProtocol(String displayName, String cameraModel, String firmware, String programName,
                 int scale, int tempReg, int setpointReg, int modeReg, String docFile) {
        this.displayName = displayName;
        this.cameraModel = cameraModel;
        this.firmware = firmware;
        this.programName = programName;
        this.scale = scale;
        this.tempReg = tempReg;
        this.setpointReg = setpointReg;
        this.modeReg = modeReg;
        this.docFile = docFile;
    }

    /** Каноническое название протокола, как оно названо в других проектах. */
    public String displayName() {
        return displayName;
    }

    /** Модель камеры: {@code B-TH-120 E} или {@code B-TH-800 F}. */
    public String cameraModel() {
        return cameraModel;
    }

    /** Версия прошивки камеры. */
    public String firmware() {
        return firmware;
    }

    /** Программа, установленная на камере. */
    public String programName() {
        return programName;
    }

    /** Масштаб значений: {@code 100} для China Modbus, {@code 10} для BOTO-800. */
    public int scale() {
        return scale;
    }

    /** Адрес регистра с текущей температурой. */
    public int tempReg() {
        return tempReg;
    }

    /** Адрес регистра с уставкой температуры. */
    public int setpointReg() {
        return setpointReg;
    }

    /** Адрес регистра вкл/выкл (1 = работа, 0 = стоп). */
    public int modeReg() {
        return modeReg;
    }

    /** Файл с картой регистров. */
    public String docFile() {
        return docFile;
    }

    /** Перевод физического значения в raw для записи/чтения регистра. */
    public int toRaw(double physical) {
        return (int) Math.round(physical * scale);
    }

    /** Перевод raw-значения в физическое. */
    public double fromRaw(int raw) {
        return raw / (double) scale;
    }

    /** Есть ли у камеры Ethernet-вариант (Modbus TCP). */
    public boolean hasEthernet() {
        return this == BOTO_800_MODBUS;
    }

    @Override
    public String toString() {
        return displayName + " (" + cameraModel + " " + firmware + ")";
    }
}
