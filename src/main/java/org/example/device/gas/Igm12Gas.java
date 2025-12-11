package org.example.device.gas;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Enum representing gas information for IGM-10/11 device according to the reference table.
 * Contains 89 gas definitions from code 000 to 088.
 */
public enum Igm12Gas {
    // Format: GAS(CODE, NAME, FORMULA, TYPE, RANGE, PARAM1, PARAM2, PARAM3)
    
    // Code 000-009
    METHANE_000(0, "Метан", "CH4", GasType.M, 9999, 2, 1.000000000, 1.000000000),
    METHANE_001(1, "Метан", "CH4", GasType.M, 440, 1, 1.000000000, 1.000000000),
    PROPANE_002(2, "Пропан", "C3H8", GasType.M, 170, 1, 2.000000000, 1.000000000),
    CARBON_DIOXIDE_003(3, "Диоксид углерода", "CO2", GasType.M, 500, 2, 1.000000000, 1.000000000),
    ETHANE_004(4, "Этан", "C2H6", GasType.M, 240, 1, 0.000000000, 1.000000000),
    BUTANE_005(5, "Бутан", "С4Н10", GasType.M, 140, 1, 0.000000000, 1.000000000),
    ISOBUTANE_006(6, "Изобутан", "(СН3)2СНСН3", GasType.M, 130, 1, 0.000000000, 1.000000000),
    PENTANE_007(7, "Пентан", "С5Н12", GasType.M, 110, 1, 0.000000000, 1.000000000),
    CYCLOPENTANE_008(8, "Циклопентан", "С5Н10", GasType.M, 100, 1, 1.000000000, 1.000000000),
    HEXANE_009(9, "Гексан", "С6Н14", GasType.M, 100, 1, 0.000000000, 1.000000000),
    
    // Code 010-019
    CYCLOHEXANE_010(10, "Циклогексан", "С6Н12", GasType.M, 100, 1, 1.000000000, 1.000000000),
    ETHYLENE_011(11, "Этилен", "С2Н4", GasType.M, 230, 1, 1.000000000, 1.000000000),
    METHANOL_012(12, "Метанол", "СН3ОН", GasType.M, 600, 1, 0.000000000, 1.000000000),
    HEPTANE_013(13, "Гептан", "С7Н16", GasType.M, 85, 1, 1.000000000, 1.000000000),
    PROPYLENE_014(14, "Пропилен", "С3Н6", GasType.M, 200, 1, 1.000000000, 1.000000000),
    ETHANOL_015(15, "Этанол", "С2Н5ОН", GasType.M, 310, 1, 1.000000000, 1.000000000),
    TOLUENE_016(16, "Толуол", "C7H8", GasType.M, 50, 1, 1.000000000, 1.000000000),
    BENZENE_017(17, "Бензол", "С6Н6", GasType.M, 120, 1, 1.000000000, 1.000000000),
    ETHYLBENZENE_018(18, "Этилбензол", "С8Н10", GasType.M, 80, 1, 0.000000000, 1.000000000),
    ACETONE_019(19, "Ацетон", "СН3СОСН3", GasType.M, 250, 1, 1.000000000, 1.000000000),
    
