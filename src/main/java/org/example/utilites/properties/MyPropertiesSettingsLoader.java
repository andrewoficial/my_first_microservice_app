package org.example.utilites.properties;

import java.util.Arrays;
import java.util.Properties;
import java.util.stream.Collectors;

import jakarta.persistence.criteria.CriteriaBuilder;
import org.apache.log4j.Logger;

public class MyPropertiesSettingsLoader {
    private final MyPropertiesFileHandler fileHandler;
    private final Properties properties;
    private static final Logger log = Logger.getLogger(MyPropertiesSettingsLoader.class);

    public MyPropertiesSettingsLoader(MyPropertiesFileHandler fileHandler, Properties properties) {
        this.fileHandler = fileHandler;
        this.properties = properties;
    }

    public boolean getBoolean(String name, boolean defaultValue) {
        if(name == null){
            log.warn("в loadBooleanByName не передано название параметра ");
            return defaultValue;
        }

        if(! properties.containsKey(name)){
            log.warn("в configAccess.properties отсутствует свойство: " + name + ". Он будет добавлен и установлен в " + defaultValue);
            properties.put(name, String.valueOf(defaultValue));
            fileHandler.updateFileFromProperties(properties);
            return defaultValue;
        }
        if(properties.getProperty(name) == null){
            log.warn("в configAccess.properties найдено свойтво: " + name + ". Но ему не задан параметр. Он будет установлен в " + defaultValue);
            properties.setProperty(name, String.valueOf(defaultValue));
            fileHandler.updateFileFromProperties(properties);
            return defaultValue;
        }

        try{
            return Boolean.parseBoolean(properties.getProperty(name));
        }catch (NumberFormatException exception){
            log.warn("configAccess.properties содержит недопустимое значение параметра " + name + ". Он будет установлен " + defaultValue);
            properties.setProperty(name, String.valueOf(defaultValue));
            fileHandler.updateFileFromProperties(properties);
            return defaultValue;
        }
    }


    public int getInt(String name, int defaultValue) {
        if(name == null){
            log.warn("в loadIntByName не передано название параметра ");
            return defaultValue;
        }

        if(! properties.containsKey(name)){
            log.warn("в configAccess.properties отсутствует свойство: " + name + ". Он будет добавлен и установлен в " + defaultValue);
            properties.put(name, String.valueOf(defaultValue));
            fileHandler.updateFileFromProperties(properties);
        }
        if(properties.getProperty(name) == null){
            log.warn("в configAccess.properties найдено свойтво: " + name + ". Но ему не задан параметр. Он будет установлен в " + defaultValue);
            properties.setProperty(name, String.valueOf(defaultValue));
            fileHandler.updateFileFromProperties(properties);
            return defaultValue;
        }


        try{
            return Integer.parseInt(properties.getProperty(name));
        }catch (NumberFormatException exception){
            log.warn("configAccess.properties содержит недопустимое значение параметра " + name + ". Он будет установлен в " + defaultValue);
            properties.setProperty(name, String.valueOf(defaultValue));
            fileHandler.updateFileFromProperties(properties);
            return defaultValue;
        }
    }

    public String getString(String name, String defaultValue) {
        if (name == null) {
            log.warn("в loadStringByName не передано название параметра");
            return "";
        }

        if(defaultValue == null){
            log.warn("в loadStringByName не передано значение по умолчанию");
            return "";
        }

        if (!properties.containsKey(name)) {
            log.warn("в configAccess.properties отсутствует свойство: " + name + ". Он будет добавлен и установлен в значение " + defaultValue);
            properties.put(name, defaultValue);
            fileHandler.updateFileFromProperties(properties);
            return defaultValue;
        }

        if (properties.getProperty(name) == null) {
            log.warn("в configAccess.properties найдено свойство: " + name + ". Но ему не задан параметр. Он будет установлен в значение " + defaultValue);
            properties.setProperty(name, defaultValue);
            fileHandler.updateFileFromProperties(properties);
            return defaultValue;
        }

        String value = properties.getProperty(name).trim();
        if (value.isEmpty()) {
            log.warn("configAccess.properties содержит пустое значение для параметра " + name + ". Оно будет заменено на " + defaultValue);
            properties.setProperty(name, defaultValue);
            fileHandler.updateFileFromProperties(properties);
            return defaultValue;
        }

        return value;
    }



