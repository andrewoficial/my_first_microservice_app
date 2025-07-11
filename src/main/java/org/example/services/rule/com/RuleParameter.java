package org.example.services.rule.com;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface RuleParameter {
    String name();
    String description() default "";
    String type() default "STRING"; // FLOAT, INT, BOOLEAN, etc.
    double min() default Double.MIN_VALUE;
    double max() default Double.MAX_VALUE;
    String[] options() default {}; // Для выпадающих списков
}