    // Code 020-029
    P_XYLENE_020(20, "П-Ксилол", "P-С8Н10", GasType.M, 90, 1, 1.000000000, 1.000000000),
    O_XYLENE_021(21, "О-Ксилол", "O-С8Н10", GasType.M, 100, 1, 1.000000000, 1.000000000),
    ISOPROPANOL_022(22, "Изопропанол", "С3Н8О", GasType.M, 200, 1, 1.000000000, 1.000000000),
    OXYGEN_023(23, "Кислород", "O2", GasType.E, 3000, 2, 1.000000000, 1.000000000),
    CARBON_MONOXIDE_024(24, "Оксид углерода", "CO", GasType.E, 5000, 0, 1.000000000, 1.000000000),
    HYDROGEN_SULFIDE_025(25, "Сероводород", "H2S", GasType.E, 20000, 1, 1.000000000, 1.000000000),
    SULFUR_DIOXIDE_026(26, "Диоксид серы", "SO2", GasType.E, 20000, 1, 1.000000000, 1.000000000),
    NITROGEN_DIOXIDE_027(27, "Диоксид азота", "NO2", GasType.E, 20000, 1, 1.000000000, 1.000000000),
    CHLORINE_028(28, "Хлор", "CL2", GasType.E, 2000, 1, 1.000000000, 1.000000000),
    AMMONIA_029(29, "Аммиак", "NH3", GasType.E, 1000, 0, 1.000000000, 1.000000000),
    
    // Code 030-039
    NITRIC_OXIDE_030(30, "Оксид азота", "NO", GasType.E, 2000, 0, 1.000000000, 1.000000000),
    HYDROGEN_CYANIDE_031(31, "Циановодород", "HCN", GasType.E, 1000, 1, 1.000000000, 1.000000000),
    HYDROGEN_032(32, "Водород", "H2", GasType.E, 400, 2, 1.000000000, 1.000000000),
    DIESEL_FUEL_033(33, "Дизтопливо", "DT", GasType.M, 200, 1, 1.550000000, 1.000000000),
    METHANOL_PDK_034(34, "Метанол(ПДК)", "СН3ОН", GasType.E, 2000, 1, 1.000000000, 1.000000000),
    HYDROGEN_FLUORIDE_035(35, "Фтористый водород", "HF", GasType.E, 100, 1, 1.000000000, 1.000000000),
    HYDROGEN_CHLORIDE_036(36, "Хлористый водород", "HCL", GasType.E, 300, 1, 1.000000000, 1.000000000),
    MONOETHANOLAMINE_037(37, "Моноэтаноламин", "C2H7NO", GasType.P, 200, 2, 1.000000000, 1.000000000),
    PID1_038(38, "PID1", "PID1", GasType.P, 1000, 1, 1.000000000, 1.000000000),
    BUTYL_ACETATE_039(39, "Бутилацетат", "C6H12O2", GasType.M, 120, 1, 1.474836470, 1.000000000),
    
    // Code 040-049
    BUTADIENE_040(40, "Бутадиен-1,3", "СН2=СНСН=СН2", GasType.M, 140, 1, 0.000000000, 1.000000000),
    HEXENE_041(41, "Гексен-1", "С6Н12", GasType.M, 120, 1, 0.000000000, 1.000000000),
    BUTENE_042(42, "Бутен-1", "С4Н8", GasType.M, 160, 1, 0.000000000, 1.000000000),
    ISOBUTYLENE_043(43, "Изобутилен", "i-С4Н8", GasType.M, 160, 1, 0.000000000, 1.000000000),
    GASOLINE_51313_99_044(44, "Бензин 51313-99", "Gasoline", GasType.M, 140, 1, 0.000000000, 1.000000000),
    KEROSENE_52050_06_045(45, "Керосин 52050-06", "Kerosene", GasType.M, 70, 1, 0.000000000, 1.000000000),
    PID2_046(46, "PID2", "PID2", GasType.P, 1000, 2, 1.000000000, 1.000000000),
    PID3_047(47, "PID3", "PID3", GasType.P, 1000, 3, 1.000000000, 1.000000000),
    ISOPENTANE_048(48, "Изопентан", "i-C5H12", GasType.M, 130, 1, 0.000000000, 1.000000000),
    ETHYLENE_OXIDE_049(49, "Этиленоксид", "C2H4O", GasType.M, 260, 1, 1.000000000, 1.000000000),
    
