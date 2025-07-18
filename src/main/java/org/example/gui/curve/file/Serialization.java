package org.example.gui.curve.file;

import org.apache.log4j.Logger;
import org.example.gui.curve.CurveDataTypes;
import org.example.gui.curve.CurveDeviceCommander;
import org.example.gui.curve.CurveMetaData;

public class Serialization {
    private Logger log = Logger.getLogger(Serialization.class);
    public CurveMetaData fileHeaderToCurveMetaData(String fileHeader) { //updateCurveMetaDara
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
            System.out.println("fileString: " + fileString);
            if (fileString.startsWith("Sensor Model:")) {
                String value = fileString.replace("Sensor Model:", "").trim();
                curveMetaData.setSensorModel(value);
            } else if (fileString.startsWith("Serial Number:")) {
                curveMetaData.setSerialNumber(fileString.replace("Serial Number:", "").trim());
            } else if (fileString.startsWith("Data Format:")) {
                String dataFormat = fileString.replace("Data Format:", "");
                CurveDataTypes dataTypes = null;
                if (dataFormat.contains("\t")) { //curve
                    log.info("  Определяю тип данных для формата curve ");
                    dataFormat = dataFormat.trim();
                    dataFormat = dataFormat.replace("    ", "\t");
                    try {
                        Integer foundedNumber = Integer.parseInt(dataFormat.split("\t")[0]);
                        System.out.println("foundedNumber in Curve: " + foundedNumber);
                        dataTypes = CurveDataTypes.getByValue(foundedNumber);
                    } catch (NumberFormatException ex) {
                        log.warn(ex.getMessage());
                    }
                } else {//340
                    log.info("  Определяю тип данных для формата 340 ");
                    log.info("  Строка для разбора: " + fileString);
                    try {
                        // Проверяем наличие двоеточия и скобки
                        if (!fileString.contains(":") || !fileString.contains("(")) {
                            log.warn("Data format does not contain ':' or '('");
                            break;
                        }

                        // Разделяем строку по двоеточию и скобке
                        String[] parts = fileString.split("[:()]");
                        for (String part : parts) {
                            log.info(part);
                        }
                        // Берем первую часть после разделения и чистим от пробелов
                        String numberPart = parts[1].trim();
                        log.info("  Разобрана str " + numberPart);
                        // Парсим число
                        Integer num = Integer.parseInt(numberPart);
                        log.info("  Разобрано число " + num);
                        System.out.println("foundedNumber in 340: " + num);
                        dataTypes = CurveDataTypes.getByValue(num);

                    } catch (Exception e) {
                        // Если что-то пошло не так (например, число не парсится)
                        log.warn(e.getMessage());
                    } finally {
                        if (dataTypes == null) {
                            // Если число не удалось распарсить, устанавливаем значение по умолчанию
                            dataTypes = CurveDataTypes.getByValue(2);
                        }
                    }

                }
                curveMetaData.setDataFormat(dataTypes);
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
                System.out.println("Temperature coefficient:");
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
}
