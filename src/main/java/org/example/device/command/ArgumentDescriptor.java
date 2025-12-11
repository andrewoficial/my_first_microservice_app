package org.example.device.command;

import lombok.Getter;

import java.util.function.Predicate;

// Descriptor для аргументов (для UI generation и validation)
public class ArgumentDescriptor {
    @Getter
    private String name;          // e.g., "value"
    @Getter
    private Class<?> type;        // e.g., Float.class, Path.class for files
    @Getter
    private Object defaultValue;  // optional
    private Predicate<Object> validator;  // e.g., val -> (Float)val > 0

    public ArgumentDescriptor(String name, Class<?> type, Object defaultValue, Predicate<Object> validator) {
        this.name = name;
        this.type = type;
        this.defaultValue = defaultValue;
        this.validator = validator;
    }

    public boolean validate(Object value) {
        return validator == null || validator.test(value);
    }
}