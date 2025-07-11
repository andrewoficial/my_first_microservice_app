package org.example.services.rule;

import org.example.services.rule.com.ComRule;
import org.example.services.rule.com.MipexZeroingRule;
import org.example.services.rule.com.OwonVoltLinearRule;
import org.example.services.rule.com.RuleType;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

public class RuleFactory {
    public static ComRule createRule(Class<? extends ComRule> ruleClass,
                                     Map<String, String> params) throws InvocationTargetException, InstantiationException, IllegalAccessException {
        Constructor<?> constructor = ruleClass.getDeclaredConstructors()[0];
        List<ParamMetadata> metadata = ParamMetadata.fromClass(ruleClass);
        Object[] args = new Object[metadata.size()];

        for (int i = 0; i < metadata.size(); i++) {
            ParamMetadata meta = metadata.get(i);
            String value = params.get(meta.getName());
            args[i] = convertValue(value, meta);
        }

        return (ComRule) constructor.newInstance(args);
    }
    private static Object convertValue(String value, ParamMetadata meta) {
        try {
            return switch (meta.getType()) {
                case "FLOAT" -> Float.parseFloat(value);
                case "INT" -> Integer.parseInt(value);
                case "BOOLEAN" -> Boolean.parseBoolean(value);
                default -> value; // STRING и другие
            };
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Invalid value for parameter '" + meta.getName() + "': " + value, e
            );
        }
    }
}