    // Code 050-059
    ARSINE_050(50, "Арсин", "AsH3", GasType.E, 100, 2, 1.000000000, 1.000000000),
    PHOSPHINE_051(51, "Фосфин", "PH3", GasType.E, 30000, 2, 1.000000000, 1.000000000),
    MONOSILANE_052(52, "Моносилан", "SiH4", GasType.E, 500, 1, 1.000000000, 1.000000000),
    CARBONYL_CHLORIDE_053(53, "Карбонилхлорид", "COCl2", GasType.E, 100, 2, 1.000000000, 1.000000000),
    METHYL_MERCAPTAN_054(54, "Метилмеркаптан", "CH3SH", GasType.E, 100, 1, 1.000000000, 1.000000000),
    OZONE_055(55, "Озон", "O3", GasType.E, 25, 2, 1.000000000, 1.000000000),
    BROMINE_056(56, "Бром", "Br2", GasType.E, 50, 1, 1.000000000, 1.000000000),
    FORMALDEHYDE_057(57, "Формальдегид", "CH2O", GasType.E, 100, 1, 1.000000000, 1.000000000),
    ETHYL_MERCAPTAN_058(58, "Этилмеркаптан", "C2H5SH", GasType.E, 140, 1, 1.000000000, 1.000000000),
    ETHYLENE_OXIDE_PDK_059(59, "Этиленоксид(ПДК)", "C2H4O", GasType.E, 1000, 1, 1.000000000, 1.000000000),
    
    // Code 060-069
    ETHYLENE_PDK_060(60, "Этилен(ПДК)", "C2H4", GasType.E, 1000, 1, 1.000000000, 1.000000000),
    ETHANOL_PDK_061(61, "Этанол(ПДК)", "C2H5OH", GasType.E, 2000, 1, 1.000000000, 1.000000000),
    STYRENE_062(62, "Стирол", "С8Н8", GasType.M, 110, 1, 0.000000000, 1.000000000),
    N_OCTANE_063(63, "Н-октан", "C8H18", GasType.M, 80, 1, 1.000000000, 1.000000000),
    MTBE_064(64, "МТБЭ", "CH3CO(CH3)3", GasType.M, 150, 1, 0.000000000, 1.000000000),
    NONANE_065(65, "Нонан", "C9H2", GasType.M, 70, 1, 1.000000000, 1.000000000),
    DECANE_066(66, "Декан", "C10H22", GasType.M, 70, 1, 1.000000000, 1.000000000),
    ETHYL_ACETATE_067(67, "Этилацетат", "CH3COOCH2CH3", GasType.M, 220, 1, 1.000000000, 1.000000000),
    WHITE_SPIRIT_068(68, "Уайт-спирит", "Нефрас-С4", GasType.M, 140, 1, 1.000000000, 1.000000000),
    FUEL_RD_069(69, "Топливо РД", "ТРД", GasType.M, 100, 1, 1.000000000, 1.000000000),
    
    // Code 070-079
    AVIATION_GASOLINE_070(70, "Бензин авиационный", "БА", GasType.M, 600, 1, 1.000000000, 1.000000000),
    UNLEADED_GASOLINE_071(71, "Бензин неэтилированный", "БНЭ", GasType.M, 100, 1, 1.000000000, 1.000000000),
    ISOPRENE_072(72, "Изопропен", "C5H8", GasType.M, 170, 1, 1.000000000, 1.000000000),
    DICHLOROETHANE_073(73, "Дихлорэтан-1,2", "C2H4CL2", GasType.M, 620, 1, 1.000000000, 1.000000000),
    DIMETHYL_SULFIDE_074(74, "Диметилсульфид", "C2H5SH", GasType.M, 220, 1, 1.000000000, 1.000000000),
    BUTANOL_075(75, "Бутанол-1", "C4H9OH", GasType.M, 140, 1, 1.000000000, 1.000000000),
    VINYL_CHLORIDE_076(76, "Винилхлорид", "C2H3CL", GasType.M, 360, 1, 1.000000000, 1.000000000),
    CYCLOPROPANE_077(77, "Циклопропан", "C3H6", GasType.M, 240, 1, 1.000000000, 1.000000000),
    DIETHYL_ETHER_078(78, "Диэтиловый эфир", "C4H10O", GasType.M, 170, 1, 1.000000000, 1.000000000),
    PROPYLENE_OXIDE_079(79, "Пропиленоксид", "C3H6O", GasType.M, 190, 1, 1.000000000, 1.000000000),
    
