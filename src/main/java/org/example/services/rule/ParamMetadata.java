package org.example.services.rule;

import lombok.Getter;
import lombok.Setter;
import org.example.services.rule.com.OwonVoltLinearRule;
import org.example.services.rule.com.RuleParameter;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

public class ParamMetadata {
    @Getter @Setter
    String name;
    @Getter @Setter
    String description;
    @Getter @Setter
    String type; // тип из аннотации (FLOAT, INT и т.д.)
    @Getter @Setter
    double min;
    @Getter @Setter
    double max;
    @Getter @Setter
    String [] options;
    @Getter @Setter
    Class<?> javaType; // реальный тип в Java (float, int и т.д.)
    public ParamMetadata(Parameter param, RuleParameter annotation) {
        this.name = annotation.name();
        this.description = annotation.description();
        this.type = annotation.type();
        this.min = annotation.min();
        this.max = annotation.max();
        this.options = annotation.options();
        this.javaType = param.getType();
    }

    public static List<ParamMetadata> fromClass(Class<?> ruleClass) {
        // Берем первый конструктор (предполагаем, что он один и аннотирован)
        System.out.println("получаю параметры из объекта ruleClass: " + ruleClass);
        Constructor<?> constructor = ruleClass.getDeclaredConstructors()[0];
        System.out.println("Получил конструктор");
        Parameter[] parameters = constructor.getParameters();
        System.out.println("Получил Параметры");
        List<ParamMetadata> metadataList = new ArrayList<>();
        for (Parameter param : parameters) {

            RuleParameter annotation = param.getAnnotation(RuleParameter.class);
            System.out.println("Перечисляю параметр: " + param);
            if (annotation != null) {
                System.out.println("Он аннотирован, как: " + annotation);
                metadataList.add(new ParamMetadata(param, annotation));
            } else {
                System.out.println("Он НЕ аннотирован");
                // Если аннотации нет, но мы хотим все равно показать параметр?
            }
        }
        System.out.println("Возвращаемый список пуст: " + metadataList.isEmpty());
        return metadataList;
    }

}



