package org.example.services.rule.com;

public enum RuleType {
    OWON_VOLTAGE_LINEAR(OwonVoltLinearRule.class, "Линейное изменение напряжения (Owon)"),
    OWON_VOLTAGE_SINUS(OwonVoltSinusRule.class, "Синусоидальное изменение напряжения (Owon)"),
    MIPEX_ZEROING(MipexZeroingRule.class, "Обнуление MIPEX");

    private final Class<? extends ComRule> ruleClass;
    private final String displayName;
    RuleType(Class<? extends ComRule> ruleClass, String displayName) {
        this.ruleClass = ruleClass;
        this.displayName = displayName;
    }
    public Class<? extends ComRule> getRuleClass() {
        return ruleClass;
    }
    @Override
    public String toString() {
        return displayName;
    }
}