    // Code 080-088
    CHLOROBENZENE_080(80, "Хлорбензол", "C6H5CL", GasType.M, 130, 1, 1.000000000, 1.000000000),
    BUTANOL_2METHYL_2PROPANOL_081(81, "2-Метил-2-Пропанол", "(CH3)3COH", GasType.M, 180, 1, 1.000000000, 1.000000000),
    BUTANONE_082(82, "2-Бутанон", "C4H8O", GasType.M, 180, 1, 1.000000000, 1.000000000),
    PETROLEUM_VAPORS_083(83, "Пары нефтепродуктов", "ПН", GasType.M, 126, 1, 0.000000000, 1.000000000),
    HYDROCARBONS_SUM_084(84, "Сумма углеводородов", "CxHx", GasType.M, 170, 1, 1.000000000, 1.000000000),
    HYDROCARBONS_CH4_TK_085(85, "Сумма CxHy по CH4 (ТК)", "CxHy", GasType.T, 440, 2, 1.000000000, 1.000000000),
    HYDROCARBONS_C3H8_TK_086(86, "Сумма CxHy по C3H8 (ТК)", "CxHy", GasType.T, 170, 2, 1.000000000, 1.000000000),
    FLUORINE_087(87, "Фтор", "F2", GasType.E, 100, 2, 1.000000000, 1.000000000),
    DICHLOROETHANE_1_2_088(88, "1,2-Дихлорэтан", "C2H4C12", GasType.M, 620, 1, 0.000000000, 1.000000000);
    
    private final int code;
    private final String name;
    private final String formula;
    private final GasType type;
    private final int range;
    private final int param1;
    private final double param2;
    private final double param3;
    
    private static final Igm12Gas[] BY_CODE = new Igm12Gas[89];
    
    static {
        for (Igm12Gas gas : values()) {
            BY_CODE[gas.code] = gas;
        }
    }
    
    Igm12Gas(int code, String name, String formula, GasType type, int range, int param1, double param2, double param3) {
        this.code = code;
        this.name = name;
        this.formula = formula;
        this.type = type;
        this.range = range;
        this.param1 = param1;
        this.param2 = param2;
        this.param3 = param3;
    }
    
    // Getters
    public int getCode() { return code; }
    public String getName() { return name; }
    public String getFormula() { return formula; }
    public GasType getType() { return type; }
    public int getRange() { return range; }
    public int getParam1() { return param1; }
    public double getParam2() { return param2; }
    public double getParam3() { return param3; }
    
    /**
     * Returns gas information by code (0-88)
     * 
     * @param code gas code (0-88)
     * @return Igm10Gas instance or null if code is invalid
     */
    public static Igm12Gas byCode(int code) {
        if (code < 0 || code >= BY_CODE.length) {
            return null;
        }
        return BY_CODE[code];
    }
    
    /**
     * Returns gas name by code
     * 
     * @param code gas code (0-88)
     * @return gas name or "Unknown" if code is invalid
     */
    public static String getNameByCode(int code) {
        Igm12Gas gas = byCode(code);
        return gas != null ? gas.getName() : "Unknown";
    }
    
    /**
     * Returns all gases filtered by type
     * 
     * @param type gas type to filter by
     * @return list of gases of specified type
     */
    public static List<Igm12Gas> getByType(GasType type) {
        return Arrays.stream(values())
                .filter(gas -> gas.getType() == type)
                .collect(Collectors.toList());
    }
}