    public String[] getStringArray(String name, String defaultValue, int minSize) {
        if(minSize < 0){
            log.warn("в loadStringByName не передано минимальное количество элементов");
            return null;
        }

        if (name == null) {
            log.warn("в loadStringByName не передано название параметра");
            return null;
        }

        if(defaultValue == null){
            log.warn("в loadStringByName не передано значение по умолчанию");
            defaultValue = "";
        }

        if (! properties.containsKey(name)){
            log.warn("в configAccess.properties отсутствует свойство: " + name + ". Он будет заполнен " + minSize + " элементами по умолчанию " + defaultValue);
            return createAndSaveDefaultArray(minSize, name, defaultValue);
        }

        return properties.getProperty(name).split(", ");
    }

    private String [] createAndSaveDefaultArray(int minSize, String name, String defaultValue){
        String[] result = new String[minSize];
        Arrays.fill(result, defaultValue);
        properties.put(name, String.join(", ", result));
        fileHandler.updateFileFromProperties(properties);
        return result;
    }


    public Integer[] getIntegerArray(String name, Integer defaultValue, int minSize) {
        Integer array [];
        if(minSize < 0){
            log.warn("в loadStringByName не передано минимальное количество элементов");
            throw new IllegalArgumentException("Минимальный размер массива не может быть отрицательным.");
        }

        if (name == null) {
            log.warn("в loadStringByName не передано название параметра");
            throw new IllegalArgumentException("Название параметра не может быть null или пустым.");
        }

        if(defaultValue == null){
            log.warn("в loadStringByName не передано значение по умолчанию");
            defaultValue = 0;
        }

        if (! properties.containsKey(name)){
            log.warn("Свойство '" + name + "' отсутствует. Создается массив из " + minSize + " элементов по умолчанию: " + defaultValue);
            array = createDefaultArray(minSize, name, defaultValue);
            saveIntegerArray(name, array);
            return array;
        }

        // Чтение значения из properties
        String propertyValue = properties.getProperty(name);
        String[] parts = propertyValue.split(",\\s*");

        // Преобразование строк в числа с обработкой ошибок
        Integer[] result = new Integer[parts.length];
        boolean wasException = false;
        for (int i = 0; i < parts.length; i++) {
            try {
                result[i] = Integer.parseInt(parts[i]);
            } catch (NumberFormatException e) {
                log.warn("Ошибка преобразования элемента '" + parts[i] + "' в число. Используется значение по умолчанию: " + defaultValue, e);
                wasException = true;
                result[i] = defaultValue;
            }
        }

        // Если размер массива меньше минимального, заполняем недостающие элементы значением по умолчанию
        if (result.length < minSize) {
            log.warn("Размер массива '" + name + "' меньше минимального (" + result.length + " < " + minSize + "). Дополняется до " + minSize + " элементов значением: " + defaultValue);
            Integer [] newResult = new Integer[minSize];
            Arrays.fill(newResult, defaultValue);
            for (int i = 0; i < result.length; i++) {
                newResult [i] = result[i];
            }
            //нет времени выяснять что с ней
            //newResult = Arrays.copyOfRange(result, 0, result.length - 1);

            saveIntegerArray(name, newResult);
            log.warn("Был создан и сохранен массив значений параметра  '" + name + "' он имеет вид:" + Arrays.toString(newResult));
            return newResult;
        }
        if(wasException){
            saveIntegerArray(name, result);
        }
        return result;
    }



    private Integer [] createDefaultArray(int minSize, String name, Integer defaultValue){
        Integer[] result = new Integer[minSize];
        Arrays.fill(result, defaultValue);
        saveIntegerArray(name, result);
        return result;
    }


    public void setStringArray(String name, String [] parameters, boolean makeNullEmpty){
        if(makeNullEmpty){
            String [] notNullContainArray = new String[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                if(parameters[i] == null){
                    notNullContainArray [i] = "";
                }else{
                    notNullContainArray [i] = parameters[i];
                }
            }
            parameters = notNullContainArray;
        }
        String arrayAsString = Arrays.stream(parameters)
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
        putOrUpdateProperty(name, arrayAsString);
        fileHandler.updateFileFromProperties(properties);

    }

    public void setBoolean(String name, boolean stat){
        putOrUpdateProperty(name, String.valueOf(stat));
        fileHandler.updateFileFromProperties(properties);

    }

    public void setInt(String name, int value){
        putOrUpdateProperty(name, String.valueOf(value));
        fileHandler.updateFileFromProperties(properties);
    }

    public void saveIntegerArray(String name, Integer [] array){
        String arrayAsString = Arrays.stream(array)
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
        putOrUpdateProperty(name, arrayAsString);
        fileHandler.updateFileFromProperties(properties);
    }

    private void putOrUpdateProperty(String name, String property){
        if(properties.containsKey(name)){
            properties.setProperty(name, property);
        }else{
            properties.put(name, property);
        }
    }
}
