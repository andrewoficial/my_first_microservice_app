package org.example.gui.curve.file;

import org.apache.log4j.Logger;
import org.example.gui.curve.CurveData;
import org.example.gui.curve.CurveDataTypes;
import org.example.gui.curve.CurveDeviceCommander;
import org.example.gui.curve.CurveMetaData;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class Serialization {
    private Logger log = Logger.getLogger(Serialization.class);



    public CurveMetaData fileHeaderToCurveMetaData(String fileHeader) { //updateCurveMetaDara
        log.info("Начинаю разбор метаданных");
        CurveMetaData curveMetaData = new CurveMetaData();
        if (fileHeader == null || fileHeader.isEmpty()) {
            log.warn("fileHeader == null || fileHeader.isEmpty()");
            return null;
        }

        String[] fileStrings = fileHeader.split("\n");
        if (fileStrings.length == 0) {
            log.warn("fileStrings.length == 0");
        }

        for (String fileString : fileStrings) {
            log.info("Рассматриваю строку: " + fileString);

            if (fileString.startsWith("Sensor Model:")) {
                String value = fileString.replace("Sensor Model:", "").trim();
                curveMetaData.setSensorModel(value);
            } else if (fileString.startsWith("Serial Number:")) {
                curveMetaData.setSerialNumber(fileString.replace("Serial Number:", "").trim());
            } else if (fileString.startsWith("Data Format:")) {
                String dataFormat = fileString.replace("Data Format:", "");
                if (dataFormat.contains("\t")) { //curve
                    log.info("  Определяю тип данных для формата curve ");
                    dataFormat = dataFormat.trim();
                    dataFormat = dataFormat.replace("    ", "\t");
                    try {
                        Integer foundedNumber = Integer.parseInt(dataFormat.split("\t")[0]);
                        System.out.println("foundedNumber in Curve: " + foundedNumber);
                        curveMetaData.setDataFormat(CurveDataTypes.getByValue(foundedNumber));
                    } catch (NumberFormatException ex) {
                        log.warn(ex.getMessage());
                    }
                } else {//340
                    log.info("  Определяю тип данных для формата 340 ");
                    log.info("  Строка для разбора: " + fileString);
                    try {
                        if (!fileString.contains(":") || !fileString.contains("(")) {
                            log.warn("Data format does not contain ':' or '('");
                            break;
                        }

                        String[] parts = fileString.split("[:()]");

                        String numberPart = parts[1].trim();
                        Integer num = Integer.parseInt(numberPart);
                        log.info("  Разобрано число " + num);
                        curveMetaData.setDataFormat(CurveDataTypes.getByValue(num));
                        log.info("Установлен параметр " + curveMetaData.getDataFormat());

                    } catch (Exception e) {
                        // Если что-то пошло не так (например, число не парсится)
                        log.error(e.getMessage());
                    }
                }
                if (curveMetaData.getDataFormat() == null) {
                    // Если число не удалось распарсить, устанавливаем значение по умолчанию
                    curveMetaData.setDataFormat(CurveDataTypes.getByValue(2));
                    log.error("Был установлен тип данных по умолчанию!");
                }
            } else if (fileString.startsWith("SetPoint Limit:")) {
                String setPointsLimitString = fileString.replace("SetPoint Limit:", "").trim();
                Integer setPointLimit = 800;
                if (setPointsLimitString.isEmpty()) {
                    log.warn("Set points limit is empty");
                    curveMetaData.setSetPointLimit(setPointLimit);
                    continue;
                }

                if (setPointsLimitString.contains("      ")) {
                    setPointsLimitString = setPointsLimitString.split("      ")[0];
                    setPointsLimitString = setPointsLimitString.trim();
                }
                try {
                    setPointLimit = Integer.parseInt(setPointsLimitString);
                } catch (NumberFormatException ex) {
                    log.warn(ex.getMessage());
                    curveMetaData.setSetPointLimit(setPointLimit);
                    continue;
                }
                curveMetaData.setSetPointLimit(setPointLimit);
            } else if (fileString.startsWith("Temperature coefficient:")) {
                String temperatureCoefficient = fileString.replace("Temperature coefficient:", "").trim();
                curveMetaData.setTemperatureCoefficient(temperatureCoefficient);
            } else if (fileString.startsWith("Number of Breakpoints:")) {
                String numberOfBreakpoints = fileString.replace("Number of Breakpoints:", "").trim();
                Integer numberOfBreakpointsInt = 99999;
                try {
                    numberOfBreakpointsInt = Integer.parseInt(numberOfBreakpoints);
                } catch (NumberFormatException ex) {
                    log.warn(ex.getMessage());
                    curveMetaData.setNumberOfBreakpoints(numberOfBreakpointsInt);
                    continue;
                }
                curveMetaData.setNumberOfBreakpoints(numberOfBreakpointsInt);
            }
        }
        return curveMetaData;
    }

    public CurveData deserializeCurveData(File file) throws CurveFileSerializationException {
        CurveData curveOpenedData = new CurveData();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            log.info("Начинаю чтение файла " + file.getName());
            List<String[]> tableData = new ArrayList<>();
            String line;
            int lineCount = 0;
            StringBuilder sb = new StringBuilder();
            int needLineScip = 5;
            if (file.getName().endsWith(".340")) {
                needLineScip = 8;
            } else if (file.getName().endsWith(".curve")) {
                needLineScip = 5;
            }else{
                log.warn("Неопознанный формат файла");
                throw new CurveFileSerializationException("Неопознанный формат файла" + file.getName());
            }
            // Пропуск заголовков (первые n строк)
            line = reader.readLine();
            while (line != null && lineCount < needLineScip) {
                sb.append(line);
                sb.append("\n");
                lineCount++;
                line = reader.readLine();
            }
            log.info("Считан заголовок: [\n" + sb.toString() + "\n]");
            // Чтение данных

            while (line != null) {

                String[] parts = new String[2];
                if (file.getName().endsWith(".340")) {
                    //log.info("Ориентируюсь на позиции в строке");
                    if (line.length() < 24) {
                        log.warn("Слишком короткая строка: [" + line.length() + "] в файле [" + file.getName() + "] при том, что было пропущено [" + lineCount + "] строк. Пропускаю ее.");
                        log.warn(line);
                        line = reader.readLine();
                        continue;
                    }
                    if (!(line.charAt(3) == ' ')) {
                        log.warn("Неверное положение пробела после номера строки: " + line.charAt(3));
                        line = reader.readLine();
                        continue;
                    }
                    String lineNumber = line.substring(0, 3);
                    lineNumber = lineNumber.trim();

                    String units = line.substring(5, 19);
                    units = units.trim();

                    String temperature = line.substring(19, line.length() - 1);
                    temperature = temperature.trim();

                    parts[0] = units;
                    parts[1] = temperature;
                    //System.out.println("Line number: + " + i + "Readet lineNumber: " + lineNumber + "Line: " + line + " units " + units + " temp " + temperature);
                } else {
                    //log.info("Ориентируюсь на нарезку");
                    parts = line.split("\t"); // Разделитель - табуляция

                    //System.out.println("Line number: + " + i + "Line: " + line + " units " + parts[0] + " temp " + parts[1]);
                }

                if (parts.length == 2) {
                    tableData.add(parts);
                }

                line = reader.readLine();
            }
            CurveMetaData curveOpenedMetaData;

            curveOpenedMetaData = fileHeaderToCurveMetaData(sb.toString()); //updateCurveMetaDara


            curveOpenedData.setCurveMetaData(curveOpenedMetaData);

            for (String[] row : tableData) {
                curveOpenedData.addCurvePointFromString(row[0], row[1]);
            }

        } catch (Exception ex) {
            throw new CurveFileSerializationException("Ошибка чтения файла: " + ex.getMessage());
        }
        log.info("Возвращаю curveOpenedData с форматом данных" + curveOpenedData.getCurveMetaData().getDataFormat() + " и его статус самопроверки " + curveOpenedData.isConsistent());
        return curveOpenedData;
    }

}
